package com.maddy.formexample.ui.login

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.maddy.formexample.R
import com.maddy.formvalidator.Form
import com.maddy.formvalidator.Rules
import com.maddy.formvalidator.register

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    private val loginForm = Form()

    private lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        val passwordTIL = findViewById<TextInputLayout>(R.id.passwordTIL)
        val genderSpinner = findViewById<Spinner>(R.id.genderSpinner)
        val genderErrorText = findViewById<TextView>(R.id.genderError)
        val login = findViewById<Button>(R.id.login)
        loading = findViewById(R.id.loading)

        // Register Input Fields
        username.register(
            loginForm,
            "email",
            Rules.Builder(getString(R.string.prompt_email))
                .required()
                .email()
                .build(this)
        )

        passwordTIL.register(
            loginForm,
            "password",
            Rules.Builder(getString(R.string.prompt_password))
                .required()
                .minLength(4)
                .maxLength(8)
                .build(this)
        )

        val genders = resources.getStringArray(R.array.gender)
        genderSpinner.register(
            loginForm,
            "gender",
            Rules.Builder(getString(R.string.prompt_gender))
                .required()
                .build(this),
            getValue = {
                val gender = genderSpinner.selectedItem.toString()
                if (gender == genders[0]) null
                else gender
            },
            setValue = {
                genderSpinner.setSelection(genders.indexOf(it))
            },
            setError = { genderErrorText.text = it }
        )

        loginViewModel =
            ViewModelProvider(this, LoginViewModelFactory()).get(LoginViewModel::class.java)

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        password.apply {

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        login()
                }
                false
            }

            login.setOnClickListener {
                login()
            }
        }
    }

    private fun login() {
        if (loginForm.validate()) {
            loading.visibility = View.VISIBLE
            loginViewModel.login(loginForm.value("email"), loginForm.value("password"))
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}