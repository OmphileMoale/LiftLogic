package com.example.liftlogic.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val units: String = "kg",
    val xp: Int = 0,
    val streakDays: Int = 0,
    val notificationsEnabled: Boolean = true
)