package com.maddy.formvalidator


import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Madhusudan Sapkota on 8/30/20.
 */
class Form(val validateOnChange: Boolean = true) {

    private val inputs: HashMap<String, FormInput> = HashMap()
    private val rules: HashMap<String, ValidationRules> = HashMap()
    private val isTouched = HashMap<String, Boolean>()

    private val valueChangeUnsubscribe = HashMap<String, Unsubscribe>()
    private val focusChangeUnsubscribe = HashMap<String, Unsubscribe>()

    fun registerInput(
        name: String,
        rules: ValidationRules,
        valueProvider: () -> String?,
        setError: (String?) -> Unit = {},
        setValue: (String?) -> Unit = {},
        setChangeListener: (listener: OnChangeListener) -> Unsubscribe = { {} },
        setFocusListener: (listener: OnFocusListener) -> Unsubscribe = { {} }
    ) {
        val input = FormInput(
            valueProvider,
            setError,
            setValue,

            { listener -> setFocusListener(listener) },
            { listener -> setChangeListener(listener) }
        )
        this.inputs[name] = input
        this.rules[name] = rules

        if (validateOnChange) {
            val unsubscribe = input.addChangeListener {
                if (!isTouched(name) && it.isNullOrEmpty()) return@addChangeListener

                isTouched[name] = true
                this.validateCustomField(name)
            }
            valueChangeUnsubscribe[name] = unsubscribe
        } else {
            val unsubscribe = input.addOnFocusListener { _, focus ->
                if (focus) return@addOnFocusListener
                this.validateCustomField(name)
            }
            focusChangeUnsubscribe[name] = unsubscribe
        }
    }

    fun unregisterInput(name: String) {
        this.valueChangeUnsubscribe[name]?.invoke()
        this.focusChangeUnsubscribe[name]?.invoke()
        this.valueChangeUnsubscribe.remove(name)
        this.focusChangeUnsubscribe.remove(name)

        this.inputs.remove(name)
    }

    fun values(): Map<String, String?> {
        return this.inputs.mapValues { it.value.valueProvider() }
    }

    fun value(name: String): String? {
        return this.inputs[name]?.valueProvider?.invoke()
    }

    fun setValues(values: Map<String, String?>) {
        this.inputs.forEach {
            it.value.setValue(values[it.key])
            it.value.setError(null)
        }
    }

    fun setError(name: String, errorMessage: String?) {
        this.inputs[name]?.setError?.invoke(errorMessage)
    }

    fun setValues(name: String, values: String?) {
        this.inputs[name]?.let {
            it.setValue(values)
        }
    }

    fun isTouched(name: String): Boolean {
        return isTouched[name] ?: false
    }

    fun isTouched(): Boolean {
        return isTouched.all { it.value }
    }

    fun reset() {
        this.inputs.forEach {
            isTouched[it.key] = false
            it.value.setValue(null)
            it.value.setError(null)
        }
    }

    fun reset(name: String) {
        this.isTouched[name] = false
        this.inputs[name]?.setValue?.invoke(null)
        this.inputs[name]?.setError?.invoke(null)
    }

    fun resetError() {
        this.inputs.forEach {
            it.value.setError(null)
        }
    }

    fun validate(): Boolean {
        val isValid = AtomicBoolean(true)
        this.inputs.keys.forEach {
            val isFieldValid = validateField(it)
            if (!isFieldValid) isValid.set(false)
        }
        this.inputs.keys.forEach {
            val isFieldValid = validateCustomField(it)
            if (!isFieldValid) isValid.set(false)
        }
        return isValid.get()
    }

    fun validateField(fieldName: String): Boolean {
        val field = this.inputs[fieldName] ?: return false

        val rule = this.rules[fieldName]

        val compareFieldValue =
            if (rule?.compareWith != null) this.inputs[rule.compareWith.compareWith]?.valueProvider?.invoke() else null

        val isValidField = validateCustomField(
            field,
            rule,
            compareFieldValue
        )

        // validate the field which it referenced to
        if (rule?.compareFor?.compareFor != null &&
            rule.compareFor.compareFor != fieldName &&
            this.inputs.containsKey(rule.compareFor.compareFor)
        ) {
            validateField(rule.compareFor.compareFor)
        }

        return isValidField
    }

    private fun validateCustomField(fieldName: String): Boolean {
        val field = this.inputs[fieldName] ?: return false

        val rule = this.rules[fieldName]

        val compareField =
            if (rule?.compareWith != null) this.inputs[rule.compareWith.compareWith] else null

        val isValidField = validateCustomField(
            field,
            rule,
            compareField?.valueProvider?.invoke()
        )

        // validate the field which it referenced to
        if (rule?.compareFor?.compareFor != null && rule.compareFor.compareFor != fieldName) {
            validateCustomField(rule.compareFor.compareFor)
        }

        return isValidField
    }

    private fun validateCustomField(
        field: FormInput,
        rules: ValidationRules?,
        compareWithValue: String? = null
    ): Boolean {
        val currentValue = field.valueProvider()

        var error = Validator.validate(currentValue, rules)
        field.setError(error)

        if (error == null && rules?.compareWith != null) {
            error = if (currentValue != compareWithValue) rules.compareWith.message else null
            field.setError(error)
        }

        return error == null
    }
}
typealias Unsubscribe = () -> Unit
typealias OnFocusListener = (String?, Boolean) -> Unit
typealias OnChangeListener = (String?) -> Unit

open class FormInput(
    val valueProvider: () -> String?,
    val setError: (String?) -> Unit,
    val setValue: (String?) -> Unit,
    val addOnFocusListener: (listener: OnFocusListener) -> Unsubscribe = { {} },
    val addChangeListener: (listener: OnChangeListener) -> Unsubscribe = { {} }
)
