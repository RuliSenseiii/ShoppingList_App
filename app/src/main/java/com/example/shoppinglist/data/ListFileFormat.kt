package com.example.shoppinglist.data

import java.util.Locale

/**
 * The app's own file for sending a list (".listacompra"). Inside it is plain CSV, so it can also be
 * opened with a spreadsheet:
 *
 *     listacompra,1,"Compra del finde"
 *     producto,categoria,cantidad,unidad
 *     Pollo,CARNE,1.5,
 *     "Galletas, sin azúcar",OTROS,2,
 *     Pan,PAN,1.5,kg
 *
 * "unidad" is only filled when the user chose kilos ("kg") or units ("uds") for a product instead of
 * the one of its category; older versions ignore that column.
 *
 * The first line identifies the file and its format version.
 */
object ListFileFormat {

    const val EXTENSION = "listacompra"
    const val MIME_TYPE = "application/vnd.example.listacompra"

    private const val MAGIC = "listacompra"
    private const val VERSION = 1
    private const val HEADER = "producto,categoria,cantidad,unidad"
    private const val OLD_HEADER = "producto,categoria,cantidad"
    private const val MAX_PRODUCTS = 500
    private const val MAX_NAME_LENGTH = 80

    /** Biggest file worth reading; a real list is a few KB. */
    const val MAX_FILE_BYTES = 256 * 1024

    fun write(list: ShoppingList): String = buildString {
        append(MAGIC).append(',').append(VERSION).append(',').append(csvField(list.name)).append("\r\n")
        append(HEADER).append("\r\n")
        list.products.forEach { p ->
            append(csvField(p.name)).append(',')
            append(p.category.name).append(',')
            append(formatQuantity(p.quantity)).append(',')
            append(when (p.kilograms) { true -> "kg"; false -> "uds"; null -> "" }).append("\r\n")
        }
    }

    /** Returns null if [text] is not a list file (or has nothing usable in it). */
    fun read(text: String): SharedList? {
        val rows = parseCsv(text.removePrefix("﻿"))
        val first = rows.firstOrNull() ?: return null
        if (first.getOrNull(0)?.trim() != MAGIC) return null
        val name = first.getOrNull(2).orEmpty().trim().take(MAX_NAME_LENGTH)
        val products = rows.drop(1)
            .filterNot { row -> row.joinToString(",").trim().trimEnd(',').let { it.equals(HEADER, true) || it.equals(OLD_HEADER, true) } }
            .take(MAX_PRODUCTS)
            .mapNotNull { row ->
                sharedProduct(row.getOrNull(0), row.getOrNull(1), row.getOrNull(2)?.trim()?.toDoubleOrNull(), row.getOrNull(3))
            }
        return if (name.isEmpty() && products.isEmpty()) null else SharedList(name, products)
    }

    /**
     * Builds a product received from someone else, or null if it is not usable. Shared with the text
     * code of V0.2/V0.3 so both are checked the same way.
     */
    fun sharedProduct(name: String?, categoryName: String?, quantity: Double?, unit: String? = null): Product? {
        val productName = name.orEmpty().trim().take(MAX_NAME_LENGTH)
        // An unknown category (e.g. from a newer version of the app) falls back to "Otros".
        val category = Category.entries.firstOrNull { it.name == categoryName?.trim() } ?: Category.OTROS
        var amount = quantity ?: return null
        val chosen = when (unit?.trim()?.lowercase()) {
            "kg" -> true
            "uds" -> false
            else -> null
        }
        val product = Product(name = productName, category = category, quantity = amount).let { p ->
            if (chosen == null) p else p.withUnit(chosen)
        }
        if (!product.usesKilograms) amount = Math.round(amount).toDouble()
        if (productName.isEmpty() || amount.isNaN() || amount <= 0.0 || amount > 1_000_000) return null
        return product.copy(quantity = amount)
    }

    /** File name for [list], without characters that are not allowed in file names. */
    fun fileName(list: ShoppingList): String {
        val safe = list.name.replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "").trim().take(MAX_NAME_LENGTH)
        return "${safe.ifEmpty { "lista" }}.$EXTENSION"
    }

    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.ROOT, "%.3f", value).trimEnd('0')

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** Minimal CSV reader: commas, quoted fields with "" escapes and line breaks inside quotes. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        fun endField() {
            row.add(field.toString())
            field.clear()
        }
        fun endRow() {
            endField()
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = mutableListOf()
        }
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endField()
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    endRow()
                }
                else -> field.append(c)
            }
            i++
        }
        endRow()
        return rows
    }
}
