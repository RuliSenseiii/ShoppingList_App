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
 */
class ListRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val missingListName = context.getString(com.example.shoppinglist.R.string.missing_list_name)

    private val lists: MutableList<ShoppingList> = load()

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
            products.forEach { list.addOrMerge(it) }
            lists.add(list)
            save()
        }

    fun deleteList(id: String) {
        if (id == ShoppingList.MISSING_LIST_ID) {
            missingList().products.clear()
        } else {
            lists.removeAll { it.id == id }
        }
        save()
    }

    fun addProduct(listId: String, product: Product) {
        get(listId)?.addOrMerge(product)
        save()
    }

    fun removeProduct(listId: String, productId: String) {
        get(listId)?.products?.removeAll { it.id == productId }
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
        save()
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
            array.put(
                JSONObject()
                    .put("id", list.id)
                    .put("name", list.name)
                    .put("products", products)
            )
        }
        prefs.edit { putString(KEY_LISTS, array.toString()) }
    }

    /**
     * Reads the saved lists. A damaged list or product is skipped instead of discarding everything,
     * and if the data cannot be read at all a copy is kept so the next save does not wipe it out.
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
                lists.add(ShoppingList(id = obj.getString("id"), name = obj.getString("name"), products = products))
            } catch (e: Exception) {
                Log.w(TAG, "Skipping unreadable list", e)
            }
        }
        return lists
    }

    companion object {
        private const val PREFS_NAME = "shopping_lists"
        private const val KEY_LISTS = "lists"
        private const val KEY_BACKUP = "lists_backup"
        private const val TAG = "ListRepository"

        @Volatile
        private var instance: ListRepository? = null

        fun get(context: Context): ListRepository =
            instance ?: synchronized(this) {
                instance ?: ListRepository(context.applicationContext).also { instance = it }
            }
    }
}
