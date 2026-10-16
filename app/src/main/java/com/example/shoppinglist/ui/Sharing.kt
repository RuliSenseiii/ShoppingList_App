package com.example.shoppinglist.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.shoppinglist.ListsActivity
import com.example.shoppinglist.R
import com.example.shoppinglist.data.Category
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.ListShareCodec
import com.example.shoppinglist.data.ShoppingList
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Sends [list] through any app (WhatsApp, Telegram, email...). The message is readable on its own,
 * grouped by category, and ends with a code the receiver's app uses to import the list.
 */
fun Activity.shareList(list: ShoppingList) {
    if (list.products.isEmpty()) {
        Toast.makeText(this, R.string.share_empty_list, Toast.LENGTH_LONG).show()
        return
    }
    val text = buildString {
        appendLine(getString(R.string.share_message_title, list.name))
        Category.entries.forEach { category ->
            val items = list.products.filter { it.category == category }
            if (items.isEmpty()) return@forEach
            appendLine()
            appendLine(category.displayName)
            items.forEach { appendLine("• ${it.name} — ${it.formattedQuantity}") }
        }
        appendLine()
        appendLine(getString(R.string.share_message_import))
        append(ListShareCodec.encode(list))
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_message_title, list.name))
        .putExtra(Intent.EXTRA_TEXT, text)
    val chooser = Intent.createChooser(send, getString(R.string.share_list))
        // Sharing the list with this same app would just import a copy of it.
        .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this, ListsActivity::class.java)))
    startActivity(chooser)
}

/**
 * Imports the list contained in [text] after asking the user. Calls [onImported] with the new list,
 * or shows an error if the text has no valid list code.
 */
fun Activity.importSharedList(text: CharSequence?, onImported: (ShoppingList) -> Unit) {
    val shared = ListShareCodec.decode(text)
    if (shared == null) {
        Toast.makeText(this, R.string.import_invalid, Toast.LENGTH_LONG).show()
        return
    }
    val name = shared.name.ifEmpty { getString(R.string.imported_list_default_name) }
    val emojis = Category.entries.filter { c -> shared.products.any { it.category == c } }.joinToString(" ") { it.emoji }
    val count = resources.getQuantityString(R.plurals.products_count, shared.products.size, shared.products.size)
    Popups.show(
        activity = this,
        title = getString(R.string.import_question, name),
        subtitle = if (emojis.isEmpty()) count else "$count  $emojis",
        badge = PopupBadge("📩", ContextCompat.getColor(this, R.color.green)),
        actions = listOf(
            PopupAction(getString(R.string.import_action), R.drawable.ic_download, green = true) {
                val list = ListRepository.get(this).importList(name, shared.products)
                Toast.makeText(this, getString(R.string.imported_list, list.name), Toast.LENGTH_SHORT).show()
                onImported(list)
            },
        ),
        showClose = true,
    )
}

/**
 * "Importar lista" button: uses the copied message straight away if the clipboard holds a list code,
 * otherwise asks the user to paste the message.
 */
fun Activity.importFromClipboardOrAsk(onImported: (ShoppingList) -> Unit) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val copied = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)
    if (ListShareCodec.containsCode(copied)) {
        importSharedList(copied, onImported)
        return
    }

    val input = EditText(this).apply {
        hint = getString(R.string.import_paste_hint)
        textSize = 16f
        minLines = 3
        maxLines = 6
        setBackgroundResource(R.drawable.bg_input)
        setPadding(dp(16), dp(12), dp(16), dp(12))
    }
    val wrapper = FrameLayout(this).apply {
        setPadding(dp(24), dp(8), dp(24), 0)
        addView(input, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
    }
    val dialog = MaterialAlertDialogBuilder(this)
        .setTitle("📩  " + getString(R.string.import_paste_title))
        .setMessage(R.string.import_paste_help)
        .setView(wrapper)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.import_list, null)
        .create()
    dialog.setOnShowListener {
        // Only close the dialog when the pasted text holds a list; otherwise let the user fix it.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (ListShareCodec.containsCode(input.text)) {
                dialog.dismiss()
                importSharedList(input.text, onImported)
            } else {
                Toast.makeText(this, R.string.import_invalid, Toast.LENGTH_LONG).show()
            }
        }
    }
    dialog.show()
}
