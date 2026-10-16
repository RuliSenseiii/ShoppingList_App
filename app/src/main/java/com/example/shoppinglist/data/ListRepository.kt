package com.example.shoppinglist.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores every shopping list as JSON in SharedPreferences.
 * The "Productos que me faltan" list always exists and uses [ShoppingList.MISSING_LIST_ID].
 * Also remembers the products used before (name + category) to suggest them while typing.
 */
class ListRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val missingListName = context.getString(com.example.shoppinglist.R.string.missing_list_name)

    private val lists: MutableList<ShoppingList> = load()

    /** Products used before, most recent first, keyed by lowercase name. */
    private val history: LinkedHashMap<String, ProductSuggestion> = loadHistory()

    /** User lists, without the missing-products list. */
    fun userLists(): List<ShoppingList> = lists.filterNot { it.isMissingList }

    fun missingList(): ShoppingList =
        lists.firstOrNull { it.isMissingList }
            ?: ShoppingList(id = ShoppingList.MISSING_LIST_ID, name = missingListName).also {
                lists.add(0, it)
                save()
            }

    fun get(id: String): ShoppingList? =
        if (id == ShoppingList.MISSING_LIST_ID) missingList() else lists.firstOrNull { it.id == id }

    fun createList(name: String): ShoppingList =
        ShoppingList(name = name).also {
            lists.add(it)
            save()
        }

    /** Saves a list received from someone else as a new list of this user. */
    fun importList(name: String, products: List<Product>): ShoppingList =
        ShoppingList(name = name).also { list ->
            products.forEach {
                list.addOrMerge(it)
                remember(it)
            }
            lists.add(list)
            save()
        }

    fun renameList(id: String, name: String) {
        get(id)?.takeUnless { it.isMissingList }?.name = name
        save()
    }

    /** A deleted list and where it was, so it can be put back with [restoreList]. */
    class DeletedList internal constructor(internal val list: ShoppingList, internal val index: Int)

    fun deleteList(id: String): DeletedList? {
        if (id == ShoppingList.MISSING_LIST_ID) {
            missingList().products.clear()
            missingList().inCart.clear()
            save()
            return null
        }
        val index = lists.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = lists.removeAt(index)
        save()
        return DeletedList(removed, index)
    }

    fun restoreList(deleted: DeletedList) {
        if (lists.any { it.id == deleted.list.id }) return
        lists.add(deleted.index.coerceAtMost(lists.size), deleted.list)
        save()
    }

    fun addProduct(listId: String, product: Product) {
        get(listId)?.addOrMerge(product)
        remember(product)
        save()
    }

    /** A deleted product and where it was, so it can be put back with [restoreProduct]. */
    class DeletedProduct internal constructor(
        internal val listId: String,
        internal val product: Product,
        internal val index: Int,
        internal val wasInCart: Boolean,
    )

    fun removeProduct(listId: String, productId: String): DeletedProduct? {
        val list = get(listId) ?: return null
        val index = list.products.indexOfFirst { it.id == productId }
        if (index < 0) return null
        val removed = list.products.removeAt(index)
        val wasInCart = list.inCart.remove(productId)
        save()
        return DeletedProduct(listId, removed, index, wasInCart)
    }

    fun restoreProduct(deleted: DeletedProduct) {
        val list = get(deleted.listId) ?: return
        if (list.products.any { it.id == deleted.product.id }) return
        list.products.add(deleted.index.coerceAtMost(list.products.size), deleted.product)
        if (deleted.wasInCart) list.inCart.add(deleted.product.id)
        save()
    }

    fun updateQuantity(listId: String, productId: String, quantity: Double) {
        val products = get(listId)?.products ?: return
        val index = products.indexOfFirst { it.id == productId }
        if (index >= 0) products[index] = products[index].copy(quantity = quantity)
        save()
    }

    /** Moves every product of the missing list into [listId] and empties the missing list. */
    fun moveMissingInto(listId: String) {
        val target = get(listId) ?: return
        val missing = missingList()
        missing.products.forEach { target.addOrMerge(it) }
        missing.products.clear()
        missing.inCart.clear()
        save()
    }

    /** Marks a product as in the cart (or not) while shopping; survives leaving the screen. */
    fun setInCart(listId: String, productId: String, inCart: Boolean) {
        val list = get(listId) ?: return
        if (inCart) list.inCart.add(productId) else list.inCart.remove(productId)
        save()
    }

    fun clearCart(listId: String) {
        get(listId)?.inCart?.clear()
        save()
    }

    /** Products used before, most recent first. */
    fun suggestions(): List<ProductSuggestion> = history.values.toList()

    private fun remember(product: Product) {
        val key = product.name.trim().lowercase()
        if (key.isEmpty()) return
        // Move it to the front, so the latest name and category win and the oldest ones drop off.
        history.remove(key)
        val older = history.entries.take(MAX_HISTORY - 1).map { it.toPair() }
        history.clear()
        history[key] = ProductSuggestion(product.name.trim(), product.category)
        older.forEach { (k, v) -> history[k] = v }
    }

    private fun save() {
        val array = JSONArray()
        lists.forEach { list ->
            val products = JSONArray()
            list.products.forEach { p ->
                products.put(
                    JSONObject()
                        .put("id", p.id)
                        .put("name", p.name)
                        .put("category", p.category.name)
                        .put("quantity", p.quantity)
                )
            }
            // Only ids still in the list, so deleted products do not linger in the cart.
            val ids = list.products.map { it.id }.toSet()
            val inCart = JSONArray()
            list.inCart.filter { it in ids }.forEach { inCart.put(it) }
            array.put(
                JSONObject()
                    .put("id", list.id)
                    .put("name", list.name)
                    .put("products", products)
                    .put("inCart", inCart)
            )
        }
        val historyJson = JSONArray()
        history.values.forEach { historyJson.put(JSONArray().put(it.name).put(it.category.name)) }
        prefs.edit {
            putString(KEY_LISTS, array.toString())
            putString(KEY_HISTORY, historyJson.toString())
        }
    }

    /**
     * Reads the saved lists. A damaged list or product is skipped instead of discarding everything,
     * and if the data cannot be read at all a copy is kept so the next save does not wipe it out.
     * Lists saved by older versions have no "inCart" and start with nothing in the cart.
     */
    private fun load(): MutableList<ShoppingList> {
        val raw = prefs.getString(KEY_LISTS, null) ?: return mutableListOf()
        val array = try {
            JSONArray(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Saved lists are unreadable; keeping a backup", e)
            prefs.edit { putString(KEY_BACKUP, raw) }
            return mutableListOf()
        }
        val lists = mutableListOf<ShoppingList>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                val productsJson = obj.optJSONArray("products") ?: JSONArray()
                val products = mutableListOf<Product>()
                for (j in 0 until productsJson.length()) {
                    try {
                        val p = productsJson.getJSONObject(j)
                        products.add(
                            Product(
                                id = p.getString("id"),
                                name = p.getString("name"),
                                category = Category.valueOf(p.getString("category")),
                                quantity = p.getDouble("quantity"),
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping unreadable product", e)
                    }
                }
                val inCartJson = obj.optJSONArray("inCart") ?: JSONArray()
                val inCart = (0 until inCartJson.length()).map { inCartJson.optString(it) }.toMutableSet()
                lists.add(
                    ShoppingList(id = obj.getString("id"), name = obj.getString("name"), products = products, inCart = inCart)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Skipping unreadable list", e)
            }
        }
        return lists
    }

    /** Reads the saved history and adds the products already in the lists (e.g. after updating from V0.2). */
    private fun loadHistory(): LinkedHashMap<String, ProductSuggestion> {
        val history = LinkedHashMap<String, ProductSuggestion>()
        try {
            val array = JSONArray(prefs.getString(KEY_HISTORY, null) ?: "[]")
            for (i in 0 until array.length()) {
                val item = array.optJSONArray(i) ?: continue
                val name = item.optString(0).trim()
                val category = Category.entries.firstOrNull { it.name == item.optString(1) } ?: continue
                if (name.isNotEmpty()) history.putIfAbsent(name.lowercase(), ProductSuggestion(name, category))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Product history is unreadable; rebuilding it from the lists", e)
        }
        lists.flatMap { it.products }.forEach {
            history.putIfAbsent(it.name.trim().lowercase(), ProductSuggestion(it.name.trim(), it.category))
        }
        return history
    }

    companion object {
        private const val PREFS_NAME = "shopping_lists"
        private const val KEY_LISTS = "lists"
        private const val KEY_HISTORY = "product_history"
        private const val KEY_BACKUP = "lists_backup"
        private const val MAX_HISTORY = 300
        private const val TAG = "ListRepository"

        @Volatile
        private var instance: ListRepository? = null

        fun get(context: Context): ListRepository =
            instance ?: synchronized(this) {
                instance ?: ListRepository(context.applicationContext).also { instance = it }
            }
    }
}
