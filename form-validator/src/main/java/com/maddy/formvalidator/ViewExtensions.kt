package com.maddy.formvalidator

import android.view.View
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import java.util.*

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
    formValidator: FormValidator,
    name: String,
    rules: ValidationRules,
    setError: (String?) -> Unit = {}
) {
    formValidator.registerInput(
        name = name,
        rules = rules,
        setError = setError,
        setValue = { this.setText(it.toString()) },
        valueProvider = { this.value() },
        setChangeListener = { listener ->
            val watcher = this.addTextChangedListener(afterTextChanged = {
                listener(it?.toString())
            })
            return@registerInput { this.removeTextChangedListener(watcher) }
        },
        setFocusListener = { listener ->
            this.setOnFocusChangeListener { _, hasFocus ->
                listener(value(), hasFocus)
            }
            return@registerInput {}
        }
    )
}

fun Spinner.register(
    formValidator: FormValidator,
    name: String,
    rules: ValidationRules,
    getValue: () -> Any?,
    setValue: (Any?) -> Unit = {},
    setError: (String?) -> Unit = {}
) {
    formValidator.registerInput(
        name = name,
        rules = rules,
        setError = setError,
        setValue = setValue,
        valueProvider = getValue,
        setChangeListener = { listener ->
            if (formValidator.validateOnChange) {
                this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        listener.invoke(null)
                    }

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        listener.invoke(getValue())
                    }
                }
                return@registerInput { this.onItemSelectedListener = null }
            }

            return@registerInput {}
        },
        setFocusListener = { listener ->
            this.setOnFocusChangeListener { _, hasFocus ->
                listener.invoke(getValue(), hasFocus)
            }
            return@registerInput {}
        }
    )
}


fun TextInputLayout.register(
    formValidator: FormValidator,
    name: String,
    rules: ValidationRules,
    getValue: (() -> Any?)? = null,
    setValue: ((Any?) -> Unit)? = null
) {
    formValidator.registerInput(
        name = name,
        rules = rules,
        setError = { this.error = it },
        setValue = setValue ?: {
            this.editText?.setText(it?.toString())
            Unit
        },
        valueProvider = getValue ?: { this.editText?.value() },
        setChangeListener = { listener ->
            val watcher = this.editText?.addTextChangedListener(afterTextChanged = { editable ->
                listener.invoke(editable?.toString())
            })
            return@registerInput { this.editText?.removeTextChangedListener(watcher) }
        },
        setFocusListener = { listener ->
            this.setOnFocusChangeListener { _, hasFocus ->
                listener.invoke(editText?.value(), hasFocus)
            }
            return@registerInput {}
        }
    )
}

fun RadioGroup.register(
    formValidator: FormValidator,
    name: String,
    rules: ValidationRules,
    setError: (String?) -> Unit = {},
    setValue: (Any?) -> Unit = {},
    extractValue: ((checkedId: Int) -> String?)? = null,
    onValueChange: (checkedId: Int) -> Unit = {}
) {

    val getValue = extractValue ?: { checkedId ->
        val button = this.findViewById<RadioButton>(checkedId)
        (button?.tag ?: button?.text)?.toString()?.toLowerCase(Locale.getDefault())
    }
    formValidator.registerInput(
        name = name,
        rules = rules,
        setError = setError,
        setValue = setValue,
        valueProvider = { getValue(this.checkedRadioButtonId) },
        setChangeListener = { listener ->
            this.setOnCheckedChangeListener { _, checkedId ->
                listener(getValue(checkedId))
                onValueChange(checkedId)
            }
            return@registerInput { this.setOnCheckedChangeListener(null) }
        },
        setFocusListener = { listener ->
            this.setOnFocusChangeListener { _, hasFocus ->
                listener(getValue(this.checkedRadioButtonId), hasFocus)
            }
            return@registerInput {}
        }
    )
}
