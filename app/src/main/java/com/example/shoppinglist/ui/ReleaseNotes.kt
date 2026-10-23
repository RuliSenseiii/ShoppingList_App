package com.example.shoppinglist.ui

/** What a version brought: the new features and what it changed in things that already existed. */
data class ReleaseNote(
    val version: String,
    val news: List<String>,
    val changes: List<String> = emptyList(),
)

/**
 * The "Notas de la app" screen. Newest version first.
 * Add an entry at the top for every new version or feature, and keep it in step with GUIA.md.
 */
object ReleaseNotes {

    val all = listOf(
        ReleaseNote(
            "V0.8",
            news = listOf(
                "Nuevas categorías: Bollería, Precocinados, Higiene, Limpieza y Pan (en unidades), y Legumbres y Hortalizas (en kilos).",
                "Cada producto puede contarse en kilos o en unidades, como prefieras. Por defecto usa la unidad de su categoría.",
                "Buscador en Mis listas, dentro de cada lista, en el recuento en casa y al hacer la compra.",
                "Ordenar los productos: por categorías, de la A a la Z o por la ruta del súper, con las categorías en el orden que elijas.",
                "Botones − y + para cambiar las cantidades en el recuento en casa y al hacer la compra, o escribirlas con el teclado.",
                "Aviso de poco stock: indica un mínimo para un producto del recuento y se apunta solo en \"Productos que me faltan\".",
                "Ajustes (⚙): copia de seguridad y restauración de tus datos, orden de las categorías y estas notas.",
                "Cuatro temas nuevos en el tutorial: buscar y ordenar, cambiar cantidades rápido, aviso de poco stock y copia de seguridad.",
            ),
            changes = listOf(
                "Al añadir un producto, la unidad (kg / uds) se puede cambiar tocándola; al editar la cantidad también.",
                "Los archivos .listacompra incluyen la unidad elegida de cada producto; las versiones anteriores siguen pudiendo abrirlos.",
                "Un producto del recuento con aviso de poco stock se queda en 0 en lugar de desaparecer, para no perder el aviso.",
            ),
        ),
        ReleaseNote(
            "V0.7.1",
            news = listOf(
                "El tutorial te guía por la propia app: ilumina el botón que debes pulsar y avanza cuando haces lo que indica.",
                "Puedes salir del tutorial en cualquier momento.",
            ),
            changes = listOf(
                "El tutorial ya no son pantallas de explicación: eliges un tema y lo haces en la app real.",
            ),
        ),
        ReleaseNote(
            "V0.7",
            news = listOf(
                "Tutorial dentro de la app, siempre disponible con el botón ? de la pantalla principal, dividido en temas.",
                "Guía de uso para usuarios en GitHub, que se actualiza con cada versión.",
            ),
        ),
        ReleaseNote(
            "V0.6.1",
            news = emptyList(),
            changes = listOf(
                "Cuando la cantidad de un producto del recuento en casa llega a 0, el producto desaparece.",
            ),
        ),
        ReleaseNote(
            "V0.6",
            news = listOf(
                "Selección de listas: mantén pulsada una para que aparezcan las casillas.",
                "Terminar la compra en cualquier momento, sin necesidad de meter toda la lista en el carro.",
                "Al terminar con productos sin comprar puedes revisar, guardar o eliminar la lista.",
            ),
            changes = listOf(
                "Unir listas pide confirmación y muestra qué listas se van a unir.",
                "Lo comprado se suma al recuento en casa y desaparece de la lista; lo que no compraste se queda.",
                "Terminar la compra pregunta con Sí o No en lugar de una cruz.",
            ),
        ),
        ReleaseNote(
            "V0.5",
            news = listOf(
                "Recuento en casa: lo que tienes en casa, que se actualiza con tus compras.",
                "Unir varias listas en una.",
            ),
        ),
        ReleaseNote(
            "V0.4",
            news = listOf(
                "Enviar las listas como un archivo .listacompra que el otro móvil abre directamente con la app.",
            ),
        ),
        ReleaseNote(
            "V0.3",
            news = listOf(
                "El progreso del carro se guarda aunque salgas de la pantalla de la compra.",
                "Entrada de productos más rápida, con sugerencias de los que ya has usado.",
                "Renombrar y eliminar listas, con \"Deshacer\".",
            ),
            changes = listOf(
                "Antes de eliminar una lista se pide confirmación.",
            ),
        ),
        ReleaseNote(
            "V0.2",
            news = listOf(
                "Compartir una lista por mensaje, con un código para importarla en la app.",
            ),
        ),
        ReleaseNote(
            "V0.1",
            news = listOf(
                "Crear listas y añadir productos con su categoría y cantidad (en kilos o unidades).",
                "Ver las listas separadas por categorías y editar cantidades.",
                "\"Voy a comprar\": tachar los productos al meterlos en el carro.",
                "\"Productos que me faltan\": apuntar lo que se acaba.",
            ),
        ),
    )
}
