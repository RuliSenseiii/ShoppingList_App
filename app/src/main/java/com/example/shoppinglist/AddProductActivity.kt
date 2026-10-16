package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.shoppinglist.data.Category
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.Product
import com.example.shoppinglist.data.ProductSuggestion
import com.example.shoppinglist.data.ShoppingList
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.ProductListRenderer
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.parseQuantity
import com.example.shoppinglist.ui.quantityInputType
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.showProductActions
import com.example.shoppinglist.ui.soft
import com.google.android.material.button.MaterialButton

/**
 * Adds products one by one: name, category and quantity (kg or units).
 * The name field suggests products used before; picking one also picks its category. Categories are
 * a grid of emoji buttons chosen with one tap, after which the quantity field opens with the keyboard.
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
    private var selectedCategory: Category? = null

    private lateinit var etName: AutoCompleteTextView
    private lateinit var categoryGrid: LinearLayout
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
        categoryGrid = findViewById(R.id.categoryGrid)
        etQuantity = findViewById(R.id.etQuantity)
        quantityRow = findViewById(R.id.quantityRow)
        tvUnit = findViewById(R.id.tvUnit)
        btnAdd = findViewById(R.id.btnAdd)
        tvTarget = findViewById(R.id.tvTarget)
        tvCurrentTitle = findViewById(R.id.tvCurrentTitle)
        content = findViewById(R.id.content)

        setupSuggestions()
        val restoredCategory = savedInstanceState?.getString(STATE_CATEGORY)
            ?.let { name -> Category.entries.firstOrNull { it.name == name } }
        selectCategory(restoredCategory, moveToQuantity = false)
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
        outState.putString(STATE_CATEGORY, selectedCategory?.name)
    }

    /** Suggestions of products used before, shown as "emoji name" under the name field. */
    private fun setupSuggestions() {
        etName.setAdapter(suggestionsAdapter())
        etName.setDropDownBackgroundResource(R.drawable.bg_card_plain)
        // As wide as the rounded name row, not the whole screen.
        val nameRow = findViewById<View>(R.id.nameRow)
        nameRow.post { etName.dropDownWidth = nameRow.width }
        etName.setOnItemClickListener { parent, _, position, _ ->
            val suggestion = parent.getItemAtPosition(position) as ProductSuggestion
            selectCategory(suggestion.category, moveToQuantity = true)
        }
        etName.doAfterTextChanged { text ->
            // Typing a known product in full also picks its category, unless one is already chosen.
            if (selectedCategory == null) {
                val typed = text.toString().trim()
                repository.suggestions().firstOrNull { it.name.equals(typed, ignoreCase = true) }
                    ?.let { selectCategory(it.category, moveToQuantity = false) }
            }
            updateAddButton()
        }
    }

    private fun suggestionsAdapter() = object : ArrayAdapter<ProductSuggestion>(
        this, android.R.layout.simple_dropdown_item_1line, repository.suggestions(),
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (super.getView(position, convertView, parent) as TextView).apply {
                text = getItem(position)?.let { "${it.category.emoji}  ${it.name}" }
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.text_dark))
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
    }

    /** Fills the 3x3 grid of category buttons, highlighting [selectedCategory]. */
    private fun renderCategoryGrid() {
        categoryGrid.removeAllViews()
        Category.entries.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, rowCategories ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowCategories.forEachIndexed { index, category ->
                row.addView(
                    categoryButton(category),
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index > 0) marginStart = dp(8)
                    },
                )
            }
            categoryGrid.addView(
                row,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { if (rowIndex > 0) topMargin = dp(8) },
            )
        }
    }

    private fun categoryButton(category: Category): View {
        val color = ContextCompat.getColor(this, category.colorRes)
        val selected = category == selectedCategory
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(72)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (selected) soft(color, 0x33) else ContextCompat.getColor(context, R.color.white))
                setStroke(
                    dp(if (selected) 2 else 1),
                    if (selected) color else ContextCompat.getColor(context, R.color.green_soft),
                )
            }
            isClickable = true
            isFocusable = true
            isSelected = selected
            contentDescription = category.label
            setOnClickListener { selectCategory(category, moveToQuantity = true) }
            addView(TextView(context).apply {
                text = category.emoji
                textSize = 24f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = category.label
                textSize = 13f
                maxLines = 1
                gravity = Gravity.CENTER
                if (selected) setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (selected) color else ContextCompat.getColor(context, R.color.text_dark))
            })
        }
    }

    private fun selectCategory(category: Category?, moveToQuantity: Boolean) {
        selectedCategory = category
        renderCategoryGrid()
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
            if (moveToQuantity) {
                etName.dismissDropDown()
                etQuantity.requestFocus()
                WindowCompat.getInsetsController(window, etQuantity).show(WindowInsetsCompat.Type.ime())
            }
        }
        updateAddButton()
    }

    private fun currentQuantity(): Double? {
        val category = selectedCategory ?: return null
        return parseQuantity(etQuantity.text.toString(), category.usesKilograms)
    }

    private fun updateAddButton() {
        val ready = etName.text.isNotBlank() && selectedCategory != null && currentQuantity() != null
        btnAdd.visibility = if (ready) View.VISIBLE else View.GONE
    }

    private fun addProduct() {
        val category = selectedCategory ?: return
        val quantity = currentQuantity() ?: return
        val name = etName.text.toString().trim().replaceFirstChar { it.uppercase() }

        val targetId = listId ?: repository.createList(newListName ?: defaultListName()).id.also { listId = it }
        repository.addProduct(targetId, Product(name = name, category = category, quantity = quantity))
        Toast.makeText(this, getString(R.string.added_to, targetName()), Toast.LENGTH_SHORT).show()

        selectCategory(null, moveToQuantity = false)
        etName.text.clear()
        etName.setAdapter(suggestionsAdapter()) // Now includes the product just added.
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
        Popups.askListName(
            activity = this,
            title = getString(R.string.new_list_title),
            initial = defaultListName(),
            positiveLabel = R.string.create,
            onCancel = { finish() },
        ) { name ->
            newListName = name
            renderTarget()
        }
    }

    companion object {
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_LIST_ID = "list_id"
        private const val STATE_LIST_ID = "state_list_id"
        private const val STATE_NEW_LIST_NAME = "state_new_list_name"
        private const val STATE_CATEGORY = "state_category"
        private const val GRID_COLUMNS = 3

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
