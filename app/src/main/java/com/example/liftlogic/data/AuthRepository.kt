package com.example.liftlogic.data

import com.example.liftlogic.util.PasswordHasher
import java.util.UUID

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
        if (name.isBlank() || email.isBlank() || password.length < 8) {
            return AuthResult.Failure(AuthError.EMPTY_FIELDS)
        }
        if (userDao.countByEmail(email.trim().lowercase()) > 0) {
            return AuthResult.Failure(AuthError.EMAIL_TAKEN)
        }

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
        return AuthResult.Success(user)
    }

    suspend fun login(email: String, password: String): AuthResult {
        val user = userDao.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Failure(AuthError.NO_ACCOUNT)

        return if (PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
            AuthResult.Success(user)
        } else {
            AuthResult.Failure(AuthError.WRONG_PASSWORD)
        }
    }

    suspend fun updateSettings(userId: String, units: String, notificationsEnabled: Boolean): UserEntity? {
        val user = userDao.getUser(userId) ?: return null
        val updated = user.copy(units = units, notificationsEnabled = notificationsEnabled)
        userDao.update(updated)
        return updated
    }
}