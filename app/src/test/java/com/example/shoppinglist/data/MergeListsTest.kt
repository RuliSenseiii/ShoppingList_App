package com.example.shoppinglist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeListsTest {

    private fun list(name: String, vararg products: Product) =
        ShoppingList(name = name, products = products.toMutableList())

    @Test
    fun merged_showsRepeatedProductsOnceWithQuantitiesSummed() {
        val friday = list(
            "Viernes",
            Product(name = "Leche", category = Category.LACTEOS, quantity = 2.0),
            Product(name = "Pollo", category = Category.CARNE, quantity = 1.5),
        )
        val saturday = list(
            "Sábado",
            Product(name = "leche", category = Category.LACTEOS, quantity = 4.0),
            Product(name = "Manzanas", category = Category.FRUTA, quantity = 6.0),
            Product(name = "Pollo", category = Category.CARNE, quantity = 0.25),
        )

        val merged = ShoppingList.merged("Finde", listOf(friday, saturday))

        assertEquals("Finde", merged.name)
        assertEquals(listOf("Leche", "Pollo", "Manzanas"), merged.products.map { it.name })
        assertEquals(listOf(6.0, 1.75, 6.0), merged.products.map { it.quantity })
        assertTrue(merged.inCart.isEmpty())
    }

    @Test
    fun merged_leavesTheOriginalListsUntouched() {
        val a = list("A", Product(name = "Arroz", category = Category.PASTA_ARROZ, quantity = 1.0))
        val b = list("B", Product(name = "Arroz", category = Category.PASTA_ARROZ, quantity = 2.0))

        val merged = ShoppingList.merged("A + B", listOf(a, b))

        assertEquals(1.0, a.products.single().quantity, 0.0)
        assertEquals(2.0, b.products.single().quantity, 0.0)
        assertTrue(merged.products.none { it.id == a.products.single().id })
    }
}
