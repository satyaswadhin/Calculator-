package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculatorRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<CalculationHistory>> = historyDao.getAllHistory()

    suspend fun insert(history: CalculationHistory) {
        historyDao.insertHistory(history)
    }

    suspend fun delete(id: Long) {
        historyDao.deleteHistoryItem(id)
    }

    suspend fun clearAll() {
        historyDao.clearHistory()
    }
}
