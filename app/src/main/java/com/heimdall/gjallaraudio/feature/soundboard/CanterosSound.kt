package com.heimdall.gjallaraudio.feature.soundboard

data class CanterosSound(
    val id: String,
    val title: String,
    val character: String,
    val assetPath: String,
    val quote: String = ""
)

object CanterosCatalog {
    val sounds = listOf(
        // LOS CANTEROS / HIMNO Y CLÁSICOS
        CanterosSound("canteros_1", "¡Nosotros! ¡Los Canteros!", "Canteros", "canteros/Otros/nosotros_canteros.mp3", "¿Quién controla el imperio británico? ¿Quién mantiene el sistema métrico? ¡Nosotros! ¡Los Canteros!"),
        CanterosSound("canteros_2", "¿Votos a favor?", "Canteros", "canteros/Otros/votos_a_favor.mp3", "¿Votos a favor? ¡A favor!"),
        CanterosSound("canteros_3", "Golpe de remo", "Canteros", "canteros/Otros/golpe_remo.mp3", "Eso es golpe de remo."),
        CanterosSound("canteros_4", "Mirar mis sandalias", "Canteros", "canteros/Otros/golpe_remo_2.mp3", "Mirar mis sandalias... golpe de remo."),
        CanterosSound("canteros_5", "No conquistas nada con ensalada", "Canteros", "canteros/Otros/no_conquistas_nada_ensalada.mp3", "¡No conquistas nada con una ensalada!"),
        CanterosSound("canteros_6", "Monorrail", "Canteros", "canteros/Otros/monorrail.mp3", "¡Monorrail! ¡Monorrail! ¡Monorrail!"),
        CanterosSound("canteros_7", "¡La caja! ¡La caja!", "Canteros", "canteros/Otros/la_caja.mp3", "Podría ser un barco... o la caja. ¡La caja!"),
        CanterosSound("canteros_8", "Inspector de billeteras", "Canteros", "canteros/Otros/inspector_billeteras.mp3", "Creo que ese no era el inspector de billeteras..."),
        CanterosSound("canteros_9", "Lo hizo un mago", "Canteros", "canteros/Otros/lo_hizo_mago.mp3", "Cada vez que veas algo así, lo hizo un mago."),
        CanterosSound("canteros_10", "¿Nadie va a pensar en los niños?", "Canteros", "canteros/Otros/pero_nadie_pensar_ninos.mp3", "¿Es que nadie va a pensar en los niños?"),
        CanterosSound("canteros_11", "¡Tomaco!", "Canteros", "canteros/Otros/tomaco.mp3", "¡Sabe a cenicero pero quiero más! ¡Tomaco!"),
        CanterosSound("canteros_12", "Na na na ¡Líder!", "Canteros", "canteros/Otros/nanana_lider.mp3", "Na na na na na na na ¡Líder!"),

        // HOMER SIMPSON
        CanterosSound("homer_1", "Compu-Globo-Hiper-Mega-Net", "Homer", "canteros/Homer/compuglobahipermeganet.mp3", "Compu-Globo-Hiper-Mega-Net, vicepresidente ejecutivo junior."),
        CanterosSound("homer_2", "¡A fregar!", "Homer", "canteros/Homer/a_fregar.mp3", "¡A fregar!"),
        CanterosSound("homer_3", "A la grande la llamo mordiscos", "Homer", "canteros/Homer/a_grande_mordiscos.mp3", "A la grande la llamo mordiscos."),
        CanterosSound("homer_4", "A ti no te puedo mentir", "Homer", "canteros/Homer/a_mentir_marge.mp3", "A ti no te puedo mentir, Marge... ¡Hasta luego!"),
        CanterosSound("homer_5", "64 lonchas de queso", "Homer", "canteros/Homer/64_lonchas_queso.mp3", "64 lonchas de queso americano... creo que me he quedado ciego."),
        CanterosSound("homer_6", "¡Callaos, hipoglúcidos!", "Homer", "canteros/Homer/callaos_hipoglucidos.mp3", "¡Callaos, hipoglúcidos!"),
        CanterosSound("homer_7", "Cama arriba, cama abajo", "Homer", "canteros/Homer/cama_arriba.mp3", "Cama arriba, cama abajo, cama arriba..."),
        CanterosSound("homer_8", "Cojo otro muelle", "Homer", "canteros/Homer/cojo_muelle.mp3", "Cojo otro muelle y lo tiro por el retrete... ya son 820 muelles los que el retrete se ha tragado."),
        CanterosSound("homer_9", "¡Mosquis!", "Homer", "canteros/Homer/mosquis.mp3", "¡Mosquis!"),
        CanterosSound("homer_10", "¡A que te mato!", "Homer", "canteros/Homer/a_que_te_mato.mp3", "¡A que te mato! ¡A que te mato!"),
        CanterosSound("homer_11", "¡Catapúm!", "Homer", "canteros/Homer/catapum.mp3", "¡Catapúm chimpún!"),

        // NED FLANDERS
        CanterosSound("flanders_1", "Como si no llevara nada", "Flanders", "canteros/Ned Flanders/con_el_llevara_nada.mp3", "¡Como si no llevara nada! ¡Llevara nada! ¡Llevara nada!"),
        CanterosSound("flanders_2", "¡Hola holita vecinitos!", "Flanders", "canteros/Ned Flanders/hola_holita_vecinitos.mp3", "¡Hola, holita, vecinitos!"),
        CanterosSound("flanders_3", "¡Estamos perdiditos!", "Flanders", "canteros/Ned Flanders/estamos_perdiditos.mp3", "¡Estamos perdiditos! ¡Perdiditos!"),

        // MOE SZYSLAK
        CanterosSound("moe_1", "Ohlalá señor franchute", "Moe", "canteros/Moe/olala_monsieur_franchute.mp3", "¿Ohlalá, señor franchute? ¿Y cómo se dice entonces? Parqueadero."),
        CanterosSound("moe_2", "Detector de mentiras", "Moe", "canteros/Moe/detector_mentiras_moe.mp3", "¿Detector de mentiras? ¡Pero si esto es una birria!"),

        // NELSON MUNTZ
        CanterosSound("nelson_1", "¡Ja, ja!", "Nelson", "canteros/Nelson/ja_ja.mp3", "¡Ja, ja!"),

        // MILHOUSE
        CanterosSound("milhouse_1", "A pedir de Milhouse", "Milhouse", "canteros/Milhouse/todo_pedir_milhouse.mp3", "¡Todo ha salido a pedir de Milhouse!"),
        CanterosSound("milhouse_2", "En forma de chapa", "Milhouse", "canteros/Milhouse/ha_vuelto_chapa.mp3", "¿Alf? ¡Ha vuelto en forma de chapa!"),

        // JEFE WIGGUM
        CanterosSound("wiggum_1", "El señor Soborno", "Wiggum", "canteros/Jefe Wiggum/a_soborno_guino.mp3", "A quien estoy buscando *guiño* es al señor Soborno."),
        CanterosSound("wiggum_2", "Máquina de escribir invisible", "Wiggum", "canteros/Jefe Wiggum/maquina_escribir_invisible.mp3", "Tecleando en mi máquina de escribir invisible..."),

        // RALPH WIGGUM
        CanterosSound("ralph_1", "El aliento de mi gato", "Ralph", "canteros/Ralph Wiggum/el_aliento_gato.mp3", "El aliento de mi gato huele a comida de gato."),
        CanterosSound("ralph_2", "¡Corre, plátano!", "Ralph", "canteros/Ralph Wiggum/corre_platano.mp3", "¡Corre, plátano!"),
        CanterosSound("ralph_3", "Soy un unicornio", "Ralph", "canteros/Ralph Wiggum/soy_unicornio.mp3", "¡Soy un unicornio retrasado!"),
        CanterosSound("ralph_4", "Eres chuli", "Ralph", "canteros/Ralph Wiggum/eres_chuli.mp3", "Eres chuli... y tiene el dibujo de un tren."),
        CanterosSound("ralph_5", "¡La ligas!", "Ralph", "canteros/Ralph Wiggum/la_ligas.mp3", "¡La ligas!"),

        // SEÑOR BURNS
        CanterosSound("burns_1", "Excelente...", "Burns", "canteros/Burns/excelente.mp3", "Excelente..."),
        CanterosSound("burns_2", "Mocasines saltarines", "Burns", "canteros/Burns/mocasines_saltarines.mp3", "Mocasines saltarines con la piel de dos mastines..."),
        CanterosSound("burns_3", "Os traigo amor...", "Burns", "canteros/Burns/os_traigo_amor.mp3", "Os traigo paz... os traigo amor..."),
        CanterosSound("burns_4", "¡Despedido!", "Burns", "canteros/Burns/despedido.mp3", "¡Despedido!"),

        // ABUELO SIMPSON
        CanterosSound("abuelo_1", "Corrían los años diquiti", "Abuelo", "canteros/Abuelo/diquiti.mp3", "Decíamos diquiti porque el Kaiser nos robó la palabra veinte."),
        CanterosSound("abuelo_2", "Mi Homer no es comunista", "Abuelo", "canteros/Abuelo/mi_homer_comunista.mp3", "Mi Homer no es comunista. Podrá ser mentiroso, puerco, idiota, comunista... pero nunca una estrella de porno."),
        CanterosSound("abuelo_3", "La cebolla en el cinturón", "Abuelo", "canteros/Abuelo/la_cebolla_cinturon.mp3", "Llevaba una cebolla en el cinturón, que era lo que se estilaba entonces."),
        CanterosSound("abuelo_4", "Más feo que el culo de una mona", "Abuelo", "canteros/Abuelo/eres_feo_culo.mp3", "Eres más feo que el culo de una mona."),

        // DIRECTOR SKINNER
        CanterosSound("skinner_1", "¿Aurora boreal?", "Skinner", "canteros/Director Skinner/aurora_boreal.mp3", "¿Aurora boreal? ¿En esta época del año, a esta hora del día, en esta parte del mundo y ubicada enteramente en su cocina?"),
        CanterosSound("skinner_2", "Se vende niño", "Skinner", "canteros/Director Skinner/se_vende_nino.mp3", "Se vende niño."),

        // BART SIMPSON
        CanterosSound("bart_1", "¡Ay, caramba!", "Bart", "canteros/Bart/ay_caramba.mp3", "¡Ay, caramba!"),
        CanterosSound("bart_2", "¡Multiplícate por cero!", "Bart", "canteros/Bart/multiplicate_cero.mp3", "¡Multiplícate por cero!"),
        CanterosSound("bart_3", "¡Yo no he sido!", "Bart", "canteros/Bart/yo_no_sido.mp3", "¡Yo no he sido!"),
        CanterosSound("bart_4", "Nadie sospecha de la mariposa", "Bart", "canteros/Bart/nadie_sospecha_mariposa.mp3", "Nadie sospecha jamás de la mariposa..."),

        // PERSONAJES CULTOS
        CanterosSound("willie_1", "Malditos escoceses", "Willie", "canteros/Willie/malditos_escoceses.mp3", "¡Malditos escoceses, han destrozado Escocia!"),
        CanterosSound("rainier_1", "¡Mis ojos!", "Otros", "canteros/Rainier Wolfcastle/mis_ojos.mp3", "¡Mis ojos! ¡Las gafas no hacen nada!"),
        CanterosSound("rainier_2", "¡Vámonos, átomos!", "Otros", "canteros/Rainier Wolfcastle/vamonos_atomos.mp3", "¡Vámonos, átomos!"),
        CanterosSound("apu_1", "Adiós al Badulaque", "Apu", "canteros/Apu/adios_badulaque.mp3", "¿Quién necesita el Badulaque? Yo no..."),
        CanterosSound("nick_1", "¡Hola Doctor Nick!", "Otros", "canteros/Doctor Nick/hola_dr_nick.mp3", "¡Hola a todo el mundo! ¡Hola Doctor Nick!"),
        CanterosSound("smithers_1", "El sombrero es nuevo", "Smithers", "canteros/Smithers/pero_sombrero_nuevo.mp3", "¡Pero el sombrero es nuevo!"),
        CanterosSound("burns_5", "Excelente Zutroy", "Burns", "canteros/Burns/excelente_zutroy.mp3", "Excelente, Zutroy..."),
        CanterosSound("homer_12", "Con erótico resultado", "Homer", "canteros/Homer/con_erotico.mp3", "Con erótico resultado..."),
        CanterosSound("homer_13", "Dinosaurio inamovible", "Homer", "canteros/Homer/dinosaurio_inamobible.mp3", "Es como un dinosaurio inamovible..."),
        CanterosSound("ralph_6", "El duende me dijo", "Ralph", "canteros/Ralph Wiggum/me_dijo_quemara.mp3", "El duende me dijo que quemara cosas."),
        CanterosSound("otros_13", "Farfalla vendetta", "Otros", "canteros/Otros/farfalla_vendetta.mp3", "Farfalla vendetta..."),
        CanterosSound("otros_14", "Si una vaca pudiera", "Otros", "canteros/Otros/si_vaca_oportunidad.mp3", "Si una vaca tuviera la oportunidad te comería a ti y a todos los que quieres."),
        CanterosSound("otros_15", "Lágrimas deliciosas", "Otros", "canteros/Otros/sus_lagrimas_real.mp3", "¡Mmm, sus lágrimas son deliciosas!")
    )

    val categories = listOf("Todos", "Canteros", "Homer", "Flanders", "Moe", "Nelson", "Milhouse", "Ralph", "Burns", "Abuelo", "Bart", "Skinner", "Willie", "Smithers", "Otros")

    val soundsById: Map<String, CanterosSound> by lazy { sounds.associateBy { it.id } }
}
