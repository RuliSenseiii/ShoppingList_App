package com.example.shoppinglist.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.shoppinglist.R

/**
 * Drawn over a screen while the tutorial runs: a dimmed screen with a pulsing spotlight on the
 * button the user has to use, and a card with the instruction and an exit button.
 * It never takes touches except on the card, so the real buttons work as usual underneath.
 */
class TutorialOverlay(private val activity: Activity) : FrameLayout(activity) {

    private var step: TutorialStep? = null
    private var mismatch = false
    private var target: View? = null
    private var revealedFor: TutorialStep? = null

    private val spotlight = Spotlight(activity)
    private val header = TextView(activity)
    private val body = TextView(activity)
    private val card = LinearLayout(activity)

    private var cardOnTop = true
    private var cardTopMargin = -1
    private var cardBottomMargin = -1

    private val pulse = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            spotlight.pulse = it.animatedValue as Float
            spotlight.invalidate()
        }
    }

    private val preDraw = ViewTreeObserver.OnPreDrawListener {
        layoutFrame()
        true
    }

    init {
        // Dims and rings only draw; touches go through to the screen below.
        isClickable = false
        isFocusable = false
        addView(spotlight, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        card.apply {
            orientation = LinearLayout.VERTICAL
            elevation = context.dp(16).toFloat()
            isClickable = true // The card swallows its own touches.
            setPadding(context.dp(16), context.dp(12), context.dp(8), context.dp(14))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(18).toFloat()
                setColor(ContextCompat.getColor(context, R.color.white))
                setStroke(context.dp(2), ContextCompat.getColor(context, R.color.green_mid))
            }
        }
        val headerRow = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        header.apply {
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.green_dark))
        }
        headerRow.addView(header, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(TextView(activity).apply {
            text = context.getString(R.string.tutorial_exit)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.orange_dark))
            setPadding(context.dp(10), context.dp(6), context.dp(10), context.dp(6))
            background = roundedBackground(soft(ContextCompat.getColor(context, R.color.orange), 0x1F), context.dp(100).toFloat())
            isClickable = true
            setOnClickListener { TutorialGuide.stop() }
        })
        body.apply {
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            setPadding(0, context.dp(6), context.dp(8), 0)
        }
        card.addView(headerRow)
        card.addView(body)
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
            marginStart = context.dp(12)
            marginEnd = context.dp(12)
        })
    }

    fun update(step: TutorialStep, number: Int, total: Int, topicTitle: String, mismatch: Boolean) {
        this.step = step
        this.mismatch = mismatch
        header.text = context.getString(R.string.tutorial_header, number, total, topicTitle)
        body.text = if (mismatch) context.getString(R.string.tutorial_wrong_screen) else step.text
        if (revealedFor !== step) {
            revealedFor = step
            target = null
            // Scroll the target into view once per step, when it exists.
            post { resolveTarget()?.let { it.requestRectangleOnScreen(Rect(0, 0, it.width, it.height), false) } }
        }
    }

    private fun resolveTarget(): View? {
        val current = step ?: return null
        val key = when {
            mismatch -> if (activity.findViewById<View>(R.id.btnBack) != null) "btnBack" else "btnTutorial"
            current.inDialog -> return null
            else -> current.target
        }
        target?.takeIf { it.isAttachedToWindow }?.let { if (it.isShown) return it }
        val found = findByKey(activity.window.decorView, "tut:$key")
            ?: activity.resources.getIdentifier(key, "id", activity.packageName).takeIf { it != 0 }
                ?.let { activity.findViewById<View>(it) }
        target = found
        return found?.takeIf { it.isShown && it.width > 0 && it.height > 0 }
    }

    /** A view may carry several comma-separated tutorial tags. */
    private fun findByKey(view: View, key: String): View? {
        if ((view.tag as? String)?.split(',')?.contains(key) == true) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findByKey(view.getChildAt(i), key)?.let { return it }
        return null
    }

    /** Places the spotlight on the target and the card on the side of the screen away from it. */
    private fun layoutFrame() {
        val view = resolveTarget()
        val rect = view?.let {
            val visible = Rect()
            if (!it.getGlobalVisibleRect(visible)) return@let null
            val origin = IntArray(2)
            getLocationInWindow(origin)
            val windowOrigin = IntArray(2)
            activity.window.decorView.getLocationInWindow(windowOrigin)
            RectF(visible).apply { offset((windowOrigin[0] - origin[0]).toFloat(), (windowOrigin[1] - origin[1]).toFloat()) }
        }
        spotlight.rect = rect

        val onTop = rect == null || rect.centerY() > height / 2f
        val insets = ViewCompat.getRootWindowInsets(this)
        val bars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())
        val bottom = maxOf(bars?.bottom ?: 0, ime?.bottom ?: 0) + context.dp(12)
        val topBar = activity.findViewById<View>(R.id.topBar)
        val top = if (topBar != null) {
            val location = IntArray(2)
            topBar.getLocationInWindow(location)
            location[1] + topBar.height + context.dp(10) - activityTopOffset()
        } else {
            (bars?.top ?: 0) + context.dp(12)
        }
        if (onTop != cardOnTop || top != cardTopMargin || bottom != cardBottomMargin) {
            cardOnTop = onTop
            cardTopMargin = top
            cardBottomMargin = bottom
            (card.layoutParams as LayoutParams).apply {
                gravity = if (onTop) Gravity.TOP else Gravity.BOTTOM
                topMargin = if (onTop) top else 0
                bottomMargin = if (onTop) 0 else bottom
            }
            card.requestLayout()
        }
    }

    private fun activityTopOffset(): Int {
        val origin = IntArray(2)
        getLocationInWindow(origin)
        return origin[1]
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulse.start()
        viewTreeObserver.addOnPreDrawListener(preDraw)
    }

    override fun onDetachedFromWindow() {
        pulse.cancel()
        viewTreeObserver.removeOnPreDrawListener(preDraw)
        super.onDetachedFromWindow()
    }

    /** Dark veil with a rounded hole over [rect] and an orange ring around it. */
    private class Spotlight(context: Context) : View(context) {
        var rect: RectF? = null
            set(value) {
                if (field != value) {
                    field = value
                    invalidate()
                }
            }
        var pulse = 0f

        private val veil = Paint().apply { color = Color.argb(0x73, 0, 0, 0) }
        private val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dp(4).toFloat()
            color = ContextCompat.getColor(context, R.color.orange)
        }

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
            isClickable = false
        }

        override fun onDraw(canvas: Canvas) {
            val r = rect ?: return
            val grow = context.dp(6) + pulse * context.dp(5)
            val area = RectF(r.left - grow, r.top - grow, r.right + grow, r.bottom + grow)
            val radius = context.dp(18).toFloat()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), veil)
            canvas.drawRoundRect(area, radius, radius, hole)
            ring.alpha = (140 + 115 * (1 - pulse)).toInt()
            canvas.drawRoundRect(area, radius, radius, ring)
        }
    }
}
