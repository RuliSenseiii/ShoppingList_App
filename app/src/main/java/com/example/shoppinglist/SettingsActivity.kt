package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.ui.PopupAction
import com.example.shoppinglist.ui.PopupBadge
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.TutorialGuide
import com.example.shoppinglist.ui.TutorialScreen
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.roundedBackground
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.soft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings: save everything to a file and restore it later (e.g. on a new phone), and the order
 * of the categories used by "Ruta del súper".
 */
class SettingsActivity : AppCompatActivity(), TutorialScreen {

    override val screenId = "settings"

    private val repository by lazy { ListRepository.get(this) }

    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) writeBackup(uri)
    }
    private val pickBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) askBeforeRestoring(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        setupTopBar(getString(R.string.settings_title))
        val content = findViewById<LinearLayout>(R.id.content)

        content.addView(card("💾", R.string.settings_backup, R.string.settings_backup_sub, green = true, tag = "tut:settings_backup") {
            TutorialGuide.emit(this, "backup_opened")
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            createBackup.launch("copia-lista-compra-$day.json")
        })
        content.addView(card("📂", R.string.settings_restore, R.string.settings_restore_sub, green = false) {
            pickBackup.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        })
        content.addView(card("↕️", R.string.settings_order, R.string.settings_order_sub, green = true) {
            startActivity(CategoryOrderActivity.intent(this))
        })
        // Last of the settings: what each version brought and changed.
        content.addView(card("📋", R.string.settings_notes, R.string.settings_notes_sub, green = false) {
            startActivity(ReleaseNotesActivity.intent(this))
        })
    }

    override fun onResume() {
        super.onResume()
        TutorialGuide.attach(this)
    }

    private fun card(emoji: String, title: Int, subtitle: Int, green: Boolean, tag: String? = null, onClick: () -> Unit): View {
        val accent = ContextCompat.getColor(this, if (green) R.color.green_dark else R.color.orange_dark)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (green) R.drawable.bg_card else R.drawable.bg_card_missing)
            elevation = dp(3).toFloat()
            setPadding(dp(16), dp(16), dp(12), dp(16))
            isClickable = true
            isFocusable = true
            this.tag = tag
            setOnClickListener { onClick() }
        }
        card.addView(FrameLayout(this).apply {
            background = roundedBackground(soft(accent, 0x30), 0f, oval = true)
            addView(TextView(context).apply {
                text = emoji
                textSize = 26f
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = getString(title)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        texts.addView(TextView(this).apply {
            text = getString(subtitle)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        })
        card.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(16) })
        card.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setColorFilter(accent)
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(14) }
        return card
    }

    private fun writeBackup(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(repository.exportBackup().toByteArray(Charsets.UTF_8)) }
                ?: error("No output stream")
            Toast.makeText(this, R.string.backup_saved, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_LONG).show()
        }
    }

    /** Restoring replaces everything, so it is checked first and confirmed. */
    private fun askBeforeRestoring(uri: Uri) {
        val text = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
        if (text == null || !text.contains("listacompra-backup")) {
            Toast.makeText(this, R.string.restore_invalid, Toast.LENGTH_LONG).show()
            return
        }
        Popups.show(
            activity = this,
            title = getString(R.string.restore_question),
            subtitle = getString(R.string.restore_sub),
            badge = PopupBadge("📂", ContextCompat.getColor(this, R.color.orange)),
            actions = listOf(
                PopupAction(getString(R.string.restore_action), R.drawable.ic_check, green = true) {
                    if (repository.importBackup(text)) {
                        Toast.makeText(this, R.string.restore_done, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.restore_invalid, Toast.LENGTH_LONG).show()
                    }
                },
                PopupAction(getString(R.string.cancel), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    companion object {
        fun intent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
