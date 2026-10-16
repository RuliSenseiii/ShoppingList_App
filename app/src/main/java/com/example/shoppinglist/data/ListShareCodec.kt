package com.example.shoppinglist.data

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Reads the text code that V0.2 and V0.3 put at the end of a shared message ("LDC1:..."), so lists
 * sent by those versions can still be imported. Since V0.4 lists are sent as a [ListFileFormat] file.
 * The code is the list as compact JSON, compressed and Base64 encoded.
 */
object ListShareCodec {

    private val CODE_REGEX = Regex("""LDC1:([A-Za-z0-9+/=]+)""")
    private const val MAX_DECODED_BYTES = 256 * 1024
    private const val MAX_PRODUCTS = 500
    private const val TAG = "ListShareCodec"

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
                ListFileFormat.sharedProduct(p.optString(0), p.optString(1), p.optDouble(2, Double.NaN))
            }
            if (name.isEmpty() && products.isEmpty()) null else SharedList(name, products)
        } catch (e: Exception) {
            Log.w(TAG, "Unreadable list code", e)
            null
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
