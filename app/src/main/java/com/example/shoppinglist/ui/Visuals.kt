package com.example.shoppinglist.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.example.shoppinglist.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

fun Context.dp(value: Number): Int = (value.toFloat() * resources.displayMetrics.density).toInt()

/** [color] with the given alpha (0-255), for soft tinted backgrounds. */
@ColorInt
fun soft(@ColorInt color: Int, alpha: Int = 0x24): Int = ColorUtils.setAlphaComponent(color, alpha)

/** Rounded rectangle or circle filled with [color]. */
fun roundedBackground(@ColorInt color: Int, radiusPx: Float, oval: Boolean = false) = GradientDrawable().apply {
    shape = if (oval) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
    if (!oval) cornerRadius = radiusPx
    setColor(color)
}

/** Big rounded button with the green or orange gradient and an optional icon. */
fun gradientButton(
    context: Context,
    text: String,
    @DrawableRes icon: Int?,
    green: Boolean,
): MaterialButton = MaterialButton(context).apply {
    this.text = text
    textSize = 17f
    isAllCaps = false
    letterSpacing = 0.02f
    setTextColor(ContextCompat.getColor(context, R.color.white))
    applyGradient(green)
    if (icon != null) {
        setIconResource(icon)
        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        iconPadding = context.dp(10)
        iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
    }
}

fun MaterialButton.applyGradient(green: Boolean) {
    setBackgroundResource(if (green) R.drawable.bg_btn_green else R.drawable.bg_btn_orange)
    // Must come after the background so the theme tint does not flatten the gradient.
    backgroundTintList = null
}

/** Centered illustration for empty screens: a big emoji, a title and an optional hint. */
fun LinearLayout.addEmptyState(emoji: String, title: String, hint: String? = null) {
    val context = context
    addView(TextView(context).apply {
        text = emoji
        textSize = 64f
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER
        setPadding(0, context.dp(48), 0, context.dp(8))
    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    addView(TextView(context).apply {
        text = title
        textSize = 19f
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.text_dark))
    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    if (hint != null) {
        addView(TextView(context).apply {
            text = hint
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            setPadding(context.dp(24), context.dp(6), context.dp(24), 0)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
}

/**
 * Bottom message with a "Deshacer" button, shown after deleting something. It sits above the
 * screen's bottom button, if there is one, so it does not cover it.
 */
fun Activity.showUndo(message: String, onUndo: () -> Unit) {
    Snackbar.make(findViewById(android.R.id.content), message, UNDO_DURATION_MS)
        .setAction(R.string.undo) { onUndo() }
        .setActionTextColor(ContextCompat.getColor(this, R.color.orange_soft))
        .apply { findViewById<View>(R.id.btnBottom)?.takeIf { it.isVisible }?.let { anchorView = it } }
        .show()
}

private const val UNDO_DURATION_MS = 5000
