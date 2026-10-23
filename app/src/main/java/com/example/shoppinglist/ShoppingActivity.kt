package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

/**
 * "VOY A COMPRAR": go through a list crossing products out as they go into the cart.
 * Once the list is completed, what was bought is added to the "Recuento en casa".
 */
class ShoppingActivity : AppCompatActivity() {

    private val repository by lazy { ListRepository.get(this) }
    private lateinit var listId: String
    private lateinit var content: LinearLayout

    private var reviewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_content)
        applySystemBarInsets()
        listId = intent.getStringExtra(EXTRA_LIST_ID) ?: return finish()
        content = findViewById(R.id.content)

        reviewing = savedInstanceState?.getBoolean(STATE_REVIEWING) ?: false
        render()

        if (savedInstanceState == null) {
            if (repository.missingList().products.isNotEmpty()) {
                askAboutMissingProducts()
            } else {
                // The list may have been left with everything already in the cart.
                checkCompleted()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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
        // What is in the cart is saved with the list, so leaving this screen does not lose it.
        val inCart = if (reviewing) emptySet() else list.products.map { it.id }.filter { it in list.inCart }.toSet()

        val onClick: ((Product) -> Unit)? = if (reviewing) null else ::onProductClick
        ProductListRenderer.render(content, list.products, inCart, onClick)

        if (list.products.isEmpty()) {
            content.addEmptyState("🧺", getString(R.string.empty_list))
        } else if (!reviewing) {
            content.addView(progressCard(inCart.size, list.products.size), 0)
        }
        if (reviewing) addDeleteListButton() else if (inCart.isNotEmpty()) addFinishButton(inCart.size, list.products.size)
    }

    /** The shopping can end at any moment, even with products still out of the cart. */
    private fun addFinishButton(done: Int, total: Int) {
        val button = gradientButton(this, getString(R.string.finish_shopping), R.drawable.ic_check, green = true).apply {
            setOnClickListener { if (done < total) confirmFinish(done, total) else checkCompleted() }
        }
        content.addView(
            button,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(28) },
        )
    }

    private fun confirmFinish(done: Int, total: Int) {
        Popups.show(
            activity = this,
            title = getString(R.string.finish_shopping_question),
            subtitle = getString(R.string.finish_shopping_sub, done, total),
            badge = PopupBadge("🛒", ContextCompat.getColor(this, R.color.green)),
            actions = listOf(
                PopupAction(getString(R.string.yes), R.drawable.ic_check, green = true) {
                    // Counted and taken out of the list right away, so leaving afterwards never counts it twice.
                    addBoughtToHome()
                    repository.removeBought(listId)
                    render()
                    showLeftoverOptions()
                },
                PopupAction(getString(R.string.no), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    /** The list now only has what was not bought. */
    private fun showLeftoverOptions() {
        Popups.show(
            activity = this,
            title = getString(R.string.shopping_finished),
            subtitle = getString(R.string.shopping_finished_sub),
            badge = PopupBadge("🛒", ContextCompat.getColor(this, R.color.green)),
            actions = listOf(
                PopupAction(getString(R.string.review_list), R.drawable.ic_visibility, green = true) {
                    reviewing = true
                    render()
                },
                PopupAction(getString(R.string.save_list), R.drawable.ic_check, green = true) { goHome() },
                PopupAction(getString(R.string.delete_list), R.drawable.ic_delete, green = false) { deleteListAndGoHome() },
            ),
            showClose = false,
        )
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
        if (product.id in repository.get(listId)?.inCart.orEmpty()) {
            // Tapping a crossed-out product undoes it, in case it was marked by mistake.
            repository.setInCart(listId, product.id, false)
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
                    repository.setInCart(listId, product.id, true)
                    render()
                    checkCompleted()
                },
                PopupAction(getString(R.string.missing), R.drawable.ic_cancel, green = false) {},
            ),
            showClose = false,
        )
    }

    /**
     * The home count is updated when one of the pop-up buttons is pressed: the pop-up cannot be
     * dismissed and comes back if the user leaves, so each completed trip is counted exactly once.
     */
    private fun checkCompleted() {
        val list = repository.get(listId) ?: return
        if (list.products.isEmpty() || !list.products.all { it.id in list.inCart }) return
        showFinishOptions()
    }

    private fun showFinishOptions() {
        Popups.show(
            activity = this,
            title = getString(R.string.list_completed),
            subtitle = getString(R.string.list_completed_sub),
            badge = PopupBadge("🎉", ContextCompat.getColor(this, R.color.green)),
            actions = listOf(
                PopupAction(getString(R.string.delete_list), R.drawable.ic_delete, green = false) {
                    addBoughtToHome()
                    deleteListAndGoHome()
                },
                PopupAction(getString(R.string.review_list), R.drawable.ic_visibility, green = true) {
                    addBoughtToHome()
                    reviewing = true
                    repository.clearCart(listId)
                    render()
                },
            ),
            showClose = false,
        )
    }

    private fun addBoughtToHome() {
        repository.addBoughtToHome(listId)
        Toast.makeText(this, R.string.bought_added_home, Toast.LENGTH_SHORT).show()
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
        goHome()
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        private const val EXTRA_LIST_ID = "list_id"
        private const val STATE_REVIEWING = "state_reviewing"

        fun intent(context: Context, listId: String) =
            Intent(context, ShoppingActivity::class.java).putExtra(EXTRA_LIST_ID, listId)
    }
}
