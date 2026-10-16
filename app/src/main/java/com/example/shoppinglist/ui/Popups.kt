package com.example.shoppinglist.ui

import android.app.Activity
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.shoppinglist.R
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.Product
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class PopupAction(
    val label: String,
    @param:DrawableRes val icon: Int?,
    val green: Boolean,
    val onClick: () -> Unit,
)

/** Emoji shown in a soft coloured circle at the top-left of a pop-up. */
data class PopupBadge(val emoji: String, @param:ColorInt val color: Int)

object Popups {

    /** Shows a pop-up with a title, a column of big buttons and, optionally, a close cross. */
    fun show(
        activity: Activity,
        title: String,
        actions: List<PopupAction>,
        showClose: Boolean,
        badge: PopupBadge? = null,
        subtitle: String? = null,
    ): AlertDialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_popup, null)
        view.findViewById<TextView>(R.id.tvPopupTitle).text = title
        view.findViewById<TextView>(R.id.tvPopupEmoji).apply {
            if (badge != null) {
                visibility = View.VISIBLE
                text = badge.emoji
                background = roundedBackground(soft(badge.color, 0x30), 0f, oval = true)
            }
        }
        view.findViewById<TextView>(R.id.tvPopupSubtitle).apply {
            if (subtitle != null) {
                visibility = View.VISIBLE
                text = subtitle
            }
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setCancelable(showClose)
            .create()

        view.findViewById<ImageButton>(R.id.btnPopupClose).apply {
            visibility = if (showClose) View.VISIBLE else View.GONE
            setOnClickListener { dialog.dismiss() }
        }
        val buttons = view.findViewById<LinearLayout>(R.id.popupButtons)
        actions.forEach { action ->
            val button = gradientButton(activity, action.label, action.icon, action.green).apply {
                setOnClickListener {
                    dialog.dismiss()
                    action.onClick()
                }
            }
            buttons.addView(
                button,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(60))
                    .apply { topMargin = activity.dp(10) },
            )
        }
        dialog.show()
        return dialog
    }

    /** Asks for a new quantity, using kg (decimals) or units depending on the product category. */
    fun editQuantity(activity: Activity, product: Product, onSaved: (Double) -> Unit) {
        val kilograms = product.category.usesKilograms
        val input = EditText(activity).apply {
            inputType = quantityInputType(kilograms)
            background = null
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(activity, R.color.text_dark))
            hint = activity.getString(if (kilograms) R.string.quantity_kg_hint else R.string.quantity_units_hint)
            setText(if (kilograms) product.formattedQuantity.removeSuffix(" kg") else product.quantity.toLong().toString())
            setSelection(text.length)
        }
        val field = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_input)
            setPadding(activity.dp(16), 0, activity.dp(16), 0)
            addView(input, LinearLayout.LayoutParams(0, activity.dp(58), 1f))
            addView(TextView(activity).apply {
                text = activity.getString(if (kilograms) R.string.unit_kg else R.string.unit_units)
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(activity, R.color.orange_dark))
            })
        }
        val wrapper = LinearLayout(activity).apply {
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), 0)
            addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("${product.category.emoji}  ${product.name}")
            .setView(wrapper)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = parseQuantity(input.text.toString(), kilograms)
                if (value == null) {
                    Toast.makeText(activity, R.string.invalid_quantity, Toast.LENGTH_SHORT).show()
                } else {
                    dialog.dismiss()
                    onSaved(value)
                }
            }
        }
        dialog.show()
    }
}

/** Pop-up shown when tapping a product while editing a list: delete it, edit its quantity or close. */
fun Activity.showProductActions(listId: String, product: Product, onChanged: () -> Unit) {
    val repository = ListRepository.get(this)
    Popups.show(
        activity = this,
        title = product.name,
        subtitle = "${product.category.label} · ${product.formattedQuantity}",
        badge = PopupBadge(product.category.emoji, ContextCompat.getColor(this, product.category.colorRes)),
        actions = listOf(
            PopupAction(getString(R.string.delete_product), R.drawable.ic_delete, green = false) {
                repository.removeProduct(listId, product.id)
                onChanged()
            },
            PopupAction(getString(R.string.edit_quantity), R.drawable.ic_edit, green = true) {
                Popups.editQuantity(this, product) { quantity ->
                    repository.updateQuantity(listId, product.id, quantity)
                    onChanged()
                }
            },
        ),
        showClose = true,
    )
}

fun quantityInputType(kilograms: Boolean): Int =
    if (kilograms) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    else InputType.TYPE_CLASS_NUMBER

/** Accepts "1,5" or "1.5" for kg and whole numbers for units. Returns null if not a positive amount. */
fun parseQuantity(text: String, kilograms: Boolean): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    if (!kilograms && value % 1.0 != 0.0) return null
    return value
}
