package com.example.shoppinglist.data

import androidx.annotation.ColorRes
import com.example.shoppinglist.R

enum class Category(
    val emoji: String,
    val label: String,
    @param:ColorRes val colorRes: Int,
    val usesKilograms: Boolean,
) {
    PASTA_ARROZ("🍝", "Pasta/Arroz", R.color.cat_pasta, true),
    CARNE("🥩", "Carne", R.color.cat_carne, true),
    PESCADO("🐟", "Pescado", R.color.cat_pescado, true),
    MARISCO("🦐", "Marisco", R.color.cat_marisco, true),
    LACTEOS("🥛", "Lácteos", R.color.cat_lacteos, false),
    CONSERVAS("🥫", "Conservas", R.color.cat_conservas, false),
    VERDURA("🥬", "Verdura", R.color.cat_verdura, false),
    FRUTA("🍎", "Fruta", R.color.cat_fruta, false),
    OTROS("🛒", "Otros", R.color.cat_otros, false);

    val displayName: String get() = "$emoji $label"
}
