package com.example.shoppinglist.data

import android.content.Context
import androidx.core.content.edit

/** How the products of a list are shown. */
enum class SortMode {
    /** Grouped by category, in the usual order. */
    CATEGORY,

    /** One list, from A to Z. */
    ALPHABETICAL,

    /** Grouped by category, in the order the user walks through the supermarket. */
    ROUTE,
}

/** User preferences about how products are ordered. Part of the backup. */
object ViewSettings {

    private const val PREFS = "app_state"
    private const val KEY_SORT = "sort_mode"
    private const val KEY_CATEGORY_ORDER = "category_order"

    fun sortMode(context: Context): SortMode {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SORT, null)
        return SortMode.entries.firstOrNull { it.name == saved } ?: SortMode.CATEGORY
    }

    fun setSortMode(context: Context, mode: SortMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_SORT, mode.name) }
    }

    /** The user's category order; categories added by a later version go at the end. */
    fun categoryOrder(context: Context): List<Category> {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CATEGORY_ORDER, null)
        return parseOrder(saved)
    }

    fun setCategoryOrder(context: Context, order: List<Category>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_CATEGORY_ORDER, order.joinToString(",") { it.name })
        }
    }

    fun resetCategoryOrder(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(KEY_CATEGORY_ORDER) }
    }

    /** Raw values, for the backup file. */
    fun export(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SORT, null) to prefs.getString(KEY_CATEGORY_ORDER, null)
    }

    fun restore(context: Context, sort: String?, order: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            if (sort != null && SortMode.entries.any { it.name == sort }) putString(KEY_SORT, sort) else remove(KEY_SORT)
            if (order != null) putString(KEY_CATEGORY_ORDER, order) else remove(KEY_CATEGORY_ORDER)
        }
    }

    internal fun parseOrder(saved: String?): List<Category> {
        val chosen = saved.orEmpty().split(',')
            .mapNotNull { name -> Category.entries.firstOrNull { it.name == name.trim() } }
            .distinct()
        return chosen + Category.entries.filter { it !in chosen }
    }
}
