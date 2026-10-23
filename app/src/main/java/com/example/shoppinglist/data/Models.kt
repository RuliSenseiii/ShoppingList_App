package com.example.shoppinglist.data

import java.util.Locale
import java.util.UUID

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: Category,
    val quantity: Double,
    /** Only in the home count: when the quantity falls to this or less, the product is noted as missing. 0 = no alert. */
    val minQuantity: Double = 0.0,
    /** The alert for [minQuantity] has already been raised; cleared once the quantity is above it again. */
    val lowAlerted: Boolean = false,
    /** The unit the user chose for this product: true = kilos, false = units, null = the one of its category. */
    val kilograms: Boolean? = null,
) {
    /** Counted in kilos (with decimals) or in units, whatever the category says unless the user chose otherwise. */
    val usesKilograms: Boolean get() = kilograms ?: category.usesKilograms

    /** The same product counted in kilos or in units; choosing what the category already uses clears the override. */
    fun withUnit(kilos: Boolean): Product = copy(kilograms = if (kilos == category.usesKilograms) null else kilos)

    /** "1,5 kg" for weighed categories, "3 uds" for the rest. */
    val formattedQuantity: String get() = format(quantity)

    /** [minQuantity] written like [formattedQuantity]. */
    val formattedMin: String get() = format(minQuantity)

    /** How much one tap on + or − changes the quantity: a quarter of a kilo, or one unit. */
    val step: Double get() = if (usesKilograms) 0.25 else 1.0

    private fun format(value: Double): String =
        if (usesKilograms) {
            "${formatNumber(value)} kg"
        } else {
            val units = value.toLong()
            if (units == 1L) "1 ud" else "$units uds"
        }

    private fun formatNumber(value: Double): String =
        String.format(Locale.forLanguageTag("es-ES"), "%.3f", value)
            .trimEnd('0')
            .trimEnd(',')
}

/** A product used before, offered while typing a new one. [toString] is what the field filters on. */
data class ProductSuggestion(val name: String, val category: Category, val kilograms: Boolean? = null) {
    override fun toString(): String = name
}

/** A list received from someone else, not yet saved. */
data class SharedList(val name: String, val products: List<Product>)

data class ShoppingList(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val products: MutableList<Product> = mutableListOf(),
    /** Ids of the products already in the cart while shopping, kept until the list is completed. */
    val inCart: MutableSet<String> = mutableSetOf(),
) {
    val isMissingList: Boolean get() = id == MISSING_LIST_ID
    val isHomeList: Boolean get() = id == HOME_LIST_ID

    /** The product with this name, ignoring case and surrounding spaces, whatever its category. */
    fun findByName(name: String): Product? =
        products.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    /** Adds the product, summing the quantity if the same product is already in the list. */
    fun addOrMerge(product: Product) {
        val index = products.indexOfFirst {
            it.category == product.category && it.usesKilograms == product.usesKilograms &&
                it.name.equals(product.name.trim(), ignoreCase = true)
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
        /** "Recuento en casa": what the user has at home. Quantities can be 0 ("no me queda"). */
        const val HOME_LIST_ID = "home"

        /** A new list with the products of all [lists]; a product repeated across them appears once, summed. */
        fun merged(name: String, lists: List<ShoppingList>): ShoppingList =
            ShoppingList(name = name).also { merged ->
                lists.flatMap { it.products }.forEach { merged.addOrMerge(it.copy(id = UUID.randomUUID().toString())) }
            }
    }
}
