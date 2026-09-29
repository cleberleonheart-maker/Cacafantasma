package com.caca.fantasma.game

data class Equip(
    val id: String,
    val name: String,
    val desc: String,
    val price: Int,
    val upgradePrice: Int,
    val unlockAfter: Int
)

data class Scenario(
    val index: Int,
    val name: String,
    val local: String,
    val ghost: String,
    val diff: Int,
    val baseReward: Int,
    val clueTip: Int,
    val intro: String,
    val rounds: List<Round>,
    val victory: String,
    val defeat: String,
    val isBoss: Boolean = false
)

data class Round(
    val spot: String,
    val ideal: String,
    val hit: String
)

data class Consumable(
    val id: String,
    val name: String,
    val desc: String,
    val price: Int,
    val effect: String
)

data class Achievement(
    val id: String,
    val name: String,
    val desc: String,
    val reward: Int
)

data class Rank(
    val name: String,
    val min: Int,
    val rewardBonus: Float,
    val shopDiscount: Int
)

object GameData {

    val EQUIP: List<Equip> = listOf(
        Equip("uv", "Lanterna UV", "Revela marcas e pegadas de ectoplasma.", 0, 60, -1),
        Equip("emf", "Medidor EMF", "Mede o campo de energia do local.", 150, 90, -1),
        Equip("evp", "Gravador EVP", "Capta vozes que o ouvido não percebe.", 200, 100, -1),
        Equip("camera", "Câmera Térmica", "Fotografa vultos e figuras frias.", 250, 120, -1),
        Equip("salt", "Sal Protegido", "Forma barreiras que enfraquecem o fantasma.", 400, 140, 0),
        Equip("cross", "Crucifixo", "Protege o caçador de retaliações.", 550, 160, 1),
        Equip("trap", "Armadilha de Fumaça", "Aprisiona o fantasma no confronto.", 900, 250, 2)
    )

    val CONSUMABLES: List<Consumable> = listOf(
        Consumable("battery", "Pilha de Ecto-Lítio", "A próxima vistoria vira pista automática.", 180, "intuition"),
        Consumable("amulet", "Amuleto Protetor", "Protege os achados se o confronto falhar.", 260, "shield"),
        Consumable("incense", "Incenso de Aura", "Conta como +1 evidência no confronto.", 300, "aura")
    )

    val RANKS: List<Rank> = listOf(
        Rank("Aprendiz", 0, 0.0f, 0),
        Rank("Caçador Iniciante", 1, 0.05f, 0),
        Rank("Caçador", 3, 0.10f, 5),
        Rank("Exterminador", 6, 0.15f, 10),
        Rank("Lenda Urbana", 10, 0.20f, 10),
        Rank("Mestre das Sombras", 15, 0.30f, 15)
    )

    val ACHIEVEMENTS: List<Achievement> = listOf(
        Achievement("first", "Primeiro Espírito", "Capture seu primeiro fantasma.", 150),
        Achievement("five", "Frio na Espinha", "Capture 5 fantasmas.", 300),
        Achievement("master", "Mestre das Sombras", "Alcance o posto máximo.", 800),
        Achievement("perfect", "Caçada 100%", "Capture um fantasma com evidência máxima.", 400),
        Achievement("rich", "Endinheirado", "Junte R$3.000 em dinheiro.", 200),
        Achievement("collector", "Colecionador", "Capture todos os cenários, incluindo o chefe.", 1500),
        Achievement("boss", "Caçador de Chefes", "Capture o chefe final.", 2000),
        Achievement("spender", "Grande Investidor", "Gaste R$2.000 na loja.", 250)
    )

    val SCENARIOS: List<Scenario> = listOf(
        Scenario(
            0, "Casa Abandonada", "Sabará, rua dos pinheiros", "Sussurro",
            1, 300, 25,
            "Uma casa vazia há 20 anos. Dizem que o antigo morador ainda sussurra na escada.",
            listOf(
                Round("O salão empoeirado tem riscos no chão", "uv", "A lanterna revela marcas de unhas de ectoplasma"),
                Round("A energia oscila no corredor escuro", "emf", "O medidor acusou presença no corredor"),
                Round("Vozes baixas vêm do vão da escada", "evp", "O gravador captou um sussurro claro"),
                Round("Um vulto passou no espelho do quarto", "camera", "A foto pegou uma figura fria no espelho")
            ),
            "O Sussurro foi sugado para a armadilha. A casa ficou em silêncio.",
            "O Sussurro escapou na escuridão da cozinha. Você volta de mãos abanando."
        ),
        Scenario(
            1, "Hospital Assombrado", "ala norte, pavilhão 3", "Enfermeira Sombria",
            2, 600, 40,
            "O pavilhão fechado ainda cheira a remédio. Pacientes relatam a enfermeira que muda de forma.",
            listOf(
                Round("Marcas de mãos pequenas na cama enferrujada", "uv", "Marcas de mãos aparecem sob a luz UV"),
                Round("O campo espiralado sobre a maca", "emf", "O EMF gira como uma seta enlouquecida"),
                Round("Risos fracos na enfermaria vazia", "evp", "O gravador comoveu os risos da enfermaria"),
                Round("Um rosto pálido na janela do terceiro andar", "camera", "A foto congelou o rosto na janela")
            ),
            "A Enfermeira Sombria virou fumaça dentro da armadilha. A ala norte sossegou.",
            "A Enfermeira Sombria evaporou na maca. Ela levou metade do seu equipamento."
        ),
        Scenario(
            2, "Prisão Esquecida", "bloco B, celas solitárias", "O Guarda",
            3, 900, 55,
            "A prisão desativada guarda a alma do guarda que nunca teve plantão de folga.",
            listOf(
                Round("Pegadas que evaporam na beira do corredor", "uv", "Pegadas fantasmagóricas se acendem com a UV"),
                Round("Sinal intermitente vindo do bloco B", "emf", "O EMF pulsa forte alinhado às celas"),
                Round("Passos ecoando na ala solitária", "evp", "O gravador prendeu os passos repetidos"),
                Round("Uma silhueta escorada nas grades", "camera", "A foto revelou a silhueta nas grades")
            ),
            "O Guarda entregou as chaves. A prisão ficou vazia de vez.",
            "O Guarda gritou e você teve que recuar. As celas continuam com dono."
        ),
        Scenario(
            3, "Escola à Noite", "sala 7, auditório", "A Diretora",
            4, 1250, 70,
            "A escola que fechou depois do incêndio. À noite o auditório volta a ter coro de crianças.",
            listOf(
                Round("A lousa se escreve sozinha", "uv", "Marcas de giz ectoplasmático aparecem na UV"),
                Round("Interferência nos laboratórios de ciência", "emf", "O EMF responde na porta do laboratório"),
                Round("Coro infantil no auditório vazio", "evp", "O gravador captou a cantoria do coro"),
                Round("Bonecos com olhos brilhando na biblioteca", "camera", "A foto pegou olhos brilhando na estante")
            ),
            "A Diretora se despediu do pátio. A escola pode dormir em paz.",
            "A Diretora apagou todas as luzes de uma vez. Você escapou pelo refeitório."
        ),
        Scenario(
            4, "Boate Fantasma", "subsolo, pista principal", "O DJ Morto",
            5, 1650, 90,
            "A boate fechou depois da morte do DJ. O som ainda enche a pista às 3h da manhã.",
            listOf(
                Round("Mãos esfriam a parede de neon", "uv", "A UV revela mãos pousando no neon"),
                Round("O grave distorce no subwoofer desligado", "emf", "O EMF dança junto com o grave"),
                Round("O microfone morto acorda sozinho", "evp", "O gravador achou a risada do DJ"),
                Round("Um par de luzes vermelhas flutua na pista", "camera", "A foto pegou os dois pontos vermelhos"),
                Round("Uma onda de som varre os cabos soltos", "evp", "O EVP captou o beat fantasma nos fios")
            ),
            "O DJ Morto deu o último fade out. A pista ficou muda para sempre.",
            "O DJ Morto mixou o silêncio e você perdeu o rastro. O après-festa foi seu."
        ),
        Scenario(
            5, "Granja da Colina", "celeiro, curral 2", "O Fazendeiro",
            6, 2100, 110,
            "A granja da colina perdeu o dono há uma década. O gado ainda o espera de noite.",
            listOf(
                Round("Pegadas na terra congelada do curral", "uv", "Pegadas de botas surgem na terra com a UV"),
                Round("Pulso forte vindo do celeiro", "emf", "O EMF pulsa no portão do celeiro"),
                Round("Um chamado grave no fim da cerca", "evp", "O gravador captou o chamado do capataz"),
                Round("O espantalho vira a cabeça devagar", "camera", "A foto pegou o espantalho te olhando"),
                Round("O mangueiro range sozinho no escuro", "camera", "A térmica pegou a marca de mão no rangido")
            ),
            "O Fazendeiro recolheu o gado e partiu. A colina ficou só com o vento.",
            "O Fazendeiro espantou você pelos campos. A granja continua aguardando dono."
        ),
        Scenario(
            6, "Cemitério do Norte", "necrópole, capela em ruínas", "A Coveira",
            7, 2600, 130,
            "A necrópole abandonada tem mais visitantes à noite do que durante o dia. A coveira nunca terminou sua última cova.",
            listOf(
                Round("Mãos arranham a tampa dos jazigos", "uv", "A UV marcou dedos frescos sobre o granito"),
                Round("O campo elétrico da capela vai e volta", "emf", "O EMF espirala ao redor do altar"),
                Round("Um soluço subindo do poço", "evp", "O gravador captou o soluço da cova aberta"),
                Round("Uma figura curva entre os ciprestes", "camera", "A térmica pegou a figura encurvada"),
                Round("As velas do altar acendem sozinhas", "uv", "A luz revela hálito gelado sobre as velas")
            ),
            "A Coveira fechou a última cova e deitou em paz. O cemitério silenciou.",
            "A Coveira apontou o dedo para você e a terra tremeu. Você fugiu do portão."
        ),
        Scenario(
            7, "Trem Fantasma", "linha do serrote, vagão 7", "O Maquinista",
            8, 3200, 150,
            "O trem que descarrilou em 1962 ainda percorre a linha nas madrugadas. O maquinista busca passageiros para a última viagem.",
            listOf(
                Round("Marcas de mãos no vidro empoeirado", "uv", "A UV ilumina mãos grudadas no vidro"),
                Round("O apito ferve nos trilhos frios", "emf", "O EMF pulsa sincronizado ao apito"),
                Round("Um bilhete repete a mesma frase", "evp", "O gravador captou a frase do bilhete fantasma"),
                Round("Uma forma esguia corre entre os vagões", "camera", "A térmica congelou a forma no corredor"),
                Round("A sineta toca sem ninguém no freio", "evp", "O EVP registrou o toque sem dono")
            ),
            "O Maquinista fez sua última estação e se despediu no horizonte. O trilho ficou mudo.",
            "O Maquinista arrancou e você saltou em movimento. O trem sumiu na neblina."
        ),
        Scenario(
            8, "Castelo Maldito", "torre negra, salão do trono", "O Senhor das Sombras",
            9, 8000, 200,
            "O antigo senhor do castelo domina o medo da região. Dizem que ele coleta as almas dos caçadores que tentaram o salão do trono. Só os melhores voltam.",
            listOf(
                Round("Pegadas de botas de alguma outra era", "uv", "A UV revela botas de uma era que não existiu"),
                Round("A coroa no trono irradia corrente", "emf", "O EMF enlouquece perto da coroa"),
                Round("Um conselho de vozes vem do trono", "evp", "O gravador captou o conselho das almas"),
                Round("A armadura vazia se move sem dono", "camera", "A térmica pegou a armadura abrindo o visor"),
                Round("As tochas apagam na mesma ordem", "uv", "A UV desenhou uma mão apagando as tochas"),
                Round("O chão estala com passos invisíveis", "emf", "O EMF chia em cada estalo do salão")
            ),
            "O Senhor das Sombras se desfez em névoa dourada. O castelo respirou luz pela primeira vez.",
            "O Senhor das Sombras fechou o salão atrás de você. Você escapou pela masmorra, sem os achados."
        )
    )

    const val STARTER_MONEY = 300
    const val STARTER_EQUIP = "uv"

    fun rankFor(total: Int): Rank =
        RANKS.lastOrNull { total >= it.min } ?: RANKS.first()

    fun equipById(id: String): Equip? = EQUIP.firstOrNull { it.id == id }

    fun consumableById(id: String): Consumable? = CONSUMABLES.firstOrNull { it.id == id }

    fun achievementById(id: String): Achievement? = ACHIEVEMENTS.firstOrNull { it.id == id }
}