package com.example.shoppinglist.ui

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.shoppinglist.R
import com.example.shoppinglist.TutorialActivity
import com.example.shoppinglist.data.ListRepository
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Implemented by the screens the tutorial can guide through. */
interface TutorialScreen {
    /** Id used by the steps' `screen` and by the "screen:<id>" events. */
    val screenId: String

    /**
     * Emits again the events for what the screen already shows (a name typed, a category chosen...),
     * so a step is never stuck because the user did things in another order.
     */
    fun replayTutorialEvents() {}
}

/**
 * Runs the interactive tutorial: it keeps the topic and step being followed and draws, over the real
 * screens, a spotlight on the button to use plus a card with the instruction. A step is completed
 * only when the screen reports its event with [emit]; the user can leave with [stop] at any time.
 */
object TutorialGuide {

    private var topic: TutorialTopic? = null
    private var stepIndex = 0
    private var replaying = false

    private val screens = WeakHashMap<Activity, String>()
    private val overlays = WeakHashMap<Activity, TutorialOverlay>()
    private val dialogs = mutableListOf<WeakReference<Dialog>>()
    private val pulses = WeakHashMap<View, ObjectAnimator>()

    val active: Boolean get() = topic != null

    fun start(topic: TutorialTopic) {
        this.topic = topic
        stepIndex = 0
    }

    /** Ends the tutorial wherever the user is. */
    fun stop() {
        topic = null
        stepIndex = 0
        removeOverlays()
        refreshDialogs()
    }

    /** Whether the saved data allows following [topic] (some steps need lists to exist). */
    fun missingRequirement(activity: Activity, topic: TutorialTopic): String? {
        val lists = ListRepository.get(activity).userLists()
        val ok = when (topic.requires) {
            TutorialRequirement.NONE -> true
            TutorialRequirement.ONE_LIST -> lists.any { it.products.isNotEmpty() }
            TutorialRequirement.TWO_LISTS -> lists.size >= 2
            TutorialRequirement.SHOP_LIST -> lists.any { it.products.size >= 2 }
            TutorialRequirement.HOME_PRODUCT -> ListRepository.get(activity).homeList().products.isNotEmpty()
        }
        return if (ok) null else topic.requires.message
    }

    /** Called by each screen when it is shown. */
    fun attach(activity: Activity) {
        val screen = (activity as? TutorialScreen)?.screenId ?: return
        screens[activity] = screen
        if (!active) {
            removeOverlay(activity)
            return
        }
        emit(activity, "screen:$screen")
    }

    /** A screen reports that something happened; the step completes if it was waiting for that. */
    fun emit(activity: Activity, event: String) {
        val current = currentStep() ?: return
        if (screens[activity] == null) return
        if (event == current.done) advance(activity)
        if (!replaying) refresh(activity)
    }

    fun currentStep(): TutorialStep? = topic?.steps?.getOrNull(stepIndex)

    fun stepNumber() = stepIndex + 1
    fun stepCount() = topic?.steps?.size ?: 0
    fun topicTitle() = topic?.title.orEmpty()

    private fun advance(activity: Activity) {
        val current = topic ?: return
        stepIndex++
        if (stepIndex >= current.steps.size) {
            topic = null
            stepIndex = 0
            removeOverlays()
            refreshDialogs()
            activity.window.decorView.post { showOutro(activity, current) }
        }
    }

    private fun showOutro(activity: Activity, finished: TutorialTopic) {
        if (activity.isFinishing || activity.isDestroyed) return
        Popups.show(
            activity = activity,
            title = activity.getString(R.string.tutorial_topic_done),
            subtitle = finished.outro,
            badge = PopupBadge("🎉", ContextCompat.getColor(activity, R.color.green)),
            actions = listOf(
                PopupAction(activity.getString(R.string.tutorial_more_topics), R.drawable.ic_help, green = true) {
                    activity.startActivity(TutorialActivity.intent(activity))
                },
                PopupAction(activity.getString(R.string.close), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    private fun refresh(activity: Activity) {
        if (!active) {
            removeOverlay(activity)
            refreshDialogs()
            return
        }
        // Let the screen report what it already has; steps may complete one after another.
        (activity as? TutorialScreen)?.let {
            replaying = true
            try {
                it.replayTutorialEvents()
            } finally {
                replaying = false
            }
        }
        val step = currentStep()
        if (step == null) {
            removeOverlay(activity)
            refreshDialogs()
            return
        }
        val overlay = overlays.getOrPut(activity) {
            TutorialOverlay(activity).also {
                activity.findViewById<ViewGroup>(android.R.id.content)
                    .addView(it, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        }
        overlay.update(step, stepNumber(), stepCount(), topicTitle(), mismatch = screens[activity] != step.screen)
        refreshDialogs()
    }

    private fun removeOverlay(activity: Activity) {
        overlays.remove(activity)?.let { (it.parent as? ViewGroup)?.removeView(it) }
    }

    private fun removeOverlays() {
        overlays.keys.toList().forEach { removeOverlay(it) }
    }

    // ---- Pop-ups: they are separate windows, so their buttons are highlighted here. ----

    /** Registers an open pop-up so its tagged buttons can be highlighted while a step points at them. */
    fun dialogShown(activity: Activity, dialog: Dialog) {
        dialogs.add(WeakReference(dialog))
        dialog.setOnDismissListener {
            highlightIn(dialog, false)
            dialogs.removeAll { it.get() == null || it.get() === dialog }
            if (active) emit(activity, "popup_closed")
        }
        highlightIn(dialog, true)
    }

    private fun refreshDialogs() {
        dialogs.removeAll { it.get() == null }
        dialogs.forEach { ref -> ref.get()?.let { highlightIn(it, true) } }
    }

    /** Pulses the pop-up button the current step points at, and calms the others. */
    private fun highlightIn(dialog: Dialog, enabled: Boolean) {
        val root = dialog.window?.decorView ?: return
        val step = currentStep()
        val wanted = if (enabled && step != null && step.inDialog) "tut:${step.target}" else null
        forEachTagged(root) { view, tag ->
            if (tag == wanted) startPulse(view) else stopPulse(view)
        }
    }

    private fun forEachTagged(view: View, action: (View, String) -> Unit) {
        (view.tag as? String)?.takeIf { it.startsWith("tut:") }?.let { action(view, it) }
        if (view is ViewGroup) for (i in 0 until view.childCount) forEachTagged(view.getChildAt(i), action)
    }

    private fun startPulse(view: View) {
        if (pulses[view]?.isRunning == true) return
        pulses[view] = ObjectAnimator.ofPropertyValuesHolder(
            view,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.04f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.04f),
        ).apply {
            duration = 600
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopPulse(view: View) {
        pulses.remove(view)?.cancel()
        view.scaleX = 1f
        view.scaleY = 1f
    }
}
