package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.ui.ProductListRenderer
import com.example.shoppinglist.ui.addEmptyState
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.showProductActions
import com.google.android.material.button.MaterialButton

/** Option 1: shows a list grouped by category and lets the user edit it. */
class ListDetailActivity : AppCompatActivity() {

    private lateinit var listId: String
    private lateinit var content: LinearLayout
    private val repository by lazy { ListRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        listId = intent.getStringExtra(EXTRA_LIST_ID) ?: return finish()
        content = findViewById(R.id.content)

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
    }

    private fun render() {
        val list = repository.get(listId) ?: return finish()
        setupTopBar(list.name)
        ProductListRenderer.render(content, list.products) { product ->
            showProductActions(listId, product) { render() }
        }
        if (list.products.isEmpty()) {
            content.addEmptyState(if (list.isMissingList) "✅" else "🧺", getString(R.string.empty_list), getString(R.string.empty_list_hint))
        }
    }

    companion object {
        private const val EXTRA_LIST_ID = "list_id"

        fun intent(context: Context, listId: String) =
            Intent(context, ListDetailActivity::class.java).putExtra(EXTRA_LIST_ID, listId)
    }
}
