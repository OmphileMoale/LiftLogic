package com.example.liftlogic.data

import com.example.liftlogic.data.db.BodyWeightDao
import com.example.liftlogic.data.db.BodyWeightEntity

class BodyWeightRepository(private val dao: BodyWeightDao) {

    fun observeHistory(userId: String) = dao.observeHistory(userId)

    suspend fun log(userId: String, weightKg: Double, at: Long = System.currentTimeMillis()) {
        dao.insert(BodyWeightEntity(userId = userId, weightKg = weightKg, recordedAt = at))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
