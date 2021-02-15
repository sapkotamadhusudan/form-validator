package com.maddy.form.builder

import android.view.View
import com.maddy.formvalidator.ValidationRules

enum class FormFieldType {
    TEXT_INPUT,

    AUTOCOMPLETE,

    SELECTION,

    RADIO_GROUP,

    CHECKBOX,
    MULTI_CHECKBOX
}

abstract class FormField<T>(
    val name: String,
    val rules: ValidationRules
) {

    var value: T? = null
        protected set

    val defaultValue: T? = null


    abstract val type: FormFieldType

    abstract fun validate(): Boolean

    abstract fun buildView(): View
}