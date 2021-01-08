package com.maddy.formvalidator

import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Validate the editText
 */
fun EditText.hasValue(): Boolean {
    return this.text.isNotEmpty()
}

/**
 * get the EditText value
 */
fun EditText.value(): String? {
    return if (this.hasValue()) this.text.toString()
    else null
}

fun EditText.register(
    form: Form,
    name: String,
    rules: ValidationRules,
    setError: ((String?) -> Unit)? = null
) {
    form.registerInput(
        name = name,
        rules = rules,
        setError = setError ?: { this.error = it },
        setValue = { this.setText(it) },
        valueProvider = { this.value() },
        setChangeListener = {
            if (form.validateOnChange) {
                val watcher = this.addTextChangedListener(afterTextChanged = {
                    form.validateField(
                        name
                    )
                })
                return@registerInput { this.removeTextChangedListener(watcher) }
            }
            return@registerInput {}
        },
        setFocusListener = {
            this.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) form.validateField(
                    name
                )
            }
            return@registerInput {}
        }
    )
}

fun Spinner.register(
    form: Form,
    name: String,
    rules: ValidationRules,
    getValue: () -> String?,
    setValue: (String?) -> Unit = {},
    setError: (String?) -> Unit = {}
) {
    val count = AtomicInteger(0)
    form.registerInput(
        name = name,
        rules = rules,
        setError = setError,
        setValue = setValue,
        valueProvider = getValue,
        setChangeListener = {
            if (form.validateOnChange) {
                this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        form.validateField(name)
                    }

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        //#FIXME: onItemSelection will invoke on first lunch, so it will tries to validate without user interaction
                        if (count.getAndAdd(1) < 1) return

                        form.validateField(name)
                    }
                }
                return@registerInput { this.onItemSelectedListener = null }
            }

            return@registerInput {}
        },
        setFocusListener = {
            this.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) form.validateField(name)
            }
            return@registerInput {}
        }
    )
}


fun TextInputLayout.register(form: Form, name: String, rules: ValidationRules) {
    form.registerInput(
        name = name,
        rules = rules,
        setError = {
            Log.d("Form", "TextInputLayout.register(), setError(${it})")
            this.error = it
        },
        setValue = { this.editText?.setText(it) },
        valueProvider = { this.editText?.value() },
        setChangeListener = {
            val watcher = this.editText?.addTextChangedListener(afterTextChanged = {
                form.validateField(name)
            })
            return@registerInput { this.editText?.removeTextChangedListener(watcher) }
        },
        setFocusListener = {
            this.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) form.validateField(name)
            }

            return@registerInput {}
        }
    )
}

fun RadioGroup.register(
    form: Form,
    name: String,
    rules: ValidationRules,
    setError: (String?) -> Unit = {},
    setValue: (String?) -> Unit = {},
    extractValue: ((checkedId: Int) -> String?)? = null,
    onValueChange: (checkedId: Int) -> Unit = {}
) {

    val getValue = extractValue ?: { checkedId ->
        val button = this.findViewById<RadioButton>(checkedId)
        (button?.tag ?: button?.text)?.toString()?.toLowerCase()
    }
    form.registerInput(
        name = name,
        rules = rules,
        setError = setError,
        setValue = setValue,
        valueProvider = { getValue(this.checkedRadioButtonId) },
        setChangeListener = {
            this.setOnCheckedChangeListener { _, checkedId ->
                form.validateField(name)
                onValueChange(checkedId)
            }
            return@registerInput { this.setOnCheckedChangeListener(null) }
        },
        setFocusListener = {
            this.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) form.validateField(name)
            }
            return@registerInput {}
        }
    )
}

fun <I> Collection<I>.every(predicate: (I) -> Boolean): Boolean {
    val isMatched = AtomicBoolean(true)
    this.forEach {
        if (!predicate(it)) {
            isMatched.set(false)
        }
    }
    return isMatched.get()
}