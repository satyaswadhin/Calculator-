package com.example.math

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.*

class ExpressionEvaluator(private val isDegreeMode: Boolean = false) {

    fun evaluate(expr: String): Double {
        val sanitized = sanitize(expr)
        val tokens = tokenize(sanitized)
        val parser = Parser(tokens, isDegreeMode)
        return parser.parse()
    }

    companion object {
        fun formatResult(value: Double): String {
            if (value.isNaN()) return "NaN"
            if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"

            try {
                // If value is an integer or very close to an integer
                if (floor(value) == value && value < Long.MAX_VALUE && value > Long.MIN_VALUE) {
                    return value.toLong().toString()
                }

                // Handle common floating point issues (e.g. 0.1 + 0.2)
                val bd = BigDecimal(value).setScale(12, RoundingMode.HALF_UP)
                val roundedVal = bd.toDouble()
                if (floor(roundedVal) == roundedVal && roundedVal < Long.MAX_VALUE && roundedVal > Long.MIN_VALUE) {
                    return roundedVal.toLong().toString()
                }

                if (abs(value) >= 1e12 || (abs(value) < 1e-5 && value != 0.0)) {
                    val df = DecimalFormat("0.######E0")
                    return df.format(roundedVal)
                }

                var formatted = String.format(Locale.US, "%.10f", roundedVal)
                if (formatted.contains(".")) {
                    while (formatted.endsWith("0")) {
                        formatted = formatted.substring(0, formatted.length - 1)
                    }
                    if (formatted.endsWith(".")) {
                        formatted = formatted.substring(0, formatted.length - 1)
                    }
                }
                return formatted
            } catch (e: Exception) {
                return value.toString()
            }
        }
    }

    private fun sanitize(expr: String): String {
        var str = expr.replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("√", "sqrt")
            .replace("mod", "%")

        // Add implicit multiply between a closing parenthesis, digit,pi,e or function and another opening parenthesis, etc.
        // e.g., 2(3) -> 2*(3), (2)(3) -> (2)*(3)
        // Match numbers, decimals, e, pi, or ')' followed directly by function name, '(', or e, pi, digit
        // Set of functions/constants: ( | pi | e | sin | cos | tan | asin | acos | atan | log | ln | sqrt | abs | \d
        // 1. Digit, pi, e, ')' or factorial followed by opening parenthesis or function name
        str = Regex("(\\d|pi|e|\\)|!)(\\()").replace(str) { match ->
            match.groupValues[1] + "*" + match.groupValues[2]
        }
        
        // 2. Digit, pi, e, ')' or factorial followed by pi, e, or function
        str = Regex("(\\d|pi|e|\\)|!)(pi|e|sin|cos|tan|asin|acos|atan|log|ln|sqrt|abs)").replace(str) { match ->
            match.groupValues[1] + "*" + match.groupValues[2]
        }

        // 3. Right parenthesis directly followed by number
        str = Regex("(\\))(\\d+)").replace(str) { match ->
            match.groupValues[1] + "*" + match.groupValues[2]
        }

        return str
    }

    private fun tokenize(str: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            when {
                c.isWhitespace() -> { i++ }
                c in "+-*/%^()!" -> {
                    tokens.add(c.toString())
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < str.length && (str[i].isDigit() || str[i] == '.')) {
                        sb.append(str[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < str.length && str[i].isLetter()) {
                        sb.append(str[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
                else -> {
                    // Ignore or throw contextually
                    i++
                }
            }
        }
        return tokens
    }

    private class Parser(private val tokens: List<String>, private val isDegree: Boolean) {
        private var index = 0

        fun parse(): Double {
            if (tokens.isEmpty()) return 0.0
            val res = expr()
            if (index < tokens.size) {
                throw IllegalArgumentException("Unexpected sequence after parsed input: ${tokens[index]}")
            }
            return res
        }

        private fun peek(): String? = if (index < tokens.size) tokens[index] else null
        private fun consume(): String = tokens[index++]

        // Expression -> Term (+ or - Term)*
        private fun expr(): Double {
            var val1 = term()
            while (true) {
                val next = peek()
                if (next == "+" || next == "-") {
                    consume()
                    val val2 = term()
                    if (next == "+") val1 += val2 else val1 -= val2
                } else {
                    break
                }
            }
            return val1
        }

        // Term -> Factor (* or / or % Factor)*
        private fun term(): Double {
            var val1 = factor()
            while (true) {
                val next = peek()
                if (next == "*" || next == "/" || next == "%") {
                    consume()
                    val val2 = factor()
                    if (next == "*") {
                        val1 *= val2
                    } else if (next == "/") {
                        if (val2 == 0.0) throw ArithmeticException("Division by zero")
                        val1 /= val2
                    } else {
                        val1 %= val2
                    }
                } else {
                    break
                }
            }
            return val1
        }

        // Factor -> Primary (^ Primary)*  (Exponentiation)
        private fun factor(): Double {
            var val1 = primary()
            if (peek() == "^") {
                consume()
                val val2 = factor() // right-associative power evaluation
                val1 = val1.pow(val2)
            }
            return val1
        }

        // Primary -> - Primary | + Primary | Numeric | Constant | Function | ( Expression ) | Primary!
        private fun primary(): Double {
            val next = peek() ?: throw IllegalArgumentException("Term expected")
            var result: Double

            if (next == "-") {
                consume()
                result = -primary()
            } else if (next == "+") {
                consume()
                result = primary()
            } else if (next == "(") {
                consume()
                result = expr()
                if (peek() == ")") {
                    consume()
                } else {
                    throw IllegalArgumentException("Parentheses mismatched")
                }
            } else if (isNumber(next)) {
                result = consume().toDouble()
            } else if (next == "pi" || next == "e") {
                consume()
                result = if (next == "pi") Math.PI else Math.E
            } else if (isFunction(next)) {
                val func = consume()
                // Take parenthesis or parse direct argument factor
                val arg = if (peek() == "(") {
                    consume() // (
                    val r = expr()
                    if (peek() == ")") {
                        consume() // )
                    } else {
                        throw IllegalArgumentException("Parentheses mismatched in $func")
                    }
                    r
                } else {
                    primary() // evaluates simple factor, e.g., sin pi
                }
                result = evaluateFunction(func, arg)
            } else {
                throw IllegalArgumentException("Unexpected symbol: $next")
            }

            // Parse postfix factorial if present
            while (peek() == "!") {
                consume()
                result = factorial(result)
            }

            return result
        }

        private fun isNumber(s: String): Boolean {
            return s.firstOrNull()?.isDigit() == true || s.startsWith(".")
        }

        private fun isFunction(s: String): Boolean {
            return s in setOf("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "abs")
        }

        private fun evaluateFunction(func: String, arg: Double): Double {
            return when (func) {
                "sin" -> sin(if (isDegree) Math.toRadians(arg) else arg)
                "cos" -> cos(if (isDegree) Math.toRadians(arg) else arg)
                "tan" -> tan(if (isDegree) Math.toRadians(arg) else arg)
                "asin" -> {
                    if (arg !in -1.0..1.0) throw IllegalArgumentException("asin domain [-1,1]")
                    val r = asin(arg)
                    if (isDegree) Math.toDegrees(r) else r
                }
                "acos" -> {
                    if (arg !in -1.0..1.0) throw IllegalArgumentException("acos domain [-1,1]")
                    val r = acos(arg)
                    if (isDegree) Math.toDegrees(r) else r
                }
                "atan" -> {
                    val r = atan(arg)
                    if (isDegree) Math.toDegrees(r) else r
                }
                "log" -> {
                    if (arg <= 0.0) throw IllegalArgumentException("log domain must be positive")
                    log10(arg)
                }
                "ln" -> {
                    if (arg <= 0.0) throw IllegalArgumentException("ln domain must be positive")
                    ln(arg)
                }
                "sqrt" -> {
                    if (arg < 0.0) throw ArithmeticException("Square root of negative number")
                    sqrt(arg)
                }
                "abs" -> abs(arg)
                else -> throw IllegalArgumentException("Unknown expression function $func")
            }
        }

        private fun factorial(value: Double): Double {
            val n = value.roundToInt()
            if (n < 0 || abs(value - n) > 1e-9) {
                throw IllegalArgumentException("Factorial requires non-negative integer")
            }
            if (n > 100) return Double.POSITIVE_INFINITY
            var r = 1.0
            for (i in 2..n) {
                r *= i
            }
            return r
        }
    }
}
