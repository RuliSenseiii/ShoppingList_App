package com.example.shoppinglist.ui

/** A screen of the app a tutorial step can open so the user can try what was just explained. */
enum class TutorialTarget(val label: String) {
    NEW_LIST("Crear una lista ahora"),
    MY_LISTS("Ir a Mis listas"),
    MISSING("Ir a Añadir producto"),
    HOME("Ir al Recuento en casa"),
    SHOP("Ir a Voy a comprar"),
}

data class TutorialStep(
    val emoji: String,
    val title: String,
    val text: String,
    val tryIt: TutorialTarget? = null,
)

data class TutorialTopic(
    val emoji: String,
    val title: String,
    val summary: String,
    val green: Boolean,
    val steps: List<TutorialStep>,
)

/** Content of the tutorial. Keep it in step with GUIA.md when a version changes how the app works. */
object Tutorial {

    val topics = listOf(
        TutorialTopic(
            "📝", "Crear una lista", "Tu primera lista de la compra", green = true,
            steps = listOf(
                TutorialStep(
                    "👋", "Empieza desde la pantalla principal",
                    "Pulsa el banner naranja \"Crear nueva lista\". Es el punto de partida para cualquier lista nueva.",
                ),
                TutorialStep(
                    "🔎", "Escribe el producto",
                    "Escribe el nombre. Si ya lo has usado antes, aparecerá en las sugerencias y bastará con tocarlo.",
                ),
                TutorialStep(
                    "🥕", "Elige la categoría",
                    "Toca una categoría (carne, fruta, lácteos…). Ella decide si se cuenta en kilos o en unidades.",
                ),
                TutorialStep(
                    "🔢", "Pon la cantidad y añade",
                    "Escribe la cantidad y pulsa \"Añadir\". Repite con cada producto. La lista se guarda al ponerle nombre.",
                    TutorialTarget.NEW_LIST,
                ),
            ),
        ),
        TutorialTopic(
            "✏️", "Mis listas", "Ver, editar, renombrar y borrar", green = false,
            steps = listOf(
                TutorialStep(
                    "📋", "Tus listas guardadas",
                    "En \"Ver mis listas de la compra\" aparecen todas tus listas, y también \"Productos que me faltan\".",
                    TutorialTarget.MY_LISTS,
                ),
                TutorialStep(
                    "👆", "Abrir y editar una lista",
                    "Toca una lista para verla por categorías. Toca un producto para cambiar su cantidad o eliminarlo. Si te equivocas, \"Deshacer\" lo recupera.",
                ),
                TutorialStep(
                    "🖊️", "Renombrar y eliminar",
                    "Cada lista tiene un lápiz para cambiarle el nombre y una papelera para eliminarla. Antes de borrar te pedirá confirmación.",
                ),
            ),
        ),
        TutorialTopic(
            "🔗", "Unir listas", "Junta varias listas en una", green = true,
            steps = listOf(
                TutorialStep(
                    "👇", "Mantén pulsada una lista",
                    "En \"Mis listas\", mantén pulsada una lista unos segundos. Aparecerán casillas de selección.",
                    TutorialTarget.MY_LISTS,
                ),
                TutorialStep(
                    "☑️", "Marca las que quieras",
                    "Toca otras listas para marcarlas o desmarcarlas. Si desmarcas todas, sales de la selección. La flecha de volver también la cancela.",
                ),
                TutorialStep(
                    "✅", "Pulsa \"Unir listas\"",
                    "Con 2 o más marcadas, abajo aparece \"Unir listas\". Te mostrará cuáles vas a unir; confirma y ponle nombre a la nueva lista. \"Deshacer\" te devuelve las originales.",
                ),
            ),
        ),
        TutorialTopic(
            "➕", "Productos que me faltan", "Apunta lo que se acaba", green = false,
            steps = listOf(
                TutorialStep(
                    "💡", "Apunta sobre la marcha",
                    "Si se te acaba algo en casa, pulsa \"Añadir producto\" en la pantalla principal. Se guarda en la lista \"Productos que me faltan\".",
                    TutorialTarget.MISSING,
                ),
                TutorialStep(
                    "🛒", "Se suma a tu próxima compra",
                    "Cuando vayas a comprar, la app te preguntará si quieres ver esos productos. Si dices que sí, se añaden a la lista que compras.",
                ),
            ),
        ),
        TutorialTopic(
            "🛒", "Voy a comprar", "Tacha productos mientras compras", green = true,
            steps = listOf(
                TutorialStep(
                    "🟢", "Elige la lista",
                    "Pulsa el botón grande \"VOY A COMPRAR\" y elige la lista que vas a comprar.",
                    TutorialTarget.SHOP,
                ),
                TutorialStep(
                    "🧺", "Mete cada producto en el carro",
                    "Toca un producto y pulsa \"En el carro\" para tacharlo, o \"Falta\" si no lo encuentras. Toca uno tachado para deshacerlo. Lo marcado se guarda aunque salgas.",
                ),
                TutorialStep(
                    "🏁", "Termina cuando quieras",
                    "No hace falta tacharlo todo. Pulsa \"Terminar compra\" en cualquier momento y confirma con Sí o No.",
                ),
                TutorialStep(
                    "💾", "Qué pasa con lo que sobra",
                    "Lo comprado se suma a tu recuento en casa y desaparece de la lista; lo que no compraste se queda. Puedes revisar, guardar o eliminar la lista.",
                ),
            ),
        ),
        TutorialTopic(
            "🏠", "Recuento en casa", "Qué tienes en casa", green = false,
            steps = listOf(
                TutorialStep(
                    "📦", "Cuenta lo que tienes",
                    "En \"Recuento en casa\" apuntas lo que ya tienes. Al añadir un producto en otra lista, la app te avisa de cuánto tienes en casa.",
                    TutorialTarget.HOME,
                ),
                TutorialStep(
                    "➕", "Se rellena solo",
                    "Cuando terminas una compra, lo comprado se suma automáticamente al recuento.",
                ),
                TutorialStep(
                    "0️⃣", "Lo que se acaba, desaparece",
                    "Si la cantidad de un producto llega a 0, deja de aparecer en el recuento. También puedes tocar un producto para editar su cantidad.",
                ),
            ),
        ),
        TutorialTopic(
            "📤", "Compartir e importar", "Envía listas a otras personas", green = true,
            steps = listOf(
                TutorialStep(
                    "📨", "Enviar una lista",
                    "Abre una lista desde \"Mis listas\" y pulsa el botón de compartir, arriba a la derecha. Se envía como un archivo .listacompra por WhatsApp, correo, etc.",
                    TutorialTarget.MY_LISTS,
                ),
                TutorialStep(
                    "📥", "Recibir una lista",
                    "Quien la recibe toca el archivo y se abre con la app, o usa \"Importar lista\" en \"Mis listas\" para elegir el archivo. Después la confirma y se añade a sus listas.",
                ),
            ),
        ),
    )
}
