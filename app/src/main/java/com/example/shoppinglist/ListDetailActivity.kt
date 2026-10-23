package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.ProductListRenderer
import com.example.shoppinglist.ui.QuantityActions
import com.example.shoppinglist.ui.matchesSearch
import com.example.shoppinglist.ui.setupSearchBar
import com.example.shoppinglist.ui.showLowStockNotices
import com.example.shoppinglist.ui.showSortOptions
import com.example.shoppinglist.ui.TutorialGuide
import com.example.shoppinglist.ui.TutorialScreen
import com.example.shoppinglist.ui.addEmptyState
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.setupTopBarAction
import com.example.shoppinglist.ui.shareList
import com.example.shoppinglist.ui.showProductActions
import com.google.android.material.button.MaterialButton

/**
 * Option 1: shows a list grouped by category and lets the user edit or share it.
 * Also shows the "Recuento en casa", which is edited the same way but is not shared.
 */
class ListDetailActivity : AppCompatActivity(), TutorialScreen {

    override val screenId = "detail"


    private lateinit var listId: String
    private lateinit var content: LinearLayout
    private var query = ""
    private val repository by lazy { ListRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        listId = intent.getStringExtra(EXTRA_LIST_ID) ?: return finish()
        content = findViewById(R.id.content)
        setupSearchBar(onSort = { showSortOptions { render() } }) { text ->
            query = text
            render()
        }

        findViewById<MaterialButton>(R.id.btnBottom).apply {
            visibility = View.VISIBLE
            text = getString(R.string.add_product)
            setIconResource(R.drawable.ic_add)
            setOnClickListener {
                startActivity(AddProductActivity.editListIntent(this@ListDetailActivity, listId))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
        TutorialGuide.attach(this)
        showLowStockNotices()
    }

    private fun render() {
        val list = repository.get(listId) ?: return finish()
        setupTopBar(list.name)
        if (!list.isHomeList) {
            setupTopBarAction(R.drawable.ic_share, getString(R.string.share_list)) {
                TutorialGuide.emit(this, "share_opened")
                shareList(list)
            }
        }
        val shown = list.products.filter { it.name.matchesSearch(query) }
        // At home the quantities can be changed quickly: - and + buttons, or typing the number.
        val quantityActions = if (!list.isHomeList) null else QuantityActions(
            onStep = { product, direction ->
                repository.stepQuantity(listId, product.id, direction)
                showLowStockNotices()
                render()
                TutorialGuide.emit(this, "quantity_stepped")
            },
            onTap = { product ->
                TutorialGuide.emit(this, "quantity_dialog")
                Popups.editQuantity(
                    this, product, allowZero = true,
                    onUnitChosen = { kilograms -> repository.setUnit(listId, product.id, kilograms) },
                ) { quantity ->
                    repository.updateQuantity(listId, product.id, quantity)
                    showLowStockNotices()
                    render()
                    TutorialGuide.emit(this, "quantity_saved")
                }
            },
        )
        ProductListRenderer.render(content, shown, onProductClick = { product ->
            showProductActions(listId, product) { render() }
        }, quantityActions = quantityActions)
        if (list.products.isNotEmpty() && shown.isEmpty()) {
            content.addEmptyState("🔎", getString(R.string.search_no_results))
        } else if (list.products.isEmpty() && list.isHomeList) {
            content.addEmptyState("🏠", getString(R.string.home_empty), getString(R.string.home_empty_hint))
        } else if (list.products.isEmpty()) {
            content.addEmptyState(if (list.isMissingList) "✅" else "🧺", getString(R.string.empty_list), getString(R.string.empty_list_hint))
        }
    }

    companion object {
        private const val EXTRA_LIST_ID = "list_id"

        fun intent(context: Context, listId: String) =
            Intent(context, ListDetailActivity::class.java).putExtra(EXTRA_LIST_ID, listId)
    }
}
