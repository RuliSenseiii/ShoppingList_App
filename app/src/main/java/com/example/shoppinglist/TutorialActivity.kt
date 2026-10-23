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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.data.ShoppingList
import com.example.shoppinglist.ui.Tutorial
import com.example.shoppinglist.ui.TutorialStep
import com.example.shoppinglist.ui.TutorialTarget
import com.example.shoppinglist.ui.TutorialTopic
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.gradientButton
import com.example.shoppinglist.ui.roundedBackground
import com.example.shoppinglist.ui.setupTopBar
import com.example.shoppinglist.ui.soft
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Interactive tutorial, always reachable from the main screen.
 * It first lists the topics so the user can pick only the ones they want, then walks through the
 * chosen topic step by step; a step can open the real screen so the user tries it straight away.
 */
class TutorialActivity : AppCompatActivity() {

    private lateinit var content: LinearLayout
    private lateinit var bottomButton: MaterialButton

    /** Index in [Tutorial.topics] of the topic being followed, or -1 while choosing one. */
    private var topicIndex = -1
    private var stepIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        content = findViewById(R.id.content)
        bottomButton = findViewById(R.id.btnBottom)
        topicIndex = savedInstanceState?.getInt(STATE_TOPIC, -1) ?: -1
        stepIndex = savedInstanceState?.getInt(STATE_STEP, 0) ?: 0

        // Inside a topic, back returns to the topic list; from the list it leaves the tutorial.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (topicIndex >= 0) showTopics() else finish()
            }
        })

        if (topicIndex >= 0) showStep() else showTopics()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TOPIC, topicIndex)
        outState.putInt(STATE_STEP, stepIndex)
    }

    /** Sets the title and makes the back arrow follow the tutorial's own back behaviour. */
    private fun setupBar(title: String) {
        setupTopBar(title)
        findViewById<View>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun showTopics() {
        topicIndex = -1
        stepIndex = 0
        setupBar(getString(R.string.tutorial_title))
        bottomButton.visibility = View.GONE
        content.removeAllViews()
        content.addView(TextView(this).apply {
            text = getString(R.string.tutorial_intro)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            setPadding(dp(4), 0, dp(4), dp(16))
        })
        Tutorial.topics.forEachIndexed { index, topic ->
            content.addView(topicCard(topic) {
                topicIndex = index
                stepIndex = 0
                showStep()
            }, cardParams())
        }
    }

    private fun topicCard(topic: TutorialTopic, onClick: () -> Unit): View {
        val accent = ContextCompat.getColor(this, if (topic.green) R.color.green_dark else R.color.orange_dark)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (topic.green) R.drawable.bg_card else R.drawable.bg_card_missing)
            elevation = dp(3).toFloat()
            setPadding(dp(16), dp(14), dp(12), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        card.addView(emojiBadge(topic.emoji, accent, 52, 26))
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = topic.title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        texts.addView(TextView(this).apply {
            text = topic.summary
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

    private fun showStep() {
        val topic = Tutorial.topics[topicIndex]
        val step = topic.steps[stepIndex]
        val last = stepIndex == topic.steps.lastIndex
        val accent = ContextCompat.getColor(this, if (topic.green) R.color.green_dark else R.color.orange_dark)
        setupBar(topic.title)
        content.removeAllViews()

        content.addView(TextView(this).apply {
            text = getString(R.string.tutorial_step_count, stepIndex + 1, topic.steps.size)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        content.addView(LinearProgressIndicator(this).apply {
            max = topic.steps.size
            progress = stepIndex + 1
            trackThickness = dp(8)
            trackCornerRadius = dp(4)
            setIndicatorColor(accent)
            trackColor = ContextCompat.getColor(context, R.color.green_light)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
            bottomMargin = dp(20)
        })

        content.addView(stepCard(step, accent), cardParams())

        step.tryIt?.let { target ->
            content.addView(
                gradientButton(this, target.label, R.drawable.ic_chevron_right, green = !topic.green).apply {
                    setOnClickListener { startActivity(intentFor(target)) }
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(8) },
            )
        }
        if (stepIndex > 0) {
            content.addView(TextView(this).apply {
                text = getString(R.string.tutorial_previous)
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(accent)
                setPadding(dp(12), dp(14), dp(12), dp(14))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    stepIndex--
                    showStep()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            })
        }

        bottomButton.apply {
            visibility = View.VISIBLE
            text = getString(if (last) R.string.tutorial_finish else R.string.tutorial_next)
            setIconResource(if (last) R.drawable.ic_check else R.drawable.ic_chevron_right)
            setOnClickListener {
                if (last) {
                    showTopics()
                } else {
                    stepIndex++
                    showStep()
                }
            }
        }
    }

    private fun stepCard(step: TutorialStep, accent: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.bg_card)
            elevation = dp(3).toFloat()
            setPadding(dp(22), dp(28), dp(22), dp(28))
        }
        card.addView(emojiBadge(step.emoji, accent, 88, 46))
        card.addView(TextView(this).apply {
            text = step.title
            textSize = 22f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(18)
        })
        card.addView(TextView(this).apply {
            text = step.text
            textSize = 17f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.15f)
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12)
        })
        return card
    }

    private fun emojiBadge(emoji: String, accent: Int, sizeDp: Int, textDp: Int) = FrameLayout(this).apply {
        background = roundedBackground(soft(accent, 0x30), 0f, oval = true)
        addView(TextView(context).apply {
            text = emoji
            textSize = textDp.toFloat()
            gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    private fun cardParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { bottomMargin = dp(14) }

    private fun intentFor(target: TutorialTarget): Intent = when (target) {
        TutorialTarget.NEW_LIST -> AddProductActivity.newListIntent(this)
        TutorialTarget.MY_LISTS -> ListsActivity.intent(this, ListsActivity.Mode.MANAGE)
        TutorialTarget.MISSING -> AddProductActivity.missingListIntent(this)
        TutorialTarget.HOME -> ListDetailActivity.intent(this, ShoppingList.HOME_LIST_ID)
        TutorialTarget.SHOP -> ListsActivity.intent(this, ListsActivity.Mode.SHOP)
    }

    companion object {
        private const val STATE_TOPIC = "state_topic"
        private const val STATE_STEP = "state_step"

        fun intent(context: Context) = Intent(context, TutorialActivity::class.java)
    }
}
