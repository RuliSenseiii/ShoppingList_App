package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.ui.ReleaseNote
import com.example.shoppinglist.ui.ReleaseNotes
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.roundedBackground
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.soft

/** "Notas de la app": what every version brought, newest first. */
class ReleaseNotesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        setupTopBar(getString(R.string.release_notes_title))
        val content = findViewById<LinearLayout>(R.id.content)

        val installed = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        content.addView(TextView(this).apply {
            text = getString(R.string.release_notes_installed, installed)
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            setPadding(dp(4), 0, dp(4), dp(14))
        })
        ReleaseNotes.all.forEachIndexed { index, note -> content.addView(card(note, latest = index == 0)) }
    }

    private fun card(note: ReleaseNote, latest: Boolean): View {
        val green = ContextCompat.getColor(this, R.color.green_dark)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            elevation = dp(3).toFloat()
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = note.version
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(green)
        })
        if (latest) {
            header.addView(TextView(this).apply {
                text = getString(R.string.release_notes_current)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.orange_dark))
                setPadding(dp(10), dp(3), dp(10), dp(3))
                background = roundedBackground(soft(ContextCompat.getColor(context, R.color.orange), 0x26), dp(100).toFloat())
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(10)
            })
        }
        card.addView(header)
        section(card, getString(R.string.release_notes_new), note.news)
        section(card, getString(R.string.release_notes_changed), note.changes)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(14) }
        return card
    }

    private fun section(card: LinearLayout, title: String, items: List<String>) {
        if (items.isEmpty()) return
        card.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.orange_dark))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
        items.forEach { item ->
            card.addView(TextView(this).apply {
                text = "•  $item"
                textSize = 15f
                setLineSpacing(0f, 1.1f)
                setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            })
        }
    }

    companion object {
        fun intent(context: Context) = Intent(context, ReleaseNotesActivity::class.java)
    }
}
