package br.edu.fatecpg.wealthcar_teste

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvIrCadastro = findViewById<TextView>(R.id.tvIrCadastro)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showCustomToast("Preencha e-mail e senha")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showCustomToast("Digite um e-mail válido")
                return@setOnClickListener
            }

            // INÍCIO DA INTEGRAÇÃO COM SUPABASE
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    // Verifica se já tem veículo cadastrado
                    val user = SupabaseClient.client.auth.currentUserOrNull()!!
                    val resultado = SupabaseClient.client
                        .postgrest["veiculo"]
                        .select {
                            filter { eq("id_usuario", user.id) }
                        }
                        .decodeList<kotlinx.serialization.json.JsonObject>()

                    showCustomToast("Login realizado com sucesso!")

                    // Primeira vez → VehicleActivity | Já tem → HomeActivity
                    val destino = if (resultado.isEmpty()) {
                        VehicleActivity::class.java
                    } else {
                        HomeActivity::class.java
                    }

                    val intent = Intent(this@LoginActivity, destino)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    android.util.Log.e("WealthCar", "Erro no login", e)
                    showCustomToast("Email ou senha incorretos")
                }
            }
        }
        tvIrCadastro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}