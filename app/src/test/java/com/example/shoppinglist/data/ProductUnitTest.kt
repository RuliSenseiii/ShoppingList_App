package com.example.shoppinglist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUnitTest {

    private fun product(category: Category, quantity: Double, kilograms: Boolean? = null, name: String = "Pan") =
        Product(name = name, category = category, quantity = quantity, kilograms = kilograms)

    @Test
    fun withoutAChoiceTheCategoryUnitIsUsed() {
        assertTrue(product(Category.CARNE, 1.0).usesKilograms)
        assertFalse(product(Category.LACTEOS, 1.0).usesKilograms)
    }

    @Test
    fun theUserChoiceWinsOverTheCategory() {
        val bread = product(Category.PAN, 1.5, kilograms = true)
        assertTrue(bread.usesKilograms)
        assertEquals("1,5 kg", bread.formattedQuantity)
        assertEquals(0.25, bread.step, 0.0)

        val chicken = product(Category.CARNE, 2.0, kilograms = false)
        assertFalse(chicken.usesKilograms)
        assertEquals("2 uds", chicken.formattedQuantity)
        assertEquals(1.0, chicken.step, 0.0)
    }

    @Test
    fun choosingTheCategoryUnitClearsTheOverride() {
        assertNull(product(Category.CARNE, 1.0, kilograms = false).withUnit(true).kilograms)
        assertEquals(true, product(Category.PAN, 1.0).withUnit(true).kilograms)
    }

    @Test
    fun sameProductInDifferentUnitsIsNotMerged() {
        val list = ShoppingList(name = "Lista")
        list.addOrMerge(product(Category.PAN, 2.0))
        list.addOrMerge(product(Category.PAN, 1.0, kilograms = true))
        assertEquals(2, list.products.size)
        list.addOrMerge(product(Category.PAN, 3.0))
        assertEquals(2, list.products.size)
        assertEquals(5.0, list.products.first { !it.usesKilograms }.quantity, 0.0)
    }

    @Test
    fun theFileKeepsTheChosenUnit() {
        val list = ShoppingList(
            name = "Casa",
            products = mutableListOf(
                product(Category.PAN, 1.5, kilograms = true, name = "Hogaza"),
                product(Category.CARNE, 3.0, kilograms = false, name = "Salchichas"),
                product(Category.LACTEOS, 2.0, name = "Leche"),
            ),
        )
        val read = ListFileFormat.read(ListFileFormat.write(list))!!
        assertEquals(listOf<Boolean?>(true, false, null), read.products.map { it.kilograms })
        assertEquals(1.5, read.products[0].quantity, 0.0)
    }

    @Test
    fun filesFromOlderVersionsStillRead() {
        val read = ListFileFormat.read("listacompra,1,Lista\r\nproducto,categoria,cantidad\r\nPollo,CARNE,1.5\r\nLeche,LACTEOS,6\r\n")!!
        assertEquals(2, read.products.size)
        assertEquals(listOf<Boolean?>(null, null), read.products.map { it.kilograms })
    }

    @Test
    fun aChosenUnitOfUnitsRoundsTheAmount() {
        val read = ListFileFormat.read("listacompra,1,Lista\r\nproducto,categoria,cantidad,unidad\r\nSalchichas,CARNE,2.6,uds\r\n")!!
        assertEquals(3.0, read.products.single().quantity, 0.0)
        assertFalse(read.products.single().usesKilograms)
    }
}
