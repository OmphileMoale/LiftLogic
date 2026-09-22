package com.example.liftlogic.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles password storage for LiftLogic's local (Room-backed) authentication.
 *
 * Passwords are never stored or compared in plain text (NFR3 in the LiftLogic
 * Planning & Design document). Each password is combined with a unique,
 * randomly generated salt before hashing, so that two users with the same
 * password never produce the same stored hash, and precomputed rainbow-table
 * attacks are not effective against the stored values.
 *
 * Approach follows the general guidance in the OWASP Password Storage Cheat
 * Sheet (salt + hash, unique salt per user):
 * https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
 */
object PasswordHasher {

    /**
     * Generates a new random 16-byte salt, encoded as a Base64 string so it can
     * be stored alongside the password hash in Room.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    /**
     * Hashes [password] together with [salt] using SHA-256.
     * The same password with a different salt will always produce a different hash.
     */
    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.decode(salt, Base64.NO_WRAP))
        val hashedBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    /**
     * Re-hashes [password] with the user's stored [salt] and checks it against
     * [expectedHash]. Used at login time instead of decrypting a stored password,
     * since hashing is a one-way operation.
     */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        return hash(password, salt) == expectedHash
    }
}