package br.edu.fatecpg.wealthcar_teste

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName            = findViewById<TextInputEditText>(R.id.etRegName)
        val etEmail           = findViewById<TextInputEditText>(R.id.etRegEmail)
        val etPassword        = findViewById<TextInputEditText>(R.id.etRegPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etRegConfirmPassword)
        val btnCadastrar      = findViewById<Button>(R.id.btnCadastrar)
        val tvVoltarLogin     = findViewById<TextView>(R.id.tvVoltarLogin)

        btnCadastrar.setOnClickListener {
            val name            = etName.text.toString().trim()
            val email           = etEmail.text.toString().trim()
            val password        = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validações
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showCustomToast("Preencha todos os campos")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showCustomToast("Digite um e-mail válido")
                return@setOnClickListener
            }
            if (password.length < 8) {
                showCustomToast("A senha deve ter pelo menos 8 caracteres")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showCustomToast("As senhas não coincidem")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email    = email
                        this.password = password
                        // Envia o username para o trigger criar o perfil na tabela usuario
                        data = buildJsonObject {
                            put("username", name)
                        }
                    }

                    // Supabase envia email de confirmação automaticamente
                    showCustomToast("Cadastro realizado! Verifique seu e-mail para confirmar a conta.")
                    finish()

                } catch (e: Exception) {
                    android.util.Log.e("WealthCar", "Erro no cadastro", e)
                    showCustomToast("Falha no cadastro: ${e.message}")
                }
            }
        }

        tvVoltarLogin.setOnClickListener { finish() }
    }
}