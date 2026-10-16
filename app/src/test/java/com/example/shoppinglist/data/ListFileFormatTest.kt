package com.example.shoppinglist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListFileFormatTest {

    private fun list(name: String, vararg products: Product) =
        ShoppingList(name = name, products = products.toMutableList())

    @Test
    fun writeThenRead_keepsNameProductsAndQuantities() {
        val original = list(
            "Compra del sábado",
            Product(name = "Pollo", category = Category.CARNE, quantity = 1.25),
            Product(name = "Leche", category = Category.LACTEOS, quantity = 6.0),
        )

        val read = ListFileFormat.read(ListFileFormat.write(original))!!

        assertEquals("Compra del sábado", read.name)
        assertEquals(listOf("Pollo", "Leche"), read.products.map { it.name })
        assertEquals(listOf(Category.CARNE, Category.LACTEOS), read.products.map { it.category })
        assertEquals(listOf(1.25, 6.0), read.products.map { it.quantity })
    }

    @Test
    fun write_isPlainCsvWithHeader() {
        val text = ListFileFormat.write(
            list("Cena", Product(name = "Macarrones", category = Category.PASTA_ARROZ, quantity = 0.5)),
        )

        assertEquals("listacompra,1,Cena\r\nproducto,categoria,cantidad\r\nMacarrones,PASTA_ARROZ,0.5\r\n", text)
    }

    @Test
    fun namesWithCommasQuotesAndLineBreaks_surviveTheRoundTrip() {
        val tricky = "Galletas, \"sin\" azúcar\nmarca blanca"
        val original = list("Lista, \"especial\"", Product(name = tricky, category = Category.OTROS, quantity = 2.0))

        val read = ListFileFormat.read(ListFileFormat.write(original))!!

        assertEquals("Lista, \"especial\"", read.name)
        assertEquals(tricky, read.products.single().name)
    }

    @Test
    fun read_acceptsByteOrderMarkAndLfLineEnds() {
        val read = ListFileFormat.read("﻿listacompra,1,Lista\nproducto,categoria,cantidad\nHuevos,OTROS,12\n")!!

        assertEquals("Huevos", read.products.single().name)
    }

    @Test
    fun read_unknownCategoryFallsBackToOtros() {
        val read = ListFileFormat.read("listacompra,1,Lista\r\nCosa,CATEGORIA_NUEVA,3\r\n")!!

        assertEquals(Category.OTROS, read.products.single().category)
    }

    @Test
    fun read_skipsUnusableRows() {
        val read = ListFileFormat.read(
            "listacompra,1,Lista\r\n" +
                ",CARNE,1\r\n" + // no name
                "Pollo,CARNE,abc\r\n" + // not a number
                "Pollo,CARNE,-1\r\n" + // not positive
                "Pescado,PESCADO,0.75\r\n",
        )!!

        assertEquals(listOf("Pescado"), read.products.map { it.name })
    }

    @Test
    fun read_roundsUnitsButKeepsKilograms() {
        val read = ListFileFormat.read("listacompra,1,Lista\r\nHuevos,OTROS,2.6\r\nArroz,PASTA_ARROZ,2.6\r\n")!!

        assertEquals(listOf(3.0, 2.6), read.products.map { it.quantity })
    }

    @Test
    fun read_rejectsFilesThatAreNotLists() {
        assertNull(ListFileFormat.read(""))
        assertNull(ListFileFormat.read("nombre,apellido\r\nAna,García\r\n"))
        assertNull(ListFileFormat.read("\u0000\u0001binary"))
    }

    @Test
    fun fileName_removesCharactersNotAllowedInFileNames() {
        assertEquals("Compra 12.listacompra", ListFileFormat.fileName(list("Compra 1/2")))
        assertEquals("lista.listacompra", ListFileFormat.fileName(list("??")))
    }
}
