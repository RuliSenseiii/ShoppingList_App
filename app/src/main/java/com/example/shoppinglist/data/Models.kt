package com.example.shoppinglist.data

import java.util.Locale
import java.util.UUID

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: Category,
    val quantity: Double,
) {
    /** "1,5 kg" for weighed categories, "3 uds" for the rest. */
    val formattedQuantity: String
        get() = if (category.usesKilograms) {
            "${formatNumber(quantity)} kg"
        } else {
            val units = quantity.toLong()
            if (units == 1L) "1 ud" else "$units uds"
        }

    private fun formatNumber(value: Double): String =
        String.format(Locale.forLanguageTag("es-ES"), "%.3f", value)
            .trimEnd('0')
            .trimEnd(',')
}

/** A product used before, offered while typing a new one. [toString] is what the field filters on. */
data class ProductSuggestion(val name: String, val category: Category) {
    override fun toString(): String = name
}

data class ShoppingList(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val products: MutableList<Product> = mutableListOf(),
    /** Ids of the products already in the cart while shopping, kept until the list is completed. */
    val inCart: MutableSet<String> = mutableSetOf(),
) {
    val isMissingList: Boolean get() = id == MISSING_LIST_ID

    /** Adds the product, summing the quantity if the same product is already in the list. */
    fun addOrMerge(product: Product) {
        val index = products.indexOfFirst {
            it.category == product.category && it.name.equals(product.name.trim(), ignoreCase = true)
        }
        if (index >= 0) {
            val existing = products[index]
            products[index] = existing.copy(quantity = existing.quantity + product.quantity)
        } else {
            products.add(product)
        }
    }

    companion object {
        const val MISSING_LIST_ID = "missing"
    }
}
