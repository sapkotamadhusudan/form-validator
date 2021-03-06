package com.maddy.formvalidator

import android.content.Context
import androidx.annotation.StringRes
import java.text.NumberFormat
import java.util.*

/**
 * Created by Madhusudan Sapkota on 8/30/20.
 */
open class ValidationRules(
    val regex: RegexValidation? = null,
    val range: RangeValidation? = null,
    val length: LengthValidation? = null,
    val required: RequiredValidation? = null,
    val compareFor: CompareForValidation? = null,
    val compareWith: CompareWithValidation? = null,
    val custom: CustomValidation? = null
)

sealed class ValidationRule(val message: String)

class CompareWithValidation(val compareWith: String, message: String) : ValidationRule(message)
class CompareForValidation(val compareFor: String) : ValidationRule("")

class CustomValidation(val validator: (String?) -> String?) : ValidationRule("")

class RequiredValidation(message: String, val requiredIfNotEmpty: Array<String>? = null) :
    ValidationRule(message)

class LengthValidation(
    val min: Int = 0,
    val max: Int? = null,
    message: String
) : ValidationRule(message)

class RangeValidation(
    val min: Double = 0.toDouble(),
    val max: Double? = null,
    message: String
) : ValidationRule(message)

class RegexValidation(
    val pattern: Regex,
    message: String
) : ValidationRule(message)


object Rules {

    object Patterns {
        val NAME = Regex("^[a-zA-Z.]+\$")
        val FULL_NAME = Regex("^[a-zA-Z. ]+\$")
        val EMAIL =
            Regex("^([A-Z|a-z|0-9](\\.|_){0,1})+[A-Z|a-z|0-9]\\@([A-Z|a-z|0-9])+((\\.){0,1}[A-Z|a-z|0-9]){2}\\.[a-z]{2,3}(.[n]{1}[p]{1})?\$")
        val DIGIT_EXIST_REGEX = Regex(".*\\d+.*")
        val UPPERCASE_EXIST_REGEX = Regex(".*[A-Z].*")
        val LOWERCASE_EXIST_REGEX = Regex(".*[a-z].*")
    }

    enum class Message(@StringRes val idRes: Int) {
        REQUIRED(R.string.message_required),
        LENGTH(R.string.message_length_required),
        MAX_LENGTH(R.string.message_max_length_required),
        MIN_LENGTH(R.string.message_min_length_required),
        MIN_MAX_LENGTH(R.string.message_min_max_length_required),

        RANGE(R.string.message_range_required),
        MAX_RANGE(R.string.message_max_range_required),
        MIN_RANGE(R.string.message_min_range_required),
        MIN_MAX_RANGE(R.string.message_min_max_range_required),

        INVALID(R.string.message_invalid);
    }

    fun getMessage(context: Context, type: Message, vararg args: String?): String {
        return context.getString(type.idRes, *args)
    }

    class Builder(private val label: String) {

        private var required: Boolean = false
        private var requiredIfNotEmpty: Array<String>? = null

        private var minLength: Int? = null
        private var maxLength: Int? = null

        private var minRange: Double? = null
        private var maxRange: Double? = null

        private var pattern: Regex? = null

        private var compareFor: String? = null
        private var compareWith: String? = null

        private var custom: CustomValidation? = null

        private val numberFormatter = NumberFormat.getInstance(Locale.getDefault())


        fun required(required: Boolean = true, requiredIfNotEmpty: Array<String>? = null): Builder {
            this.required = required
            this.requiredIfNotEmpty = requiredIfNotEmpty
            return this
        }

        fun minLength(length: Int): Builder {
            this.minLength = length
            return this
        }

        fun maxLength(length: Int): Builder {
            this.maxLength = length
            return this
        }


        fun minRange(length: Double): Builder {
            this.minRange = length
            return this
        }

        fun maxRange(length: Double): Builder {
            this.maxRange = length
            return this
        }

        fun pattern(pattern: Regex): Builder {
            this.pattern = pattern
            return this
        }

        fun compareFor(compareFor: String): Builder {
            this.compareFor = compareFor
            return this
        }

        fun compareWith(compareWith: String): Builder {
            this.compareWith = compareWith
            return this
        }

        fun custom(validator: CustomValidation): Builder {
            this.custom = validator
            return this
        }


        fun build(context: Context): ValidationRules {
            val mappedLabel = label.replace(" *", "")
            return ValidationRules(
                required = required(context, mappedLabel, required, requiredIfNotEmpty),
                length = length(context, mappedLabel, minLength, maxLength),
                range = range(context, mappedLabel, minRange, maxRange),
                regex = pattern(context, mappedLabel, pattern),
                compareFor = compareFor?.let { CompareForValidation(it) },
                compareWith = compareWith?.let {
                    CompareWithValidation(
                        it,
                        "${it.capitalize(Locale.getDefault())} did not match with $mappedLabel"
                    )
                },
                custom = if (custom == null) null else custom
            )
        }


        private fun getMessage(context: Context, type: Message, vararg args: String?): String {
            return context.getString(type.idRes, *args)
        }

        private fun required(
            context: Context,
            fieldName: String,
            required: Boolean?,
            requiredIfNotEmpty: Array<String>? = null,
        ): RequiredValidation? {
            if (required == null || (requiredIfNotEmpty.isNullOrEmpty() && required == false)) return null
            return RequiredValidation(getMessage(context, Message.REQUIRED, fieldName), requiredIfNotEmpty)
        }

        private fun length(
            context: Context,
            fieldName: String,
            minLength: Int?,
            maxLength: Int?
        ): LengthValidation? {
            if (minLength == null && maxLength == null) return null


            val localeMax = numberFormatter.format(maxLength?.toLong() ?: 0)
            val localeMin = numberFormatter.format(minLength?.toLong() ?: 0)

            val message =
                when {
                    minLength == maxLength -> getMessage(
                        context,
                        Message.LENGTH,
                        fieldName,
                        localeMax
                    )
                    minLength == null -> getMessage(
                        context,
                        Message.MAX_LENGTH,
                        fieldName,
                        localeMax
                    )
                    maxLength == null -> getMessage(
                        context,
                        Message.MIN_LENGTH,
                        fieldName,
                        localeMin
                    )
                    else -> getMessage(
                        context,
                        Message.MIN_MAX_LENGTH,
                        fieldName,
                        localeMin,
                        localeMax
                    )
                }

            return LengthValidation(min = minLength ?: 0, max = maxLength, message = message)
        }

        private fun range(
            context: Context,
            fieldName: String,
            minLength: Double?,
            maxLength: Double?
        ): RangeValidation? {
            if (minLength == null && maxLength == null) return null

            val localeMax = numberFormatter.format(maxLength ?: 0)
            val localeMin = numberFormatter.format(minLength ?: 0)

            val message = when {
                maxLength == minLength -> getMessage(
                    context, Message.RANGE, fieldName, localeMax
                )
                minLength == null -> getMessage(
                    context, Message.MAX_RANGE, fieldName, localeMax
                )
                maxLength == null -> getMessage(
                    context, Message.MIN_RANGE, fieldName, localeMin
                )
                else -> getMessage(
                    context, Message.MIN_MAX_RANGE, fieldName, localeMin, localeMax
                )
            }

            return RangeValidation(
                min = minLength ?: 0.0,
                max = maxLength,
                message = message
            )
        }

        private fun pattern(
            context: Context,
            fieldName: String,
            pattern: Regex?
        ): RegexValidation? {
            if (pattern == null) return null
            return RegexValidation(
                pattern, getMessage(
                    context, Message.INVALID, fieldName
                )
            )
        }

    }

}
