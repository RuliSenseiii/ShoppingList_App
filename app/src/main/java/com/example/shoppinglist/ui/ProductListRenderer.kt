package com.example.shoppinglist.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import androidx.core.content.ContextCompat
import com.example.shoppinglist.R
import com.example.shoppinglist.data.Product
import com.example.shoppinglist.data.SortMode
import com.example.shoppinglist.data.ViewSettings

/** What the "−" and "+" buttons and a tap on the quantity do, for the screens that enable them. */
class QuantityActions(
    /** [direction] is -1 or +1. */
    val onStep: (Product, Int) -> Unit,
    /** Tapping the quantity itself, to type it with the keyboard. */
    val onTap: (Product) -> Unit,
)

/**
 * Fills [container] with the products. By default they are grouped by category: a coloured category
 * title (emoji badge + product count) over a "----" line, then a rounded card with one row per product
 * (name left, quantity right) alternating white/beige. The user's sort choice ([ViewSettings]) can
 * instead put the categories in his own order or list everything from A to Z.
 */
object ProductListRenderer {

    fun render(
        container: LinearLayout,
        products: List<Product>,
        crossedIds: Set<String> = emptySet(),
        onProductClick: ((Product) -> Unit)? = null,
        quantityActions: QuantityActions? = null,
    ) {
        container.removeAllViews()
        val context = container.context
        val inflater = LayoutInflater.from(context)
        // The tutorial points at the first product still to buy and at the first crossed one.
        var rowTagged = false
        var crossedTagged = false
        var stepperTagged = false

        val groups: List<Group> = when (ViewSettings.sortMode(context)) {
            SortMode.ALPHABETICAL -> listOf(
                Group(
                    "🔤", context.getString(R.string.sort_alphabetical_header), ContextCompat.getColor(context, R.color.green_dark),
                    products.sortedBy { it.name.searchKey() },
                )
            )
            SortMode.ROUTE -> categoryGroups(context, ViewSettings.categoryOrder(context), products)
            SortMode.CATEGORY -> categoryGroups(context, com.example.shoppinglist.data.Category.entries, products)
        }

        groups.forEach { group ->
            if (group.items.isEmpty()) return@forEach
            val color = group.color

            // Header: emoji badge, coloured title and a count chip.
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, context.dp(20), 0, 0)
            }
            header.addView(TextView(context).apply {
                text = group.emoji
                textSize = 20f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                background = roundedBackground(soft(color), 0f, oval = true)
            }, LinearLayout.LayoutParams(context.dp(42), context.dp(42)))
            header.addView(TextView(context).apply {
                text = group.title
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = context.dp(12)
            })
            header.addView(TextView(context).apply {
                text = String.format(Locale.getDefault(), "%d", group.items.size)
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color)
                gravity = Gravity.CENTER
                minWidth = context.dp(30)
                setPadding(context.dp(10), context.dp(3), context.dp(10), context.dp(3))
                background = roundedBackground(soft(color), context.dp(100).toFloat())
            })
            container.addView(header)

            container.addView(TextView(context).apply {
                text = "-".repeat(200)
                maxLines = 1
                setHorizontallyScrolling(true)
                typeface = Typeface.MONOSPACE
                textSize = 14f
                setTextColor(color)
                setPadding(0, context.dp(2), 0, context.dp(6))
            })

            // Rounded card holding the rows; clipToOutline keeps the row colours inside the corners.
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_plain)
                clipToOutline = true
                elevation = context.dp(2).toFloat()
            }
            group.items.forEachIndexed { index, product ->
                val crossed = product.id in crossedIds
                val rowColor = ContextCompat.getColor(context, product.category.colorRes)
                val tagStepper = quantityActions != null && !crossed && !stepperTagged
                if (tagStepper) stepperTagged = true
                val row = productRow(inflater, card, product, index, rowColor, crossed, onProductClick, quantityActions, tagStepper)
                if (crossed && !crossedTagged) {
                    row.tag = "tut:product_crossed"
                    crossedTagged = true
                } else if (!crossed && !rowTagged) {
                    row.tag = "tut:product_row"
                    rowTagged = true
                }
                card.addView(row)
            }
            container.addView(
                card,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = context.dp(4) },
            )
        }
    }

    private class Group(val emoji: String, val title: String, val color: Int, val items: List<Product>)

    private fun categoryGroups(
        context: android.content.Context,
        categories: List<com.example.shoppinglist.data.Category>,
        products: List<Product>,
    ) = categories.map { category ->
        Group(category.emoji, category.label, ContextCompat.getColor(context, category.colorRes), products.filter { it.category == category })
    }

    private fun productRow(
        inflater: LayoutInflater,
        parent: LinearLayout,
        product: Product,
        index: Int,
        color: Int,
        crossed: Boolean,
        onProductClick: ((Product) -> Unit)?,
        quantityActions: QuantityActions?,
        tagStepper: Boolean,
    ): View {
        val context = parent.context
        val item = inflater.inflate(R.layout.item_product, parent, false)
        val rowColor = if (index % 2 == 0) R.color.white else R.color.beige
        item.findViewById<View>(R.id.row).setBackgroundColor(ContextCompat.getColor(context, rowColor))
        item.findViewById<View>(R.id.dot).background = roundedBackground(color, 0f, oval = true)
        // A bell marks the products of the home count that raise an alert when running low.
        item.findViewById<TextView>(R.id.tvName).text =
            if (product.minQuantity > 0.0) "${product.name}  🔔" else product.name
        val quantity = item.findViewById<TextView>(R.id.tvQuantity).apply {
            text = product.formattedQuantity
            setTextColor(color)
            background = roundedBackground(soft(color, 0x1A), context.dp(100).toFloat())
        }
        if (quantityActions != null && !crossed) {
            // Quick buttons, and the quantity itself opens the keyboard.
            listOf(R.id.btnMinus to -1, R.id.btnPlus to 1).forEach { (id, direction) ->
                item.findViewById<TextView>(id).apply {
                    visibility = View.VISIBLE
                    setTextColor(color)
                    background = roundedBackground(soft(color, 0x26), 0f, oval = true)
                    setOnClickListener { quantityActions.onStep(product, direction) }
                }
            }
            quantity.setOnClickListener { quantityActions.onTap(product) }
            if (tagStepper) {
                // The tutorial points at the first "+" and the first quantity.
                item.findViewById<View>(R.id.btnPlus).tag = "tut:qty_plus"
                quantity.tag = "tut:qty_value"
            }
        }
        item.findViewById<View>(R.id.row).alpha = if (crossed) 0.55f else 1f
        item.findViewById<ScribbleView>(R.id.scribble).apply {
            visibility = if (crossed) View.VISIBLE else View.GONE
            seed = product.id.hashCode()
        }
        if (onProductClick != null) {
            item.setOnClickListener { onProductClick(product) }
        } else {
            item.foreground = null
        }
        return item
    }
}
