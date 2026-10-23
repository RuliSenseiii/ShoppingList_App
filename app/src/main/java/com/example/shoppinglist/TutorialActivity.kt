package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.ui.PopupAction
import com.example.shoppinglist.ui.PopupBadge
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.Tutorial
import com.example.shoppinglist.ui.TutorialGuide
import com.example.shoppinglist.ui.TutorialTopic
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.roundedBackground
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.soft

/**
 * Lets the user pick the tutorial topic they want. The chosen topic is then followed on the real
 * screens, starting from the main one: see [TutorialGuide].
 */
class TutorialActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        setupTopBar(getString(R.string.tutorial_title))
        content = findViewById(R.id.content)
        // Coming here (again) always starts from a clean state.
        TutorialGuide.stop()

        content.addView(TextView(this).apply {
            text = getString(R.string.tutorial_intro)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            setPadding(dp(4), 0, dp(4), dp(16))
        })
        Tutorial.topics.forEach { topic ->
            content.addView(topicCard(topic), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(14) })
        }
    }

    private fun topicCard(topic: TutorialTopic): View {
        val accent = ContextCompat.getColor(this, if (topic.green) R.color.green_dark else R.color.orange_dark)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (topic.green) R.drawable.bg_card else R.drawable.bg_card_missing)
            elevation = dp(3).toFloat()
            setPadding(dp(16), dp(14), dp(12), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { begin(topic) }
        }
        card.addView(FrameLayout(this).apply {
            background = roundedBackground(soft(accent, 0x30), 0f, oval = true)
            addView(TextView(context).apply {
                text = topic.emoji
                textSize = 26f
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(52), dp(52)))

        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = topic.title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        texts.addView(TextView(this).apply {
            text = "${topic.summary} · ${getString(R.string.tutorial_steps_count, topic.steps.size)}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        })
        card.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(16)
        })
        card.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setColorFilter(accent)
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        return card
    }

    /** Starts [topic] from the main screen, unless the app still lacks something the topic needs. */
    private fun begin(topic: TutorialTopic) {
        val missing = TutorialGuide.missingRequirement(this, topic)
        if (missing != null) {
            Popups.show(
                activity = this,
                title = topic.title,
                subtitle = missing,
                badge = PopupBadge(topic.emoji, ContextCompat.getColor(this, R.color.orange)),
                actions = listOf(PopupAction(getString(R.string.tutorial_understood), R.drawable.ic_check, green = true) {}),
                showClose = false,
            )
            return
        }
        TutorialGuide.start(topic)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        fun intent(context: Context) = Intent(context, TutorialActivity::class.java)
    }
}
