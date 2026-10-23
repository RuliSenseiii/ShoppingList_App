package com.example.shoppinglist.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Stores every shopping list as JSON in SharedPreferences.
 * The "Productos que me faltan" list always exists and uses [ShoppingList.MISSING_LIST_ID].
 * The "Recuento en casa" (what the user has at home) is stored as one more list with [ShoppingList.HOME_LIST_ID].
 * Also remembers the products used before (name + category) to suggest them while typing.
 */
class ListRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val missingListName = context.getString(com.example.shoppinglist.R.string.missing_list_name)
    private val homeListName = context.getString(com.example.shoppinglist.R.string.home_list_name)

    private val lists: MutableList<ShoppingList> = load()

    /** Products used before, most recent first, keyed by lowercase name. */
    private val history: LinkedHashMap<String, ProductSuggestion> = loadHistory()

    /** User lists, without the missing-products list nor the home count. */
    fun userLists(): List<ShoppingList> = lists.filterNot { it.isMissingList || it.isHomeList }

    fun missingList(): ShoppingList =
        lists.firstOrNull { it.isMissingList }
            ?: ShoppingList(id = ShoppingList.MISSING_LIST_ID, name = missingListName).also {
                lists.add(0, it)
                save()
            }

    fun homeList(): ShoppingList =
        lists.firstOrNull { it.isHomeList }
            ?: ShoppingList(id = ShoppingList.HOME_LIST_ID, name = homeListName).also {
                lists.add(it)
                save()
            }

    fun get(id: String): ShoppingList? = when (id) {
        ShoppingList.MISSING_LIST_ID -> missingList()
        ShoppingList.HOME_LIST_ID -> homeList()
        else -> lists.firstOrNull { it.id == id }
    }

    /** What the user has at home of the product with this name, or null if it is not in the home count. */
    fun atHome(name: String): Product? = homeList().findByName(name)

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
        get(id)?.takeUnless { it.isMissingList || it.isHomeList }?.name = name
        save()
    }

    /** The list made by [mergeLists] and the lists it replaced, so [undoMerge] can put them back. */
    class MergedLists internal constructor(val merged: ShoppingList, internal val originals: List<DeletedList>)

    /**
     * Replaces the lists [ids] (in the order they are shown) with one list called [name] holding all
     * their products; a product in several of them appears once with the quantities summed.
     * The new list takes the place of the first of them. Nothing is in the cart yet.
     */
    fun mergeLists(ids: List<String>, name: String): MergedLists? {
        val sources = userLists().filter { it.id in ids }
        if (sources.size < 2) return null
        val merged = ShoppingList.merged(name, sources)
        val originals = sources.map { DeletedList(it, lists.indexOf(it)) }
        lists.removeAll(sources)
        lists.add(originals.minOf { it.index }.coerceAtMost(lists.size), merged)
        merged.products.forEach { remember(it) }
        save()
        return MergedLists(merged, originals)
    }

    fun undoMerge(result: MergedLists) {
        lists.removeAll { it.id == result.merged.id }
        // In ascending order every list goes back to its old position.
        result.originals.sortedBy { it.index }.forEach { restoreList(it) }
        save()
    }

    /** A deleted list and where it was, so it can be put back with [restoreList]. */
    class DeletedList internal constructor(internal val list: ShoppingList, internal val index: Int)

    fun deleteList(id: String): DeletedList? {
        if (id == ShoppingList.HOME_LIST_ID) return null
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

    /**
     * Adds the product to the list. Noting a product in "Productos que me faltan" means it was used up,
     * so its quantity is taken off the home count (never below 0).
     * In the home count the quantity is not summed: it replaces what was counted before.
     */
    fun addProduct(listId: String, product: Product) {
        when (listId) {
            ShoppingList.HOME_LIST_ID -> setAtHome(product)
            ShoppingList.MISSING_LIST_ID -> {
                missingList().addOrMerge(product)
                takeFromHome(product)
            }
            else -> get(listId)?.addOrMerge(product)
        }
        remember(product)
        save()
    }

    private fun setAtHome(product: Product) {
        val home = homeList()
        val index = home.products.indexOfFirst { it.name.equals(product.name.trim(), ignoreCase = true) }
        if (index >= 0) {
            home.products[index] = home.products[index].copy(category = product.category, quantity = product.quantity)
        } else {
            home.products.add(product)
        }
    }

    /**
     * Everything in the list was bought: sums each product to the home count, adding the ones not
     * counted yet. If it was counted the other way (kg / units) the bought amount replaces it.
     */
    fun addBoughtToHome(listId: String) {
        val list = get(listId)?.takeUnless { it.isHomeList } ?: return
        val bought = list.products.filter { it.id in list.inCart }
        val home = homeList().products
        bought.forEach { product ->
            val index = home.indexOfFirst { it.name.equals(product.name.trim(), ignoreCase = true) }
            when {
                index < 0 -> home.add(product.copy(id = UUID.randomUUID().toString()))
                home[index].category.usesKilograms == product.category.usesKilograms ->
                    home[index] = home[index].copy(quantity = home[index].quantity + product.quantity)
                else -> home[index] = home[index].copy(category = product.category, quantity = product.quantity)
            }
        }
        save()
    }

    private fun takeFromHome(product: Product) {
        val products = homeList().products
        val index = products.indexOfFirst { it.name.equals(product.name.trim(), ignoreCase = true) }
        if (index < 0) return
        val atHome = products[index]
        // Only when both are counted the same way (kg or units).
        if (atHome.category.usesKilograms != product.category.usesKilograms) return
        products[index] = atHome.copy(quantity = (atHome.quantity - product.quantity).coerceAtLeast(0.0))
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

    /** Takes the products in the cart out of the list; what was not bought stays for next time. */
    fun removeBought(listId: String) {
        val list = get(listId) ?: return
        list.products.removeAll { it.id in list.inCart }
        list.inCart.clear()
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
