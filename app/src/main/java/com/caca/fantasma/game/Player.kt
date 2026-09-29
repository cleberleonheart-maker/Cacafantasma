package com.caca.fantasma.game

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class Player(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("player", Context.MODE_PRIVATE)

    var money: Int
        get() = prefs.getInt("money", 0)
        set(v) = prefs.edit().putInt("money", v).apply()

    var maxUnlocked: Int
        get() = prefs.getInt("max_unlocked", 0)
        set(v) = prefs.edit().putInt("max_unlocked", v).apply()

    var totalCaptures: Int
        get() = prefs.getInt("total_captures", 0)
        set(v) = prefs.edit().putInt("total_captures", v).apply()

    var totalSpent: Int
        get() = prefs.getInt("total_spent", 0)
        set(v) = prefs.edit().putInt("total_spent", v).apply()

    fun levelOf(id: String): Int = prefs.getInt("lv_$id", 0)

    fun owns(id: String): Boolean = levelOf(id) > 0

    fun setLevel(id: String, level: Int) {
        prefs.edit().putInt("lv_$id", level).apply()
    }

    fun capturesOf(index: Int): Int = prefs.getInt("cap_$index", 0)

    fun addCapture(index: Int, reward: Int) {
        prefs.edit().apply {
            putInt("cap_$index", capturesOf(index) + 1)
            putInt("total_captures", totalCaptures + 1)
            putInt("money", money + reward)
            if (index >= maxUnlocked && index + 1 < GameData.SCENARIOS.size) {
                putInt("max_unlocked", index + 1)
            }
        }.apply()
    }

    // ---------- consumables ----------

    fun itemCount(id: String): Int = prefs.getInt("itm_$id", 0)

    fun addItem(id: String, n: Int) {
        prefs.edit().putInt("itm_$id", itemCount(id) + n).apply()
    }

    fun useItem(id: String): Boolean {
        if (itemCount(id) <= 0) return false
        prefs.edit().putInt("itm_$id", itemCount(id) - 1).apply()
        return true
    }

    // ---------- achievements ----------

    fun achUnlocked(id: String): Boolean = prefs.getBoolean("ach_$id", false)

    fun unlockAch(id: String) {
        prefs.edit().putBoolean("ach_$id", true).apply()
    }

    fun allScenariosCaptured(): Boolean =
        GameData.SCENARIOS.all { capturesOf(it.index) > 0 }

    fun bossCaptured(): Boolean =
        GameData.SCENARIOS.lastOrNull { it.isBoss }?.let { capturesOf(it.index) > 0 } ?: false

    fun achievementsUnlocked(): Int = GameData.ACHIEVEMENTS.count { achUnlocked(it.id) }

    fun unlockedAllies(): List<Ally> =
        GameData.ALLIES.filter { totalCaptures >= it.unlockAtCaptures }

    fun hasAlly(id: String): Boolean =
        GameData.ALLIES.any { it.id == id && totalCaptures >= it.unlockAtCaptures }

    fun nextAlly(): Ally? =
        GameData.ALLIES.filter { totalCaptures < it.unlockAtCaptures }.minByOrNull { it.unlockAtCaptures }

    // ---------- ranks ----------

    fun rankData(): Rank = GameData.rankFor(totalCaptures)

    fun rank(): String = rankData().name

    fun rewardMultiplier(reward: Int): Int = (reward * (1f + rankData().rewardBonus)).toInt()

    fun discountedPrice(price: Int): Int {
        val d = rankData().shopDiscount
        return if (d <= 0) price else (price * (100 - d) / 100)
    }

    // ---------- persistence ----------

    fun reset() {
        prefs.edit().clear().apply()
    }

    fun equipUnlocked(e: Equip): Boolean = e.unlockAfter < 0 || maxUnlocked > e.unlockAfter

    fun grantStarterKit() {
        prefs.edit().apply {
            putInt("money", maxOf(money, GameData.STARTER_MONEY))
            putInt("lv_${GameData.STARTER_EQUIP}", maxOf(levelOf(GameData.STARTER_EQUIP), 1))
        }.apply()
    }

    fun ensureStarterKit() {
        if (GameData.EQUIP.none { owns(it.id) }) grantStarterKit()
    }

    fun backupJson(): JSONObject = JSONObject().apply {
        put("money", money)
        put("max_unlocked", maxUnlocked)
        put("total_captures", totalCaptures)
        put("total_spent", totalSpent)
        put("equip", org.json.JSONArray().apply {
            GameData.EQUIP.forEach { if (levelOf(it.id) > 0) put(JSONObject().put(it.id, levelOf(it.id))) }
        })
        put("items", org.json.JSONArray().apply {
            GameData.CONSUMABLES.forEach { if (itemCount(it.id) > 0) put(JSONObject().put(it.id, itemCount(it.id))) }
        })
        put("captures", org.json.JSONArray().apply {
            GameData.SCENARIOS.forEach { put(capturesOf(it.index)) }
        })
        put("achievements", org.json.JSONArray().apply {
            GameData.ACHIEVEMENTS.forEach { if (achUnlocked(it.id)) put(it.id) }
        })
    }

    // ---------- save import ----------
    // O arquivo vem do usuario: toda entrada e tratada como nao confiavel.
    // Devolve null em caso de sucesso, ou a mensagem de erro.

    fun restoreJson(json: JSONObject): String? {
        val lastIndex = GameData.SCENARIOS.size - 1
        val newMoney = json.optInt("money", 0).coerceIn(0, MAX_VALUE)
        val newCaptures = json.optInt("total_captures", 0).coerceIn(0, MAX_VALUE)
        val newSpent = json.optInt("total_spent", 0).coerceIn(0, MAX_VALUE)

        val capArray = json.optJSONArray("captures")
        val newCaps = IntArray(GameData.SCENARIOS.size) { i ->
            capArray?.optInt(i, 0)?.coerceIn(0, MAX_CAPTURES) ?: 0
        }
        val highest = newCaps.indexOfLast { it > 0 }
        val newUnlocked = json.optInt("max_unlocked", 0)
            .coerceIn(0, lastIndex)
            .coerceAtLeast(highest)

        val equipLevels = HashMap<String, Int>()
        json.optJSONArray("equip")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.keys().asSequence().firstOrNull() ?: continue
                val e = GameData.equipById(id) ?: continue
                equipLevels[e.id] = obj.optInt(id, 0).coerceIn(1, MAX_EQUIP_LEVEL)
            }
        }

        val itemCounts = HashMap<String, Int>()
        json.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.keys().asSequence().firstOrNull() ?: continue
                val c = GameData.CONSUMABLES.firstOrNull { it.id == id } ?: continue
                val n = obj.optInt(id, 0).coerceIn(0, MAX_VALUE)
                if (n > 0) itemCounts[c.id] = n
            }
        }

        val achIds = HashSet<String>()
        json.optJSONArray("achievements")?.let { arr ->
            for (i in 0 until arr.length()) {
                val id = arr.optString(i, "")
                if (GameData.achievementById(id) != null) achIds.add(id)
            }
        }

        prefs.edit().apply {
            clear()
            putInt("money", newMoney)
            putInt("max_unlocked", newUnlocked)
            putInt("total_captures", newCaptures.coerceAtLeast(newCaps.sum()))
            putInt("total_spent", newSpent)
            equipLevels.forEach { (id, level) -> putInt("lv_$id", level) }
            itemCounts.forEach { (id, n) -> putInt("itm_$id", n) }
            newCaps.forEachIndexed { index, n -> putInt("cap_$index", n) }
            achIds.forEach { putBoolean("ach_$it", true) }
        }.apply()

        if (GameData.EQUIP.none { owns(it.id) }) grantStarterKit()
        return null
    }

    private companion object {
        const val MAX_VALUE = 10_000_000
        const val MAX_CAPTURES = 9_999
        const val MAX_EQUIP_LEVEL = 3
    }
}