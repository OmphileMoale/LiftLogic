package com.example.liftlogic.data

import android.util.Log
import com.example.liftlogic.util.PasswordHasher
import java.util.UUID

private const val TAG = "AuthRepository"

enum class AuthError {
    EMPTY_FIELDS,
    EMAIL_TAKEN,
    NO_ACCOUNT,
    WRONG_PASSWORD
}

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Failure(val error: AuthError) : AuthResult()
}

class AuthRepository(private val userDao: UserDao) {

    suspend fun register(name: String, email: String, password: String, units: String): AuthResult {
        Log.d(TAG, "register: attempt for email=${email.trim().lowercase()}")

        if (name.isBlank() || email.isBlank() || password.length < 8) {
            Log.w(TAG, "register: rejected, empty fields or password under 8 characters")
            return AuthResult.Failure(AuthError.EMPTY_FIELDS)
        }
        if (userDao.countByEmail(email.trim().lowercase()) > 0) {
            Log.w(TAG, "register: rejected, email already registered")
            return AuthResult.Failure(AuthError.EMAIL_TAKEN)
        }

        // Never log the raw password — only the salted hash is persisted.
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)

        val user = UserEntity(
            userId = UUID.randomUUID().toString(),
            name = name.trim(),
            email = email.trim().lowercase(),
            passwordHash = hash,
            passwordSalt = salt,
            units = units
        )
        userDao.insert(user)
        Log.i(TAG, "register: success, new userId=${user.userId}")
        return AuthResult.Success(user)
    }

    suspend fun login(email: String, password: String): AuthResult {
        Log.d(TAG, "login: attempt for email=${email.trim().lowercase()}")

        val user = userDao.findByEmail(email.trim().lowercase())
        if (user == null) {
            Log.w(TAG, "login: no account found for this email")
            return AuthResult.Failure(AuthError.NO_ACCOUNT)
        }

        return if (PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
            Log.i(TAG, "login: success for userId=${user.userId}")
            AuthResult.Success(user)
        } else {
            Log.w(TAG, "login: password mismatch for userId=${user.userId}")
            AuthResult.Failure(AuthError.WRONG_PASSWORD)
        }
    }

    suspend fun updateSettings(userId: String, units: String, notificationsEnabled: Boolean): UserEntity? {
        Log.d(TAG, "updateSettings: userId=$userId units=$units notificationsEnabled=$notificationsEnabled")
        val user = userDao.getUser(userId) ?: run {
            Log.w(TAG, "updateSettings: no user found for userId=$userId")
            return null
        }
        val updated = user.copy(units = units, notificationsEnabled = notificationsEnabled)
        userDao.update(updated)
        Log.i(TAG, "updateSettings: saved for userId=$userId")
        return updated
    }
}