package com.example.shoppinglist.ui

/** What the user needs saved in the app before a tutorial topic makes sense. */
enum class TutorialRequirement(val message: String) {
    NONE(""),
    ONE_LIST("Para este tema necesitas al menos una lista con productos. Haz primero el tema \"Crear una lista\"."),
    TWO_LISTS("Para este tema necesitas al menos dos listas. Haz primero el tema \"Crear una lista\" y crea otra."),
    SHOP_LIST("Para este tema necesitas una lista con al menos dos productos. Añade productos a una lista y vuelve."),
}

/**
 * One thing the user has to do.
 *  - [screen]: id of the screen where it happens (see the `screenId` of each activity).
 *  - [target]: tag ("tut:<target>") or view id name of the button to highlight.
 *  - [done]: event that completes the step, "screen:<id>" when it is reaching a screen.
 *  - [inDialog]: the target is inside a pop-up, not in the screen itself.
 */
data class TutorialStep(
    val screen: String,
    val target: String,
    val text: String,
    val done: String,
    val inDialog: Boolean = false,
)

data class TutorialTopic(
    val emoji: String,
    val title: String,
    val summary: String,
    val green: Boolean,
    val requires: TutorialRequirement,
    val steps: List<TutorialStep>,
    /** Shown when the last step is done. */
    val outro: String,
)

/**
 * Content of the tutorial. The app is driven by [TutorialGuide]: each step highlights a button and
 * moves on only when the user does it. Keep it in step with GUIA.md when a version changes the app.
 */
object Tutorial {

    private const val MAIN = "main"
    private const val MANAGE = "lists_manage"
    private const val SHOP = "lists_shop"
    private const val DETAIL = "detail"
    private const val SHOPPING = "shopping"

    /** The three steps shared by every "add a product" form: name, category and quantity. */
    private fun productForm(screen: String, nameText: String, quantityText: String) = listOf(
        TutorialStep(screen, "nameRow", nameText, "name_typed"),
        TutorialStep(screen, "categoryGrid", "Toca la categoría del producto. Define si se cuenta en kilos o en unidades.", "category_selected"),
        TutorialStep(screen, "quantityRow", quantityText, "quantity_ready"),
        TutorialStep(screen, "btnAdd", "Pulsa \"Añadir\" para guardar el producto.", "product_added"),
    )

    val topics = listOf(
        TutorialTopic(
            "📝", "Crear una lista", "Tu primera lista de la compra", green = true, TutorialRequirement.NONE,
            steps = listOf(
                TutorialStep(MAIN, "bannerNewList", "Pulsa \"Crear nueva lista\" para empezar.", "screen:add_new"),
                TutorialStep("add_new", "dialog_positive", "Ponle un nombre a tu lista y pulsa \"Crear\".", "list_named", inDialog = true),
            ) + productForm("add_new", "Escribe el nombre de un producto, por ejemplo \"Leche\".", "Escribe la cantidad.") + listOf(
                TutorialStep("add_new", "btnBack", "¡Producto añadido! Puedes añadir todos los que quieras. Pulsa ← para volver.", "screen:main"),
            ),
            outro = "Has creado tu primera lista. La encontrarás en \"Ver mis listas de la compra\" y puedes borrarla desde allí si era de prueba.",
        ),
        TutorialTopic(
            "✏️", "Mis listas", "Abrir, editar y renombrar", green = false, TutorialRequirement.ONE_LIST,
            steps = listOf(
                TutorialStep(MAIN, "bannerMyLists", "Pulsa \"Ver mis listas de la compra\".", "screen:$MANAGE"),
                TutorialStep(MANAGE, "list_card_full", "Toca una lista para abrirla.", "screen:$DETAIL"),
                TutorialStep(DETAIL, "product_row", "Toca un producto para ver sus opciones.", "product_popup"),
                TutorialStep(DETAIL, "popup_close", "Desde aquí puedes eliminar el producto o editar su cantidad. Ciérralo con la ✕.", "popup_closed", inDialog = true),
                TutorialStep(DETAIL, "btnBack", "Pulsa ← para volver a tus listas.", "screen:$MANAGE"),
                TutorialStep(MANAGE, "list_rename_full", "El lápiz cambia el nombre de la lista (la papelera de al lado la elimina). Pulsa el lápiz.", "rename_dialog"),
                TutorialStep(MANAGE, "dialog_positive", "Cambia el nombre y pulsa \"Guardar\".", "list_renamed", inDialog = true),
            ),
            outro = "Ya sabes abrir, editar y renombrar tus listas. Para eliminar una, usa la papelera: te pedirá confirmación y podrás deshacerlo.",
        ),
        TutorialTopic(
            "🔗", "Unir listas", "Junta varias listas en una", green = true, TutorialRequirement.TWO_LISTS,
            steps = listOf(
                TutorialStep(MAIN, "bannerMyLists", "Pulsa \"Ver mis listas de la compra\".", "screen:$MANAGE"),
                TutorialStep(MANAGE, "list_card_1", "Mantén pulsada esta lista unos segundos, hasta que aparezca una casilla.", "selection_started"),
                TutorialStep(MANAGE, "list_card_2", "Toca otra lista para marcarla también.", "selected_two"),
                TutorialStep(MANAGE, "btnBottom", "Con 2 o más listas marcadas aparece \"Unir listas\". Púlsalo.", "merge_confirm"),
                TutorialStep(MANAGE, "popup_merge", "Aquí ves qué listas vas a unir. Pulsa \"Unir\" para continuar.", "merge_name", inDialog = true),
                TutorialStep(MANAGE, "dialog_positive", "Ponle nombre a la lista unida y pulsa \"Unir\".", "merged", inDialog = true),
            ),
            outro = "Las listas se han unido de verdad. Si no querías hacerlo, pulsa \"Deshacer\" en el aviso de abajo antes de que desaparezca.",
        ),
        TutorialTopic(
            "➕", "Productos que me faltan", "Apunta lo que se acaba", green = false, TutorialRequirement.NONE,
            steps = listOf(
                TutorialStep(MAIN, "bannerAddProduct", "Pulsa \"Añadir producto\".", "screen:add_missing"),
            ) + productForm("add_missing", "Escribe lo que se te ha acabado, por ejemplo \"Aceite\".", "Escribe la cantidad que necesitas.") + listOf(
                TutorialStep("add_missing", "btnBack", "Pulsa ← para volver a la pantalla principal.", "screen:main"),
            ),
            outro = "Se ha guardado en \"Productos que me faltan\". La próxima vez que vayas a comprar, la app te preguntará si quieres añadirlo a la lista.",
        ),
        TutorialTopic(
            "🛒", "Voy a comprar", "Tacha productos mientras compras", green = true, TutorialRequirement.SHOP_LIST,
            steps = listOf(
                TutorialStep(MAIN, "btnGoShopping", "Pulsa el botón grande \"VOY A COMPRAR\".", "screen:$SHOP"),
                TutorialStep(SHOP, "list_card_full", "Elige la lista que vas a comprar.", "screen:$SHOPPING"),
                TutorialStep(SHOPPING, "product_row", "Toca un producto cuando lo tengas en la mano.", "cart_popup"),
                TutorialStep(SHOPPING, "popup_in_cart", "Pulsa \"En el carro\" para tacharlo. Si no lo encuentras, pulsa \"Falta\".", "in_cart", inDialog = true),
                TutorialStep(SHOPPING, "product_crossed", "¡Tachado! Si te equivocas, toca el producto tachado para deshacerlo.", "uncart"),
                TutorialStep(SHOPPING, "btnBack", "Puedes salir cuando quieras: lo que marques se guarda. Pulsa ←.", "screen:main"),
            ),
            outro = "Cuando termines de comprar, pulsa \"Terminar compra\" (aparece en cuanto hay algo en el carro): lo comprado se suma al recuento en casa y lo que falta se queda en la lista.",
        ),
        TutorialTopic(
            "🏠", "Recuento en casa", "Qué tienes en casa", green = false, TutorialRequirement.NONE,
            steps = listOf(
                TutorialStep(MAIN, "bannerHome", "Pulsa \"Recuento en casa\".", "screen:$DETAIL"),
                TutorialStep(DETAIL, "btnBottom", "Aquí verás lo que tienes en casa. Pulsa \"Añadir producto\" para apuntar algo.", "screen:add_home"),
            ) + productForm("add_home", "Escribe un producto que tengas en casa.", "Escribe cuántos tienes.") + listOf(
                TutorialStep("add_home", "btnBack", "Pulsa ← para volver al recuento.", "screen:$DETAIL"),
                TutorialStep(DETAIL, "btnBack", "Pulsa ← otra vez para volver a la pantalla principal.", "screen:main"),
            ),
            outro = "Tu recuento se actualiza solo: al terminar una compra se suma lo comprado, y cuando un producto llega a 0 desaparece.",
        ),
        TutorialTopic(
            "📤", "Compartir e importar", "Envía listas a otras personas", green = true, TutorialRequirement.ONE_LIST,
            steps = listOf(
                TutorialStep(MAIN, "bannerMyLists", "Pulsa \"Ver mis listas de la compra\".", "screen:$MANAGE"),
                TutorialStep(MANAGE, "list_card_full", "Abre la lista que quieras enviar.", "screen:$DETAIL"),
                TutorialStep(DETAIL, "btnTopAction", "Pulsa el botón de compartir.", "share_opened"),
                TutorialStep(DETAIL, "btnBack", "La lista se envía como un archivo .listacompra por WhatsApp, correo, etc. Cierra el menú de compartir y pulsa ←.", "screen:$MANAGE"),
                TutorialStep(MANAGE, "btnBottom", "Para recibir una lista, pulsa \"Importar lista\" y elige el archivo.", "import_opened"),
            ),
            outro = "Quien reciba el archivo también puede tocarlo directamente para abrirlo con la app y añadir la lista.",
        ),
    )
}
