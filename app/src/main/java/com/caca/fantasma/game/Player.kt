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
            putInt("money", maxOf(money, 300))
            putInt("lv_uv", maxOf(levelOf("uv"), 1))
        }.apply()
    }

    fun ensureStarterKit() {
        val ownsNothing = GameData.EQUIP.none { levelOf(it.id) > 0 }
        if (ownsNothing) grantStarterKit()
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
}