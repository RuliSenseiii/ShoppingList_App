package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.data.Category
import com.example.shoppinglist.data.ViewSettings
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.roundedBackground
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.soft
import com.google.android.material.button.MaterialButton

/**
 * Lets the user put the categories in the order he walks through the supermarket.
 * It is the order used by "Ruta del súper" when sorting products.
 */
class CategoryOrderActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        setupTopBar(getString(R.string.category_order_title))
        content = findViewById(R.id.content)
        findViewById<MaterialButton>(R.id.btnBottom).apply {
            visibility = View.VISIBLE
            text = getString(R.string.category_order_reset)
            setIconResource(R.drawable.ic_cancel)
            setOnClickListener {
                ViewSettings.resetCategoryOrder(this@CategoryOrderActivity)
                render()
            }
        }
        render()
    }

    private fun render() {
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = getString(R.string.category_order_hint)
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            setPadding(dp(4), 0, dp(4), dp(14))
        })
        val order = ViewSettings.categoryOrder(this)
        order.forEachIndexed { index, category ->
            content.addView(row(category, index, order))
        }
    }

    private fun row(category: Category, index: Int, order: List<Category>): View {
        val color = ContextCompat.getColor(this, category.colorRes)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_plain)
            elevation = dp(2).toFloat()
            setPadding(dp(14), dp(8), dp(8), dp(8))
        }
        row.addView(TextView(this).apply {
            text = "${index + 1}"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(26), LinearLayout.LayoutParams.WRAP_CONTENT))
        row.addView(TextView(this).apply {
            text = category.emoji
            textSize = 22f
            gravity = Gravity.CENTER
            background = roundedBackground(soft(color), 0f, oval = true)
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginStart = dp(6) })
        row.addView(TextView(this).apply {
            text = category.label
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(color)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
        row.addView(moveButton(R.drawable.ic_arrow_up, getString(R.string.category_move_up), color, enabled = index > 0) {
            move(order, index, -1)
        })
        row.addView(moveButton(R.drawable.ic_arrow_down, getString(R.string.category_move_down), color, enabled = index < order.lastIndex) {
            move(order, index, 1)
        })
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(10) }
        return row
    }

    private fun moveButton(@DrawableRes icon: Int, description: String, color: Int, enabled: Boolean, onClick: () -> Unit) =
        ImageButton(this).apply {
            setImageResource(icon)
            setColorFilter(color)
            contentDescription = description
            background = roundedBackground(soft(color, 0x1F), 0f, oval = true)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            alpha = if (enabled) 1f else 0.25f
            isEnabled = enabled
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(6) }
        }

    private fun move(order: List<Category>, index: Int, by: Int) {
        val moved = order.toMutableList()
        moved.add(index + by, moved.removeAt(index))
        ViewSettings.setCategoryOrder(this, moved)
        render()
    }

    companion object {
        fun intent(context: Context) = Intent(context, CategoryOrderActivity::class.java)
    }
}
