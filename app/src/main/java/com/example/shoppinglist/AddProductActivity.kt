package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.shoppinglist.data.Category
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.Product
import com.example.shoppinglist.data.ShoppingList
import com.example.shoppinglist.ui.ProductListRenderer
import com.example.shoppinglist.ui.parseQuantity
import com.example.shoppinglist.ui.quantityInputType
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.showProductActions
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Adds products one by one: name, category (drop-down) and quantity (kg or units).
 * The "Añadir" button only appears once the three fields are filled. Depending on how the screen
 * was opened the product goes to:
 *  - EDIT_LIST: the list being edited (from option 1).
 *  - NEW_LIST: a new list, created when the first product is added (option 2).
 *  - MISSING: the "Productos que me faltan" list (option 3).
 */
class AddProductActivity : AppCompatActivity() {

    enum class Mode { EDIT_LIST, NEW_LIST, MISSING }

    private val repository by lazy { ListRepository.get(this) }
    private lateinit var mode: Mode

    /** Target list; null in NEW_LIST mode until the first product is added. */
    private var listId: String? = null
    private var newListName: String? = null

    private lateinit var etName: EditText
    private lateinit var spCategory: Spinner
    private lateinit var etQuantity: EditText
    private lateinit var quantityRow: View
    private lateinit var tvUnit: TextView
    private lateinit var btnAdd: MaterialButton
    private lateinit var tvTarget: TextView
    private lateinit var tvCurrentTitle: TextView
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)
        applySystemBarInsets()

        mode = Mode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: Mode.MISSING.name)
        listId = savedInstanceState?.getString(STATE_LIST_ID) ?: when (mode) {
            Mode.EDIT_LIST -> intent.getStringExtra(EXTRA_LIST_ID)
            Mode.MISSING -> ShoppingList.MISSING_LIST_ID
            Mode.NEW_LIST -> null
        }
        newListName = savedInstanceState?.getString(STATE_NEW_LIST_NAME)

        setupTopBar(getString(if (mode == Mode.NEW_LIST) R.string.new_list_title else R.string.add_product))
        etName = findViewById(R.id.etName)
        spCategory = findViewById(R.id.spCategory)
        etQuantity = findViewById(R.id.etQuantity)
        quantityRow = findViewById(R.id.quantityRow)
        tvUnit = findViewById(R.id.tvUnit)
        btnAdd = findViewById(R.id.btnAdd)
        tvTarget = findViewById(R.id.tvTarget)
        tvCurrentTitle = findViewById(R.id.tvCurrentTitle)
        content = findViewById(R.id.content)

        setupCategorySpinner()
        etName.doAfterTextChanged { updateAddButton() }
        etQuantity.doAfterTextChanged { updateAddButton() }
        btnAdd.setOnClickListener { addProduct() }

        if (mode == Mode.NEW_LIST && listId == null && newListName == null) {
            askNewListName()
        }
        renderTarget()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_LIST_ID, listId)
        outState.putString(STATE_NEW_LIST_NAME, newListName)
    }

    private fun setupCategorySpinner() {
        val items = listOf(getString(R.string.category_hint)) + Category.entries.map { it.displayName }
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getView(position, convertView, parent), position)

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getDropDownView(position, convertView, parent), position).apply {
                    val padding = (14 * resources.displayMetrics.density).toInt()
                    setPadding(padding, padding, padding, padding)
                }

            private fun style(view: View, position: Int): View = (view as TextView).apply {
                textSize = 18f
                setTextColor(
                    ContextCompat.getColor(context, if (position == 0) R.color.text_muted else R.color.text_dark)
                )
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
        spCategory.setPopupBackgroundResource(R.drawable.bg_card_plain)
        // The whole rounded row opens the drop-down, not only the text.
        val categoryRow = findViewById<View>(R.id.categoryRow)
        categoryRow.setOnClickListener { spCategory.performClick() }
        categoryRow.post {
            spCategory.dropDownWidth = categoryRow.width
            spCategory.dropDownHorizontalOffset = -spCategory.left
        }
        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onCategoryChanged(selectedCategory())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun selectedCategory(): Category? =
        spCategory.selectedItemPosition.takeIf { it > 0 }?.let { Category.entries[it - 1] }

    private fun onCategoryChanged(category: Category?) {
        if (category == null) {
            quantityRow.visibility = View.GONE
            etQuantity.text.clear()
        } else {
            val newInputType = quantityInputType(category.usesKilograms)
            if (etQuantity.inputType != newInputType) {
                etQuantity.text.clear()
                etQuantity.inputType = newInputType
            }
            etQuantity.hint = getString(
                if (category.usesKilograms) R.string.quantity_kg_hint else R.string.quantity_units_hint
            )
            tvUnit.text = getString(if (category.usesKilograms) R.string.unit_kg else R.string.unit_units)
            quantityRow.visibility = View.VISIBLE
        }
        updateAddButton()
    }

    private fun currentQuantity(): Double? {
        val category = selectedCategory() ?: return null
        return parseQuantity(etQuantity.text.toString(), category.usesKilograms)
    }

    private fun updateAddButton() {
        val ready = etName.text.isNotBlank() && selectedCategory() != null && currentQuantity() != null
        btnAdd.visibility = if (ready) View.VISIBLE else View.GONE
    }

    private fun addProduct() {
        val category = selectedCategory() ?: return
        val quantity = currentQuantity() ?: return
        val name = etName.text.toString().trim().replaceFirstChar { it.uppercase() }

        val targetId = listId ?: repository.createList(newListName ?: defaultListName()).id.also { listId = it }
        repository.addProduct(targetId, Product(name = name, category = category, quantity = quantity))
        Toast.makeText(this, getString(R.string.added_to, targetName()), Toast.LENGTH_SHORT).show()

        etName.text.clear()
        etQuantity.text.clear()
        spCategory.setSelection(0)
        etName.requestFocus()
        renderTarget()
    }

    private fun targetName(): String =
        listId?.let { repository.get(it)?.name } ?: newListName ?: defaultListName()

    private fun renderTarget() {
        tvTarget.text = getString(R.string.adding_to, targetName())
        val list = listId?.let { repository.get(it) }
        val products = list?.products.orEmpty()
        tvCurrentTitle.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
        ProductListRenderer.render(content, products) { product ->
            showProductActions(list!!.id, product) { renderTarget() }
        }
    }

    private fun defaultListName(): String =
        getString(R.string.new_list_default_name, repository.userLists().size + 1)

    private fun askNewListName() {
        val input = EditText(this).apply {
            hint = getString(R.string.new_list_name_hint)
            setText(defaultListName())
            selectAll()
            maxLines = 1
            textSize = 18f
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dp(16), 0, dp(16), 0)
        }
        val wrapper = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56)))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("📝  " + getString(R.string.new_list_title))
            .setView(wrapper)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .setPositiveButton(R.string.create) { _, _ ->
                newListName = input.text.toString().trim().ifEmpty { defaultListName() }
                renderTarget()
            }
            .show()
    }

    companion object {
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_LIST_ID = "list_id"
        private const val STATE_LIST_ID = "state_list_id"
        private const val STATE_NEW_LIST_NAME = "state_new_list_name"

        fun editListIntent(context: Context, listId: String): Intent =
            if (listId == ShoppingList.MISSING_LIST_ID) {
                missingListIntent(context)
            } else {
                Intent(context, AddProductActivity::class.java)
                    .putExtra(EXTRA_MODE, Mode.EDIT_LIST.name)
                    .putExtra(EXTRA_LIST_ID, listId)
            }

        fun newListIntent(context: Context) =
            Intent(context, AddProductActivity::class.java).putExtra(EXTRA_MODE, Mode.NEW_LIST.name)

        fun missingListIntent(context: Context) =
            Intent(context, AddProductActivity::class.java).putExtra(EXTRA_MODE, Mode.MISSING.name)
    }
}
