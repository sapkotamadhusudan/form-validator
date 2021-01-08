package com.maddy.formvalidator

/**
 * Created by Madhusudan Sapkota on 8/30/20.
 */
object Validator {

    fun <V> validate(value: V?, rules: ValidationRules?): String? {
        if (rules == null) return null

        val error = validateRequired(value, rules.required)
        if (error != null) return error

        return when (value) {
            is String -> validateString(value, rules)
            is Int -> validateNumber(value.toDouble(), rules)
            is Double -> validateNumber(value.toDouble(), rules)
            is Collection<*> -> validateCollection(value, rules)
            else -> null
        }
    }

    private fun <V> validateRequired(value: V?, rule: RequiredValidation?): String? {
        if (rule == null) return null

        if (value == null) return rule.message

        if (value is Boolean && value == false) return rule.message

        if (value is String && value.isEmpty()) return rule.message

        return null
    }

    private fun validateCollection(collection: Collection<*>, rules: ValidationRules?): String? {
        return if (rules?.required != null && collection.isEmpty()) rules.required.message else null
    }

    private fun validateString(value: String, rules: ValidationRules): String? {
        var error = checkLength(value, rules.length)
        if (error != null) return error

        val currentValue = value.toDoubleOrNull()
        if (currentValue != null) {
            error = validateNumber(currentValue, rules)
            if (error != null) return error
        }

        error = if (rules.custom != null) rules.custom.validator(value) else null
        if (error != null) return error

        error = checkPattern(value, rules.regex)
        if (error != null) return error

        return null
    }

    private fun validateNumber(value: Double, rules: ValidationRules): String? {
        return checkRange(value, rules.range)
    }

    private fun checkPattern(value: String, rule: RegexValidation?): String? {
        return if (rule?.pattern?.every{ it.matches(value) } == false) rule.message else null
    }

    private fun checkLength(value: String, rule: LengthValidation?): String? {
        return if (isLessThan(value, rule?.min) || isGreaterThan(
                value,
                rule?.max
            )
        ) rule?.message else null
    }

    private fun checkRange(value: Double, rule: RangeValidation?): String? {
        return if (isLessThan(value, rule?.min) || isGreaterThan(
                value,
                rule?.max
            )
        ) rule?.message else null
    }

    private fun isGreaterThan(arg1: String, arg2: Int?): Boolean {
        return arg2 != null && arg1.length > arg2
    }

    private fun isLessThan(arg1: String, arg2: Int?): Boolean {
        return arg2 != null && arg1.length < arg2
    }

    private fun isGreaterThan(arg1: Double, arg2: Double?): Boolean {
        return arg2 != null && arg1 > arg2
    }

    private fun isLessThan(arg1: Double, arg2: Double?): Boolean {
        return arg2 != null && arg1 < arg2
    }
}

