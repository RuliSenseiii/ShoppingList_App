package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.data.Category
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.ShoppingList
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.addEmptyState
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.roundedBackground
import com.example.shoppinglist.ui.soft
import com.example.shoppinglist.ui.importFromClipboardOrAsk
import com.example.shoppinglist.ui.importSharedList
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.showUndo
import com.google.android.material.button.MaterialButton

/**
 * Shows the saved lists.
 * MANAGE (option 1): every list plus "Productos que me faltan"; opens the list to edit it.
 * SHOP ("VOY A COMPRAR"): only the user lists; opens the list in shopping mode.
 * In MANAGE mode lists sent by someone else can be imported, either with the "Importar lista" button
 * or by sharing the received message with the app (ACTION_SEND).
 */
class ListsActivity : AppCompatActivity() {

    enum class Mode { MANAGE, SHOP }

    private lateinit var mode: Mode
    private lateinit var content: LinearLayout
    private val repository by lazy { ListRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        mode = Mode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: Mode.MANAGE.name)
        setupTopBar(getString(if (mode == Mode.SHOP) R.string.choose_list_title else R.string.my_lists_title))
        content = findViewById(R.id.content)

        if (mode == Mode.MANAGE) {
            findViewById<MaterialButton>(R.id.btnBottom).apply {
                visibility = View.VISIBLE
                text = getString(R.string.import_list)
                setIconResource(R.drawable.ic_download)
                setOnClickListener { importFromClipboardOrAsk(::openImported) }
            }
        }
        if (savedInstanceState == null && intent.action == Intent.ACTION_SEND) {
            importSharedList(intent.getCharSequenceExtra(Intent.EXTRA_TEXT), ::openImported)
        }
    }

    private fun openImported(list: ShoppingList) {
        render()
        startActivity(ListDetailActivity.intent(this, list.id))
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        content.removeAllViews()
        if (mode == Mode.MANAGE) addCard(repository.missingList())

        val lists = repository.userLists()
        lists.forEach { addCard(it) }

        if (lists.isEmpty()) content.addEmptyState("📝", getString(R.string.no_lists))
    }

    private fun addCard(list: ShoppingList) {
        val missing = list.isMissingList
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (missing) R.drawable.bg_card_missing else R.drawable.bg_card)
            elevation = dp(3).toFloat()
            setPadding(dp(16), dp(16), dp(12), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { open(list) }
        }

        // Gradient circle with an icon.
        val icon = when {
            missing -> R.drawable.ic_playlist_add
            mode == Mode.SHOP -> R.drawable.ic_cart
            else -> R.drawable.ic_list
        }
        card.addView(FrameLayout(this).apply {
            setBackgroundResource(if (missing) R.drawable.bg_icon_circle_orange else R.drawable.bg_icon_circle_green)
            addView(ImageView(context).apply { setImageResource(icon) }, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(52), dp(52)))

        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = list.name
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, if (missing) R.color.orange_dark else R.color.green_dark))
        })
        texts.addView(TextView(this).apply {
            text = resources.getQuantityString(R.plurals.products_count, list.products.size, list.products.size)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        })
        // Emojis of the categories in the list, as a quick preview.
        val inCart = list.products.count { it.id in list.inCart }
        if (mode == Mode.SHOP && inCart > 0) {
            texts.addView(TextView(this).apply {
                text = getString(R.string.cart_progress_sub, inCart, list.products.size)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.orange_dark))
            })
        }
        val emojis = Category.entries.filter { c -> list.products.any { it.category == c } }.joinToString(" ") { it.emoji }
        if (emojis.isNotEmpty()) {
            texts.addView(TextView(this).apply {
                text = emojis
                textSize = 16f
                setTextColor(Color.BLACK)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(4), 0, 0)
            })
        }
        card.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(16)
        })

        if (mode == Mode.MANAGE && !missing) {
            card.addView(cardButton(R.drawable.ic_edit, getString(R.string.rename_list), R.color.green_mid) {
                rename(list)
            })
            card.addView(cardButton(R.drawable.ic_delete, getString(R.string.delete_list), R.color.orange) {
                delete(list)
            })
        } else {
            card.addView(ImageView(this).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(ContextCompat.getColor(context, if (missing) R.color.orange else R.color.green_mid))
            }, LinearLayout.LayoutParams(dp(28), dp(28)))
        }

        content.addView(
            card,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(14) },
        )
    }

    private fun open(list: ShoppingList) {
        startActivity(
            if (mode == Mode.SHOP) ShoppingActivity.intent(this, list.id)
            else ListDetailActivity.intent(this, list.id)
        )
    }

    /** Round icon button shown at the right of a list card. */
    private fun cardButton(@DrawableRes icon: Int, description: String, @ColorRes tint: Int, onClick: () -> Unit) =
        ImageButton(this).apply {
            setImageResource(icon)
            val color = ContextCompat.getColor(context, tint)
            setColorFilter(color)
            contentDescription = description
            background = roundedBackground(soft(color, 0x1F), 0f, oval = true)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(8) }
        }

    private fun rename(list: ShoppingList) {
        Popups.askListName(
            activity = this,
            title = getString(R.string.rename_list),
            initial = list.name,
            positiveLabel = R.string.save,
        ) { name ->
            repository.renameList(list.id, name)
            render()
        }
    }

    /** Deletes straight away; "Deshacer" brings the list back for a few seconds. */
    private fun delete(list: ShoppingList) {
        val deleted = repository.deleteList(list.id) ?: return
        render()
        showUndo(getString(R.string.list_deleted, list.name)) {
            repository.restoreList(deleted)
            render()
        }
    }

    companion object {
        private const val EXTRA_MODE = "mode"

        fun intent(context: Context, mode: Mode) =
            Intent(context, ListsActivity::class.java).putExtra(EXTRA_MODE, mode.name)
    }
}
