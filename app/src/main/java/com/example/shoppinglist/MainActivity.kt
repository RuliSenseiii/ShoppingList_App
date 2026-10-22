package com.example.shoppinglist

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.ShoppingList
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
