package com.shukranibusiness.app.util

import java.security.MessageDigest

object PinHasher {

    private const val SALT = "shukrani-business-pos"

    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    fun matches(pin: String, storedHash: String): Boolean = hash(pin) == storedHash
}
