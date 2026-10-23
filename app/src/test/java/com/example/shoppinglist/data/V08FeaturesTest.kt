package com.example.shoppinglist.data

import com.example.shoppinglist.ui.matchesSearch
import com.example.shoppinglist.ui.searchKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V08FeaturesTest {

    @Test
    fun searchIgnoresCaseAndAccents() {
        assertEquals("bolleria", "Bollería".searchKey())
        assertTrue("Bollería".matchesSearch("BOLLERIA"))
        assertTrue("Leche entera".matchesSearch("  leche "))
        assertFalse("Leche".matchesSearch("pan"))
    }

    @Test
    fun emptySearchMatchesEverything() {
        assertTrue("Leche".matchesSearch(""))
        assertTrue("Leche".matchesSearch("   "))
    }

    @Test
    fun categoryOrderKeepsTheSavedOnesFirstAndAppendsTheRest() {
        val order = ViewSettings.parseOrder("FRUTA,CARNE")
        assertEquals(listOf(Category.FRUTA, Category.CARNE), order.take(2))
        assertEquals(Category.entries.size, order.size)
        assertEquals(Category.entries.toSet(), order.toSet())
    }

    @Test
    fun categoryOrderIgnoresUnknownAndRepeatedNames() {
        val order = ViewSettings.parseOrder("NOEXISTE,PAN,PAN,,CARNE")
        assertEquals(listOf(Category.PAN, Category.CARNE), order.take(2))
        assertEquals(Category.entries.size, order.size)
    }

    @Test
    fun noSavedOrderMeansTheDefaultOne() {
        assertEquals(Category.entries.toList(), ViewSettings.parseOrder(null))
    }

    @Test
    fun quantityStepIsAQuarterKiloOrOneUnit() {
        assertEquals(0.25, Product(name = "Pollo", category = Category.CARNE, quantity = 1.0).step, 0.0)
        assertEquals(1.0, Product(name = "Leche", category = Category.LACTEOS, quantity = 1.0).step, 0.0)
    }

    @Test
    fun minimumIsWrittenLikeTheQuantity() {
        val milk = Product(name = "Leche", category = Category.LACTEOS, quantity = 5.0, minQuantity = 2.0)
        assertEquals("2 uds", milk.formattedMin)
        val rice = Product(name = "Arroz", category = Category.PASTA_ARROZ, quantity = 2.0, minQuantity = 0.5)
        assertEquals("0,5 kg", rice.formattedMin)
    }

    @Test
    fun newCategoriesCountInTheRightUnit() {
        assertTrue(Category.LEGUMBRES.usesKilograms)
        assertTrue(Category.HORTALIZAS.usesKilograms)
        assertFalse(Category.PAN.usesKilograms)
        assertFalse(Category.HIGIENE.usesKilograms)
    }
}
