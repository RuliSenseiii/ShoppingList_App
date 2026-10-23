package com.example.shoppinglist.ui

import android.app.Activity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.example.shoppinglist.R
import com.example.shoppinglist.CategoryOrderActivity
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.SortMode
import com.example.shoppinglist.data.ViewSettings
import java.text.Normalizer

private val ACCENTS = Regex("\\p{M}+")

/** Lowercase and without accents, so "Bollería" is found by typing "bolleria". */
fun String.searchKey(): String = Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(ACCENTS, "")

/** Whether this text contains [query]; an empty query matches everything. */
fun String.matchesSearch(query: String): Boolean = query.isBlank() || searchKey().contains(query.searchKey())

/**
 * Shows the search field under the top bar. [onQuery] gets the text on every change.
 * With [onSort] a sort button appears next to it.
 */
fun Activity.setupSearchBar(onSort: (() -> Unit)? = null, onQuery: (String) -> Unit) {
    findViewById<View>(R.id.searchBar).visibility = View.VISIBLE
    val field = findViewById<EditText>(R.id.etSearch)
    val clear = findViewById<ImageButton>(R.id.btnClearSearch)
    field.doAfterTextChanged { text ->
        val query = text.toString()
        clear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        onQuery(query.trim())
        if (query.isNotBlank()) TutorialGuide.emit(this, "search_typed")
    }
    clear.setOnClickListener {
        field.text.clear()
        TutorialGuide.emit(this, "search_cleared")
    }
    findViewById<ImageButton>(R.id.btnSort).apply {
        visibility = if (onSort == null) View.GONE else View.VISIBLE
        setOnClickListener { onSort?.invoke() }
    }
}

/** Pop-up to choose how products are ordered; [onChanged] runs after a new order is picked. */
fun Activity.showSortOptions(onChanged: () -> Unit) {
    TutorialGuide.emit(this, "sort_opened")
    val current = ViewSettings.sortMode(this)
    fun option(mode: SortMode, label: Int) = PopupAction(
        label = (if (mode == current) "✓  " else "") + getString(label),
        icon = null,
        green = mode == current,
    ) {
        ViewSettings.setSortMode(this, mode)
        onChanged()
    }
    Popups.show(
        activity = this,
        title = getString(R.string.sort_title),
        badge = PopupBadge("↕️", getColor(R.color.green)),
        actions = listOf(
            option(SortMode.CATEGORY, R.string.sort_by_category),
            option(SortMode.ALPHABETICAL, R.string.sort_alphabetical),
            option(SortMode.ROUTE, R.string.sort_route),
            PopupAction(getString(R.string.sort_edit_order), R.drawable.ic_sort, green = false) {
                startActivity(CategoryOrderActivity.intent(this))
            },
        ),
        showClose = true,
    )
}

/** Tells the user which products were just noted as missing because they ran low at home. */
fun Activity.showLowStockNotices() {
    val names = ListRepository.get(this).takeLowStockNotices()
    if (names.isEmpty()) return
    Toast.makeText(
        this,
        resources.getQuantityString(R.plurals.low_stock_added, names.size, names.size, names.first()),
        Toast.LENGTH_LONG,
    ).show()
}
