package com.example.shoppinglist.ui

import android.app.Activity
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
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
import com.example.shoppinglist.data.ShoppingList
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class PopupAction(
    val label: String,
    @param:DrawableRes val icon: Int?,
    val green: Boolean,
    /** Name the tutorial uses to point at this button ("popup_<key>"). */
    val tutorialKey: String? = null,
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
            tag = "tut:popup_close"
            visibility = if (showClose) View.VISIBLE else View.GONE
            setOnClickListener { dialog.dismiss() }
        }
        val buttons = view.findViewById<LinearLayout>(R.id.popupButtons)
        actions.forEach { action ->
            val button = gradientButton(activity, action.label, action.icon, action.green).apply {
                action.tutorialKey?.let { tag = "tut:popup_$it" }
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
        TutorialGuide.dialogShown(activity, dialog)
        return dialog
    }

    /**
     * Asks for a list name in a one-line field, pre-filled with [initial] and selected so typing
     * replaces it. An empty answer keeps [initial].
     */
    fun askListName(
        activity: Activity,
        title: String,
        initial: String,
        positiveLabel: Int,
        onCancel: (() -> Unit)? = null,
        onSaved: (String) -> Unit,
    ) {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.new_list_name_hint)
            setText(initial)
            // selectAll() alone is lost when the dialog gives the field focus.
            setSelectAllOnFocus(true)
            maxLines = 1
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            textSize = 18f
            setBackgroundResource(R.drawable.bg_input)
            setPadding(activity.dp(16), 0, activity.dp(16), 0)
        }
        val wrapper = FrameLayout(activity).apply {
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), 0)
            addView(input, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, activity.dp(56)))
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("📝  $title")
            .setView(wrapper)
            .setCancelable(onCancel == null)
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel?.invoke() }
            .setPositiveButton(positiveLabel) { _, _ -> onSaved(input.text.toString().trim().ifEmpty { initial }) }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).tag = "tut:dialog_positive"
        TutorialGuide.dialogShown(activity, dialog)
    }

    /**
     * Asks for a new quantity, using kg (decimals) or units depending on the product category.
     * [allowZero] is for the home count, where 0 means "no me queda".
     */
    fun editQuantity(
        activity: Activity,
        product: Product,
        allowZero: Boolean = false,
        title: String? = null,
        message: String? = null,
        initial: Double? = null,
        /** When given, the unit (kg / uds) can be switched in the dialog and this is told the new one. */
        onUnitChosen: ((Boolean) -> Unit)? = null,
        onSaved: (Double) -> Unit,
    ) {
        var kilograms = product.usesKilograms
        val shown = product.copy(quantity = initial ?: product.quantity)
        val input = EditText(activity).apply {
            inputType = quantityInputType(kilograms)
            background = null
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(activity, R.color.text_dark))
            hint = activity.getString(if (kilograms) R.string.quantity_kg_hint else R.string.quantity_units_hint)
            setText(if (kilograms) shown.formattedQuantity.removeSuffix(" kg") else shown.quantity.toLong().toString())
            setSelection(text.length)
        }
        val field = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_input)
            setPadding(activity.dp(16), 0, activity.dp(16), 0)
            addView(input, LinearLayout.LayoutParams(0, activity.dp(58), 1f))
            addView(TextView(activity).apply {
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(activity, R.color.orange_dark))
                showUnit(kilograms, switchable = onUnitChosen != null)
                if (onUnitChosen != null) {
                    setOnClickListener {
                        // Switching unit keeps the number when it makes sense (units are whole numbers).
                        val typed = input.text.toString().trim().replace(',', '.').toDoubleOrNull()
                        kilograms = !kilograms
                        input.inputType = quantityInputType(kilograms)
                        input.hint = activity.getString(if (kilograms) R.string.quantity_kg_hint else R.string.quantity_units_hint)
                        if (typed != null) {
                            input.setText(if (kilograms && typed % 1.0 != 0.0) typed.toString().replace('.', ',') else Math.round(typed).toString())
                            input.setSelection(input.text.length)
                        }
                        showUnit(kilograms, switchable = true)
                    }
                }
            })
        }
        val wrapper = LinearLayout(activity).apply {
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), 0)
            addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(title ?: "${product.category.emoji}  ${product.name}")
            .setMessage(message)
            .setView(wrapper)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = parseQuantity(input.text.toString(), kilograms, allowZero)
                if (value == null) {
                    Toast.makeText(activity, R.string.invalid_quantity, Toast.LENGTH_SHORT).show()
                } else {
                    dialog.dismiss()
                    if (kilograms != product.usesKilograms) onUnitChosen?.invoke(kilograms)
                    onSaved(value)
                }
            }
        }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).tag = "tut:dialog_positive"
        TutorialGuide.dialogShown(activity, dialog)
    }
}

/** Pop-up shown when tapping a product while editing a list: delete it, edit its quantity or close. */
fun Activity.showProductActions(listId: String, product: Product, onChanged: () -> Unit) {
    val repository = ListRepository.get(this)
    TutorialGuide.emit(this, "product_popup")
    Popups.show(
        activity = this,
        title = product.name,
        subtitle = "${product.category.label} · ${product.formattedQuantity}" +
            if (product.minQuantity > 0.0) "\n" + getString(R.string.low_stock_subtitle, product.formattedMin) else "",
        badge = PopupBadge(product.category.emoji, ContextCompat.getColor(this, product.category.colorRes)),
        actions = listOfNotNull(
            PopupAction(getString(R.string.delete_product), R.drawable.ic_delete, green = false) {
                val deleted = repository.removeProduct(listId, product.id)
                onChanged()
                if (deleted != null) {
                    showUndo(getString(R.string.product_deleted, product.name)) {
                        repository.restoreProduct(deleted)
                        onChanged()
                    }
                }
            },
            PopupAction(getString(R.string.edit_quantity), R.drawable.ic_edit, green = true) {
                Popups.editQuantity(
                    this, product, allowZero = listId == ShoppingList.HOME_LIST_ID,
                    onUnitChosen = { kilograms -> repository.setUnit(listId, product.id, kilograms) },
                ) { quantity ->
                    repository.updateQuantity(listId, product.id, quantity)
                    showLowStockNotices()
                    onChanged()
                }
            },
            // Only the home count has quantities that can run low.
            if (listId != ShoppingList.HOME_LIST_ID) null else
                PopupAction(getString(R.string.low_stock_action), R.drawable.ic_label, green = true, tutorialKey = "low_stock") {
                    TutorialGuide.emit(this, "low_stock_dialog")
                    Popups.editQuantity(
                        this, product, allowZero = true,
                        title = "🔔 ${product.name}",
                        message = getString(R.string.low_stock_title) + "\n" + getString(R.string.low_stock_hint),
                        initial = product.minQuantity,
                    ) { minimum ->
                        repository.setMinQuantity(product.id, minimum)
                        showLowStockNotices()
                        onChanged()
                        TutorialGuide.emit(this, "low_stock_saved")
                    }
                },
        ),
        showClose = true,
    )
}

fun quantityInputType(kilograms: Boolean): Int =
    if (kilograms) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    else InputType.TYPE_CLASS_NUMBER

/**
 * Accepts "1,5" or "1.5" for kg and whole numbers for units. Returns null if not a positive amount
 * (or, with [allowZero], a negative one).
 */
/** Writes "kg" or "uds"; when [switchable] it looks like a button (tapping it changes the unit). */
fun TextView.showUnit(kilograms: Boolean, switchable: Boolean) {
    text = context.getString(if (kilograms) R.string.unit_kg else R.string.unit_units) + if (switchable) "  ⇄" else ""
    if (switchable) {
        contentDescription = context.getString(R.string.unit_change)
        background = roundedBackground(soft(ContextCompat.getColor(context, R.color.orange), 0x26), context.dp(100).toFloat())
        setPadding(context.dp(12), context.dp(6), context.dp(12), context.dp(6))
    }
}

fun parseQuantity(text: String, kilograms: Boolean, allowZero: Boolean = false): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value < 0.0 || (value == 0.0 && !allowZero)) return null
    if (!kilograms && value % 1.0 != 0.0) return null
    return value
}
