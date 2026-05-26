package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalculationHistory
import com.example.data.CalculatorDatabase
import com.example.data.CalculatorRepository
import com.example.math.ExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CalculatorRepository
    private val prefs = application.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _realtimeResult = MutableStateFlow("")
    val realtimeResult: StateFlow<String> = _realtimeResult.asStateFlow()

    private val _isDegreeMode = MutableStateFlow(true)
    val isDegreeMode: StateFlow<Boolean> = _isDegreeMode.asStateFlow()

    private val _themeSetting = MutableStateFlow("system") // "system", "light", "dark"
    val themeSetting: StateFlow<String> = _themeSetting.asStateFlow()

    private val _history = MutableStateFlow<List<CalculationHistory>>(emptyList())
    val history: StateFlow<List<CalculationHistory>> = _history.asStateFlow()

    init {
        val database = CalculatorDatabase.getDatabase(application)
        repository = CalculatorRepository(database.historyDao())

        // Load mode and theme preferences
        _isDegreeMode.value = prefs.getBoolean("is_degree_mode", true)
        _themeSetting.value = prefs.getString("theme_mode", "system") ?: "system"

        // Observe calculation database history
        viewModelScope.launch {
            repository.allHistory.collectLatest { list ->
                _history.value = list
            }
        }
    }

    fun setThemeSetting(theme: String) {
        _themeSetting.value = theme
        prefs.edit().putString("theme_mode", theme).apply()
    }

    fun toggleTrigMode() {
        val newVal = !_isDegreeMode.value
        _isDegreeMode.value = newVal
        prefs.edit().putBoolean("is_degree_mode", newVal).apply()
        // Re-evaluate current expression with the new mode
        evaluateExpression(isFinal = false)
    }

    fun appendInput(charValue: String) {
        val currentExp = _expression.value
        
        // Custom smart behaviors
        val toAppend = when (charValue) {
            "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "abs" -> "$charValue("
            "√" -> "√("
            else -> charValue
        }

        _expression.value = currentExp + toAppend
        evaluateExpression(isFinal = false)
    }

    fun handlePercent() {
        val exp = _expression.value
        if (exp.isNotEmpty()) {
            _expression.value = "$exp%"
            evaluateExpression(isFinal = false)
        }
    }

    fun handleNegate() {
        val exp = _expression.value
        if (exp.isEmpty()) {
            _expression.value = "-"
        } else {
            // If starts with '-' and is just a simple number or term, remove it, otherwise wrap
            if (exp.startsWith("-(") && exp.endsWith(")")) {
                _expression.value = exp.substring(2, exp.length - 1)
            } else if (exp.startsWith("-")) {
                // If it is simple digits like -45
                if (exp.substring(1).all { it.isDigit() || it == '.' }) {
                    _expression.value = exp.substring(1)
                } else {
                    _expression.value = "-($exp)"
                }
            } else {
                if (exp.all { it.isDigit() || it == '.' }) {
                    _expression.value = "-$exp"
                } else {
                    _expression.value = "-($exp)"
                }
            }
        }
        evaluateExpression(isFinal = false)
    }

    fun deleteLast() {
        val currentExp = _expression.value
        if (currentExp.isNotEmpty()) {
            // Check if deleting a fully appended function string e.g. "sin("
            val functions = listOf("asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(", "sqrt(")
            var deletedFunc = false
            for (f in functions) {
                if (currentExp.endsWith(f)) {
                    _expression.value = currentExp.substring(0, currentExp.length - f.length)
                    deletedFunc = true
                    break
                }
            }
            if (!deletedFunc) {
                // Also check visual "√("
                if (currentExp.endsWith("√(")) {
                    _expression.value = currentExp.substring(0, currentExp.length - 2)
                } else {
                    _expression.value = currentExp.dropLast(1)
                }
            }
        }
        evaluateExpression(isFinal = false)
    }

    fun clearAll() {
        _expression.value = ""
        _realtimeResult.value = ""
    }

    fun selectHistoryItem(item: CalculationHistory) {
        _expression.value = item.expression
        _realtimeResult.value = item.result
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun evaluateExpression(isFinal: Boolean) {
        val exp = _expression.value.trim()
        if (exp.isEmpty()) {
            _realtimeResult.value = ""
            return
        }

        try {
            val evaluator = ExpressionEvaluator(isDegreeMode = _isDegreeMode.value)
            val computedValue = evaluator.evaluate(exp)
            val formatted = ExpressionEvaluator.formatResult(computedValue)

            if (isFinal) {
                _realtimeResult.value = formatted
                // Save valid calculation to DB history
                viewModelScope.launch {
                    repository.insert(
                        CalculationHistory(
                            expression = exp,
                            result = formatted
                        )
                    )
                }
                // Once evaluated, clear the primary equation field or set it to result so user can continue
                _expression.value = formatted
                _realtimeResult.value = ""
            } else {
                _realtimeResult.value = formatted
            }
        } catch (e: Exception) {
            if (isFinal) {
                // Show errors on final evaluation
                val rawMessage = e.message ?: "Error"
                val cleanedMsg = when {
                    rawMessage.contains("zero", ignoreCase = true) -> "Divide by zero"
                    rawMessage.contains("domain", ignoreCase = true) -> "Invalid domain"
                    rawMessage.contains("mismatched", ignoreCase = true) -> "Parenthesis error"
                    else -> "Syntax Error"
                }
                _realtimeResult.value = cleanedMsg
            } else {
                // Keep live preview clear on partial formula entry errors
                _realtimeResult.value = ""
            }
        }
    }
}
