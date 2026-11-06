package com.example.liftlogic.data
import android.util.Log

import com.example.liftlogic.data.db.UserDao
import com.example.liftlogic.data.db.UserEntity
import com.example.liftlogic.util.PasswordHasher
import java.util.UUID

enum class AuthError {
    EMPTY_FIELDS,
    EMAIL_TAKEN,
    NO_ACCOUNT,
    WRONG_PASSWORD,
    INVALID_EMAIL
}

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Failure(val error: AuthError) : AuthResult()
}

class UserRepository(private val userDao: UserDao) {

    fun observeUser(userId: String) = userDao.observeUser(userId)

    suspend fun getUser(userId: String) = userDao.getUser(userId)

    suspend fun register(name: String, email: String, password: String, units: String): AuthResult {
        Log.d("UserRepository", "register: attempting for email=$email")
        val cleanEmail = email.trim().lowercase()
        if (name.isBlank() || cleanEmail.isBlank() || password.length < 8) {
            return AuthResult.Failure(AuthError.EMPTY_FIELDS)
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return AuthResult.Failure(AuthError.INVALID_EMAIL)
        }
        if (userDao.countByEmail(cleanEmail) > 0) {
            return AuthResult.Failure(AuthError.EMAIL_TAKEN)
        }

        val salt = PasswordHasher.generateSalt()
        val user = UserEntity(
            userId = UUID.randomUUID().toString(),
            name = name.trim(),
            email = cleanEmail,
            passwordHash = PasswordHasher.hash(password, salt),
            passwordSalt = salt,
            units = units
        )
        userDao.insert(user)
        return AuthResult.Success(user)
    }

    suspend fun login(email: String, password: String): AuthResult {
        Log.d("UserRepository", "login: attempting for email=$email")
        if (email.isBlank() || password.isBlank()) return AuthResult.Failure(AuthError.EMPTY_FIELDS)
        val user = userDao.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Failure(AuthError.NO_ACCOUNT)

        return if (PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
            AuthResult.Success(user)
        } else {
            AuthResult.Failure(AuthError.WRONG_PASSWORD)
        }
    }

    /** Log in or automatically create a user account via Google Sign-In */
    suspend fun loginOrRegisterWithGoogle(email: String, name: String, googleId: String): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val existingUser = userDao.findByEmail(cleanEmail)
        if (existingUser != null) {
            return AuthResult.Success(existingUser)
        }

        val salt = PasswordHasher.generateSalt()
        val user = UserEntity(
            userId = UUID.randomUUID().toString(),
            name = name.ifBlank { "Google User" },
            email = cleanEmail,
            passwordHash = PasswordHasher.hash(googleId, salt),
            passwordSalt = salt,
            units = "kg"
        )
        userDao.insert(user)
        return AuthResult.Success(user)
    }

    /** Applies [change] to the stored user and saves it. */
    suspend fun update(userId: String, change: (UserEntity) -> UserEntity) {
        val user = userDao.getUser(userId) ?: return
        userDao.update(change(user))
    }

    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): AuthResult {
        val user = userDao.getUser(userId) ?: return AuthResult.Failure(AuthError.NO_ACCOUNT)
        if (!PasswordHasher.verify(currentPassword, user.passwordSalt, user.passwordHash)) {
            return AuthResult.Failure(AuthError.WRONG_PASSWORD)
        }
        if (newPassword.length < 8) return AuthResult.Failure(AuthError.EMPTY_FIELDS)

        val salt = PasswordHasher.generateSalt()
        val updated = user.copy(passwordHash = PasswordHasher.hash(newPassword, salt), passwordSalt = salt)
        userDao.update(updated)
        return AuthResult.Success(updated)
    }
}
