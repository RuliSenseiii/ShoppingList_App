package com.example.shoppinglist.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.shoppinglist.R

/** Wires the top bar shared by every screen except the main one: back arrow + title. */
fun Activity.setupTopBar(title: String) {
    findViewById<TextView>(R.id.tvTopTitle).text = title
    findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
}

/**
 * Apps targeting Android 15+ are drawn edge-to-edge. Keeps content clear of the status bar,
 * navigation bar and keyboard; the green top bar (if any) extends under the status bar.
 */
fun Activity.applySystemBarInsets() {
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    val topBar: View? = findViewById(R.id.topBar)
    val rootTop = root.paddingTop
    val rootBottom = root.paddingBottom
    val barTop = topBar?.paddingTop ?: 0

    // Dark status bar icons on the white main screen, light ones over the green top bar.
    WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars = topBar == null

    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        if (topBar != null) {
            topBar.setPadding(bars.left, barTop + bars.top, bars.right, topBar.paddingBottom)
            root.setPadding(root.paddingLeft, rootTop, root.paddingRight, rootBottom + maxOf(bars.bottom, ime.bottom))
        } else {
            root.setPadding(
                root.paddingLeft, rootTop + bars.top, root.paddingRight, rootBottom + maxOf(bars.bottom, ime.bottom)
            )
        }
        WindowInsetsCompat.CONSUMED
    }
}
