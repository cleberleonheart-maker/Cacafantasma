package com.caca.fantasma.game

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

class Auth(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun hasAccount(): Boolean = prefs.contains("user") && prefs.contains("salt")

    fun username(): String = prefs.getString("user", "") ?: ""

    fun register(user: String, pass: String): Boolean {
        if (hasAccount()) return false
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        prefs.edit()
            .putString("user", user.trim())
            .putString("salt", base64(salt))
            .putString("hash", hash(user.trim(), pass, salt))
            .apply()
        return true
    }

    fun login(user: String, pass: String): Boolean {
        if (!hasAccount()) return false
        val salt = base64Decode(prefs.getString("salt", "") ?: "")
        val expected = prefs.getString("hash", "") ?: ""
        return hash(user.trim(), pass, salt) == expected
    }

    fun logout() {} // sem estado de sessão: o jogo volta à tela de login

    fun wipe() {
        prefs.edit().clear().apply()
    }

    private fun hash(user: String, pass: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(user.lowercase().toByteArray())
        md.update(salt)
        md.update(pass.toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun base64(b: ByteArray): String =
        android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP)

    private fun base64Decode(s: String): ByteArray =
        android.util.Base64.decode(s, android.util.Base64.NO_WRAP)
}