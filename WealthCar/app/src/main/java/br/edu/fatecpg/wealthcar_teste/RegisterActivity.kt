package br.edu.fatecpg.wealthcar_teste

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<TextInputEditText>(R.id.etRegName)
        val etEmail = findViewById<TextInputEditText>(R.id.etRegEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etRegConfirmPassword)
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)
        val tvVoltarLogin = findViewById<TextView>(R.id.tvVoltarLogin)

        btnCadastrar.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showCustomToast("Digite um e-mail válido")
                    return@setOnClickListener
                }

                if (password == confirmPassword) {
                    val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userName", name)
                    editor.putString("email", email)
                    editor.putString("password", password)
                    editor.apply()

                    showCustomToast("Cadastro realizado com sucesso!")
                    finish()
                } else {
                    showCustomToast("As senhas não coincidem")
                }
            } else {
                showCustomToast("Preencha todos os campos")
            }
        }

        tvVoltarLogin.setOnClickListener {
            finish()
        }
    }
}