package com.example.shoppinglist.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.shoppinglist.ListsActivity
import com.example.shoppinglist.R
import com.example.shoppinglist.data.Category
import com.example.shoppinglist.data.ListFileFormat
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.ListShareCodec
import com.example.shoppinglist.data.SharedList
import com.example.shoppinglist.data.ShoppingList
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "Sharing"

/** Folder in the app cache for the files being sent; [R.xml.file_paths] exposes it to other apps. */
private const val SHARED_DIR = "shared"

/**
 * Sends [list] as a ".listacompra" file through any app (WhatsApp, Telegram, email...). Tapping the
 * file on the receiver's phone opens this app, ready to import it.
 */
fun Activity.shareList(list: ShoppingList) {
    if (list.products.isEmpty()) {
        Toast.makeText(this, R.string.share_empty_list, Toast.LENGTH_LONG).show()
        return
    }
    val uri = try {
        val dir = File(cacheDir, SHARED_DIR).apply {
            mkdirs()
            // Old files are not needed once sent.
            listFiles()?.forEach { it.delete() }
        }
        val file = File(dir, ListFileFormat.fileName(list))
        file.writeText(ListFileFormat.write(list), Charsets.UTF_8)
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    } catch (e: Exception) {
        Log.e(TAG, "Could not create the list file", e)
        Toast.makeText(this, R.string.share_failed, Toast.LENGTH_LONG).show()
        return
    }
    val count = resources.getQuantityString(R.plurals.products_count, list.products.size, list.products.size)
    val send = Intent(Intent.ACTION_SEND)
        .setType(ListFileFormat.MIME_TYPE)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_message_title, list.name))
        .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_file_caption, list.name, count))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(send, getString(R.string.share_list))
        // Sharing the list with this same app would just import a copy of it.
        .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this, ListsActivity::class.java)))
    startActivity(chooser)
}

/** Imports a received ".listacompra" file after asking the user. */
fun Activity.importSharedFile(uri: Uri, onImported: (ShoppingList) -> Unit) {
    val shared = try {
        readText(uri)?.let { ListFileFormat.read(it) }
    } catch (e: Exception) {
        Log.w(TAG, "Could not read the received file", e)
        null
    }
    confirmImport(shared, onImported)
}

/** Imports a list from a V0.2/V0.3 message with a text code, after asking the user. */
fun Activity.importSharedText(text: CharSequence?, onImported: (ShoppingList) -> Unit) {
    confirmImport(ListShareCodec.decode(text), onImported)
}

/** Types the file picker offers: chat apps and file managers often label unknown files as binary. */
val IMPORT_FILE_TYPES = arrayOf(ListFileFormat.MIME_TYPE, "application/octet-stream", "text/*")

/** Asks "¿Añadir ... a tus listas?" and saves the list, or shows an error if there is no list. */
private fun Activity.confirmImport(shared: SharedList?, onImported: (ShoppingList) -> Unit) {
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

/** Reads a small text file; returns null if it is too big to be a list. */
private fun Activity.readText(uri: Uri): String? {
    val input = contentResolver.openInputStream(uri) ?: return null
    input.use { stream ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            out.write(buffer, 0, count)
            if (out.size() > ListFileFormat.MAX_FILE_BYTES) return null
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
