package com.maddy.formvalidator

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

/**
 * Created by Madhusudan Sapkota on 8/30/20.
 */
class FormValidator(val validateOnChange: Boolean = true) {

    private val inputs: LinkedHashMap<String, FormInput> = LinkedHashMap()
    private val rules: LinkedHashMap<String, ValidationRules> = LinkedHashMap()
    private val isTouched = HashMap<String, Boolean>()

    private val valueChangeUnsubscribers = HashMap<String, Unsubscribe>()
    private val focusChangeUnsubscribers = HashMap<String, Unsubscribe>()

    private val changeListeners = HashMap<String, Subscribe?>()

    fun registerInput(
        name: String,
        rules: ValidationRules,
        valueProvider: () -> Any?,
        setError: (String?) -> Unit = {},
        setValue: (FormValue<Any>) -> Unit = {},
        setChangeListener: (listener: OnChangeListener) -> Unsubscribe = { {} },
        setFocusListener: (listener: OnFocusListener) -> Unsubscribe = { {} }
    ) {
        val input = object : FormInput(valueProvider, setError, setValue) {
            override fun setOnChangeListener(listener: OnChangeListener): Unsubscribe {
                return setChangeListener(listener)
            }

            override fun setOnFocusListener(listener: OnFocusListener): Unsubscribe {
                return setFocusListener(listener)
            }
        }
        this.inputs[name] = input
        this.rules[name] = rules

        if (validateOnChange) {
            val unsubscribe = input.setOnChangeListener {
                if (!isTouched(name) && Validator.isEmpty(value(name).value)) return@setOnChangeListener

                isTouched[name] = true
                this.validateField(name)

                changeListeners[name]?.invoke(FormValue(it))
            }
            valueChangeUnsubscribers[name] = unsubscribe
        } else {
            val unsubscribe = input.setOnFocusListener { _, focus ->
                if (focus) return@setOnFocusListener
                this.validateField(name)
            }
            focusChangeUnsubscribers[name] = unsubscribe
        }
    }

    fun setOnChangeListener(name: String, onChange: Subscribe?): Unsubscribe {
        changeListeners[name] = onChange
        return { changeListeners.remove(name) }
    }

    fun unregisterInput(name: String) {
        this.inputs.remove(name)
        this.valueChangeUnsubscribers[name]?.invoke()
        this.focusChangeUnsubscribers[name]?.invoke()
        this.valueChangeUnsubscribers.remove(name)
        this.focusChangeUnsubscribers.remove(name)

    }

    fun values(): Map<String, FormValue<Any>?> {
        return this.inputs.mapValues { FormValue(it.value.valueProvider()) }
    }

    fun value(name: String): FormValue<Any> {
        return FormValue(this.inputs[name]?.valueProvider?.invoke())
    }

    fun setValues(values: Map<String, Any?>) {
        this.inputs.forEach {
            it.value.setValue(FormValue(values[it.key]))
            it.value.setError(null)
        }
    }

    fun setError(name: String, errorMessage: String?) {
        this.inputs[name]?.setError?.invoke(errorMessage)
    }

    fun setErrors(errors: Map<String, String?>) {
        errors.forEach {
            setError(it.key, it.value)
        }
    }

    fun setValues(name: String, values: Any?) {
        this.inputs[name]?.let {
            it.setValue(FormValue(values))
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
            it.value.setValue(FormValue())
            it.value.setError(null)
        }
    }

    fun reset(name: String) {
        this.isTouched[name] = false
        this.inputs[name]?.setValue?.invoke(FormValue<Any>())
        this.inputs[name]?.setError?.invoke(null)
    }

    fun resetError() {
        this.inputs.forEach {
            it.value.setError(null)
        }
    }

    fun resetError(name: String) {
        this.inputs[name]?.setError?.invoke(null)
    }

    fun clear() {
        this.focusChangeUnsubscribers.values.forEach { it.invoke() }
        this.valueChangeUnsubscribers.values.forEach { it.invoke() }

        this.rules.clear()
        this.inputs.clear()
        this.isTouched.clear()
        this.focusChangeUnsubscribers.clear()
        this.valueChangeUnsubscribers.clear()
    }

    fun validate(): Boolean {
        val isValid = AtomicBoolean(true)
        resetError()
        this.inputs.keys.forEach {
            val isFieldValid = validateField(it)
            if (!isFieldValid) isValid.set(false)
        }
        return isValid.get()
    }

    fun validateField(fieldName: String): Boolean {
        val field = this.inputs[fieldName] ?: return false

        val rule0 = this.rules[fieldName]

        val required: Boolean? =
            rule0?.required?.requiredIfNotEmpty?.any { !Validator.isEmpty(value(it).value) }
        val rule = if (required == true) {
            ValidationRules(
                required = RequiredValidation(rule0.required.message),
                length = rule0.length,
                range = rule0.range,
                regex = rule0.regex,
                compareFor = rule0.compareFor,
                compareWith = rule0.compareWith,
                custom = rule0.custom
            )
        } else {
            rule0
        }

        val compareField =
            if (rule?.compareWith != null) this.inputs[rule.compareWith.compareWith] else null

        val isValidField = validateCustomField(
            field,
            rule,
            compareField?.valueProvider?.invoke()
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

    private fun validateCustomField(
        field: FormInput,
        rules: ValidationRules?,
        compareWithValue: Any? = null
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

    fun updateRules(name: String, rule: ValidationRules) {
        this.rules[name] = rule
        this.validateField(name)
    }
}
