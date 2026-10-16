package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shoppinglist.data.ListRepository
import com.example.shoppinglist.data.Product
import com.example.shoppinglist.ui.PopupAction
import com.example.shoppinglist.ui.PopupBadge
import com.example.shoppinglist.ui.Popups
import com.example.shoppinglist.ui.ProductListRenderer
import com.example.shoppinglist.ui.addEmptyState
import com.example.shoppinglist.ui.applySystemBarInsets
import com.example.shoppinglist.ui.dp
import com.example.shoppinglist.ui.gradientButton
import com.example.shoppinglist.ui.setupTopBar
import com.google.android.material.progressindicator.LinearProgressIndicator

/** "VOY A COMPRAR": go through a list crossing products out as they go into the cart. */
class ShoppingActivity : AppCompatActivity() {

    private val repository by lazy { ListRepository.get(this) }
    private lateinit var listId: String
    private lateinit var content: LinearLayout

    private val inCart = mutableSetOf<String>()
    private var reviewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        listId = intent.getStringExtra(EXTRA_LIST_ID) ?: return finish()
        content = findViewById(R.id.content)

        savedInstanceState?.let {
            inCart.addAll(it.getStringArrayList(STATE_IN_CART).orEmpty())
            reviewing = it.getBoolean(STATE_REVIEWING)
        }
        render()

        if (savedInstanceState == null && repository.missingList().products.isNotEmpty()) {
            askAboutMissingProducts()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_IN_CART, ArrayList(inCart))
        outState.putBoolean(STATE_REVIEWING, reviewing)
    }

    private fun askAboutMissingProducts() {
        Popups.show(
            activity = this,
            title = getString(R.string.show_missing_question),
            subtitle = getString(R.string.show_missing_sub),
            badge = PopupBadge("📝", ContextCompat.getColor(this, R.color.orange)),
            actions = listOf(
                PopupAction(getString(R.string.yes), R.drawable.ic_check, green = true) {
                    repository.moveMissingInto(listId)
                    render()
                },
                PopupAction(getString(R.string.no), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    private fun render() {
        val list = repository.get(listId) ?: return finish()
        setupTopBar(list.name)
        inCart.retainAll(list.products.map { it.id }.toSet())

        val onClick: ((Product) -> Unit)? = if (reviewing) null else ::onProductClick
        ProductListRenderer.render(content, list.products, inCart, onClick)

        if (list.products.isEmpty()) {
            content.addEmptyState("🧺", getString(R.string.empty_list))
        } else if (!reviewing) {
            content.addView(progressCard(inCart.size, list.products.size), 0)
        }
        if (reviewing) addDeleteListButton()
    }

    /** "🛒 En el carro  3 / 7" with a progress bar, shown above the list while shopping. */
    private fun progressCard(done: Int, total: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_progress_card)
            setPadding(dp(18), dp(14), dp(18), dp(16))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = getString(R.string.cart_progress_title)
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.green_dark))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = getString(R.string.cart_progress_count, done, total)
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.orange_dark))
        })
        card.addView(row)
        card.addView(LinearProgressIndicator(this).apply {
            max = total
            progress = done
            trackThickness = dp(10)
            trackCornerRadius = dp(5)
            setIndicatorColor(ContextCompat.getColor(context, R.color.green_mid))
            trackColor = ContextCompat.getColor(context, R.color.white)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(4) }
        return card
    }

    private fun onProductClick(product: Product) {
        if (product.id in inCart) {
            // Tapping a crossed-out product undoes it, in case it was marked by mistake.
            inCart.remove(product.id)
            render()
            return
        }
        Popups.show(
            activity = this,
            title = "${product.name}  ${product.formattedQuantity}",
            subtitle = product.category.label,
            badge = PopupBadge(product.category.emoji, ContextCompat.getColor(this, product.category.colorRes)),
            actions = listOf(
                PopupAction(getString(R.string.in_cart), R.drawable.ic_cart, green = true) {
                    inCart.add(product.id)
                    render()
                    checkCompleted()
                },
                PopupAction(getString(R.string.missing), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    private fun checkCompleted() {
        val products = repository.get(listId)?.products ?: return
        if (products.isEmpty() || !products.all { it.id in inCart }) return
        Popups.show(
            activity = this,
            title = getString(R.string.list_completed),
            subtitle = getString(R.string.list_completed_sub),
            badge = PopupBadge("🎉", ContextCompat.getColor(this, R.color.green)),
            actions = listOf(
                PopupAction(getString(R.string.delete_list), R.drawable.ic_delete, green = false) { deleteListAndGoHome() },
                PopupAction(getString(R.string.review_list), R.drawable.ic_visibility, green = true) {
                    reviewing = true
                    inCart.clear()
                    render()
                },
            ),
            showClose = false,
        )
    }

    private fun addDeleteListButton() {
        val button = gradientButton(this, getString(R.string.delete_list), R.drawable.ic_delete, green = false).apply {
            setOnClickListener { deleteListAndGoHome() }
        }
        content.addView(
            button,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(28) },
        )
    }

    private fun deleteListAndGoHome() {
        repository.deleteList(listId)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        private const val EXTRA_LIST_ID = "list_id"
        private const val STATE_IN_CART = "state_in_cart"
        private const val STATE_REVIEWING = "state_reviewing"

        fun intent(context: Context, listId: String) =
            Intent(context, ShoppingActivity::class.java).putExtra(EXTRA_LIST_ID, listId)
    }
}
