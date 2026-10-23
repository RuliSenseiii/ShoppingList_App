package com.example.shoppinglist

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.ShoppingList
import androidx.core.content.ContextCompat
import com.example.shoppinglist.ui.PopupAction
import com.example.shoppinglist.ui.PopupBadge
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.applySystemBarInsets

class MainActivity : AppCompatActivity() {

    private val repository by lazy { ListRepository.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        findViewById<View>(R.id.bannerMyLists).setOnClickListener {
            startActivity(ListsActivity.intent(this, ListsActivity.Mode.MANAGE))
        }
        findViewById<View>(R.id.bannerNewList).setOnClickListener {
            startActivity(AddProductActivity.newListIntent(this))
        }
        findViewById<View>(R.id.bannerAddProduct).setOnClickListener {
            startActivity(AddProductActivity.missingListIntent(this))
        }
        findViewById<View>(R.id.bannerHome).setOnClickListener {
            startActivity(ListDetailActivity.intent(this, ShoppingList.HOME_LIST_ID))
        }
        findViewById<View>(R.id.btnGoShopping).setOnClickListener {
            startActivity(ListsActivity.intent(this, ListsActivity.Mode.SHOP))
        }
        findViewById<View>(R.id.btnTutorial).setOnClickListener {
            startActivity(TutorialActivity.intent(this))
        }
        if (savedInstanceState == null) offerTutorialOnFirstLaunch()
    }

    /** New users are asked once; the tutorial stays available from the "?" button either way. */
    private fun offerTutorialOnFirstLaunch() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_TUTORIAL_OFFERED, false)) return
        // Someone updating the app already has lists: do not interrupt them.
        if (repository.userLists().isNotEmpty()) {
            prefs.edit().putBoolean(KEY_TUTORIAL_OFFERED, true).apply()
            return
        }
        prefs.edit().putBoolean(KEY_TUTORIAL_OFFERED, true).apply()
        Popups.show(
            activity = this,
            title = getString(R.string.tutorial_offer_title),
            subtitle = getString(R.string.tutorial_offer_sub),
            badge = PopupBadge("👋", ContextCompat.getColor(this, R.color.green)),
            actions = listOf(
                PopupAction(getString(R.string.tutorial_offer_yes), R.drawable.ic_help, green = true) {
                    startActivity(TutorialActivity.intent(this))
                },
                PopupAction(getString(R.string.tutorial_offer_no), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    private companion object {
        const val PREFS = "app_state"
        const val KEY_TUTORIAL_OFFERED = "tutorial_offered"
    }

    override fun onResume() {
        super.onResume()
        // Live counters under the banners.
        val lists = repository.userLists().size
        findViewById<TextView>(R.id.tvMyListsSub).text =
            if (lists == 0) getString(R.string.banner_my_lists_empty)
            else resources.getQuantityString(R.plurals.banner_my_lists_sub, lists, lists)

        val missing = repository.missingList().products.size
        findViewById<TextView>(R.id.tvAddProductSub).text =
            if (missing == 0) getString(R.string.banner_add_product_empty)
            else resources.getQuantityString(R.plurals.banner_add_product_sub, missing, missing)

        val atHome = repository.homeList().products.size
        findViewById<TextView>(R.id.tvHomeSub).text =
            if (atHome == 0) getString(R.string.banner_home_empty)
            else resources.getQuantityString(R.plurals.banner_home_sub, atHome, atHome)
    }
}
