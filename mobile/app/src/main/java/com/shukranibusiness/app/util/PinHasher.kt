package com.shukranibusiness.app.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hachage du PIN employé : PBKDF2 (HmacSHA256, 10 000 itérations) avec un sel aléatoire propre
 * à chaque employé, stocké avec le hash sous la forme "sel:hash" (hexadécimal).
 *
 * Compatibilité ascendante : les PIN créés avant ce changement utilisaient SHA-256 avec un sel
 * fixe partagé par tous les employés — un PIN à 4-6 chiffres n'a qu'un petit nombre de
 * combinaisons possibles, donc un sel fixe permettait de précalculer une seule fois tous les
 * hash possibles et de "casser" n'importe quel PIN instantanément en cas d'extraction de la
 * base. [matches] reconnaît encore cet ancien format ; [isLegacyFormat] permet à
 * [com.shukranibusiness.app.data.ShopRepository.authenticate] de migrer silencieusement vers le
 * nouveau format dès la prochaine connexion réussie, sans forcer de réinitialisation de PIN.
 */
object PinHasher {

    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256
    private const val LEGACY_SALT = "shukrani-business-pos"

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "${salt.toHex()}:${pbkdf2(pin, salt).toHex()}"
    }

    fun matches(pin: String, storedHash: String): Boolean {
        val separator = storedHash.indexOf(':')
        if (separator == -1) {
            return legacyHash(pin) == storedHash
        }
        val salt = storedHash.substring(0, separator).hexToBytes()
        val expected = storedHash.substring(separator + 1).hexToBytes()
        return MessageDigest.isEqual(pbkdf2(pin, salt), expected)
    }

    /** true si [storedHash] utilise encore l'ancien format (sel fixe, sans itérations). */
    fun isLegacyFormat(storedHash: String): Boolean = !storedHash.contains(':')

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun legacyHash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((LEGACY_SALT + pin).toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        ByteArray(length / 2) { i -> ((this[i * 2].digitToInt(16) shl 4) + this[i * 2 + 1].digitToInt(16)).toByte() }
}
