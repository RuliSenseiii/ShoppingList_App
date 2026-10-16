package com.example.shoppinglist.data

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Turns a list into a short text code ("LDC1:...") that can travel inside any chat message, and back.
 * The code is the list as compact JSON, compressed and Base64 encoded. Standard Base64 is used on
 * purpose: chat apps treat "_" and "-" as formatting, but leave "+", "/" and "=" alone.
 */
object ListShareCodec {

    private const val PREFIX = "LDC1:"
    private val CODE_REGEX = Regex("""LDC1:([A-Za-z0-9+/=]+)""")
    private const val MAX_DECODED_BYTES = 256 * 1024
    private const val MAX_PRODUCTS = 500
    private const val TAG = "ListShareCodec"

    /** A list received from someone else, not yet saved. */
    data class SharedList(val name: String, val products: List<Product>)

    fun encode(list: ShoppingList): String {
        val products = JSONArray()
        list.products.forEach { p ->
            products.put(JSONArray().put(p.name).put(p.category.name).put(p.quantity))
        }
        val json = JSONObject().put("n", list.name).put("p", products).toString()
        return PREFIX + Base64.encodeToString(deflate(json.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    /** True if [text] seems to contain a list code, without decoding it. */
    fun containsCode(text: CharSequence?): Boolean = text != null && CODE_REGEX.containsMatchIn(text)

    /**
     * Finds the code anywhere in [text] (usually the whole shared message) and decodes it.
     * Returns null if there is no code or it is damaged.
     */
    fun decode(text: CharSequence?): SharedList? {
        val code = text?.let { CODE_REGEX.find(it) }?.groupValues?.get(1) ?: return null
        return try {
            val bytes = inflate(Base64.decode(code, Base64.DEFAULT)) ?: return null
            val obj = JSONObject(String(bytes, Charsets.UTF_8))
            val name = obj.getString("n").trim().take(80)
            val productsJson = obj.getJSONArray("p")
            val products = (0 until minOf(productsJson.length(), MAX_PRODUCTS)).mapNotNull { i ->
                val p = productsJson.optJSONArray(i) ?: return@mapNotNull null
                val productName = p.optString(0).trim().take(80)
                // An unknown category (e.g. from a newer version of the app) falls back to "Otros".
                val category = Category.entries.firstOrNull { it.name == p.optString(1) } ?: Category.OTROS
                var quantity = p.optDouble(2, Double.NaN)
                if (!category.usesKilograms) quantity = Math.round(quantity).toDouble()
                if (productName.isEmpty() || quantity.isNaN() || quantity <= 0.0 || quantity > 1_000_000) {
                    null
                } else {
                    Product(name = productName, category = category, quantity = quantity)
                }
            }
            if (name.isEmpty() && products.isEmpty()) null else SharedList(name, products)
        } catch (e: Exception) {
            Log.w(TAG, "Unreadable list code", e)
            null
        }
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        return try {
            deflater.setInput(input)
            deflater.finish()
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
            out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    /** Returns null if the data is not valid or would grow beyond [MAX_DECODED_BYTES]. */
    private fun inflate(input: ByteArray): ByteArray? {
        val inflater = Inflater()
        return try {
            inflater.setInput(input)
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                out.write(buffer, 0, count)
                if (out.size() > MAX_DECODED_BYTES) return null
            }
            out.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
