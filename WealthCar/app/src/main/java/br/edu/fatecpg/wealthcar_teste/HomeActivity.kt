package br.edu.fatecpg.wealthcar_teste

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// 1. Criamos a planta do veículo com os tipos corretos (Int e String)
@Serializable
data class VeiculoResponse(
    val nome_apelido: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val placa: String? = null,
    val quilometragem_atual: Int? = 0,
    val ano: Int? = null,
    val combustivel: String? = null
)

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvVehicleName = findViewById<TextView>(R.id.tv_vehicle_name)
        val tvPlateValue = findViewById<TextView>(R.id.tv_plate_value)
        val tvOdometerValue = findViewById<TextView>(R.id.tv_odometer_value)
        val tvFuelValue = findViewById<TextView>(R.id.tv_fuel_value)
        val tvVehicleDetails = findViewById<TextView>(R.id.tv_vehicle_details)

        val btnPerfil = findViewById<View>(R.id.card_profile)

        btnPerfil.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Encerra a sessão ativa no Supabase
                    SupabaseClient.client.auth.signOut()

                    val intent = Intent(this@HomeActivity, LoginActivity::class.java)

                    // Limpa toda a pilha de telas anteriores (impede o botão "Voltar")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    android.util.Log.e("WealthCar", "Erro ao fazer logout", e)
                    showCustomToast("Erro ao sair da conta.")
                }
            }
        }

        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val user = SupabaseClient.client.auth.currentUserOrNull()

                if (session == null || user == null) {
                    val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    return@launch
                }

                // 2. Trocamos o Map<String, String> pelo VeiculoResponse
                val resultado = SupabaseClient.client
                    .postgrest["veiculo"]
                    .select {
                        filter { eq("id_usuario", user.id) }
                    }
                    .decodeList<VeiculoResponse>()

                // 3. Preenchemos a tela com segurança
                if (resultado.isNotEmpty()) {
                    val veiculo = resultado[0]

                    val apelido = veiculo.nome_apelido
                    val marca = veiculo.marca ?: ""
                    val modelo = veiculo.modelo ?: ""
                    val ano = veiculo.ano?.toString() ?: ""

                    tvVehicleName.text = if (!apelido.isNullOrBlank()) apelido else "$marca $modelo"

                    tvVehicleDetails.text = "$marca $modelo • $ano".trim()

                    tvPlateValue.text = veiculo.placa ?: "---"
                    tvOdometerValue.text = "${veiculo.quilometragem_atual ?: 0} km"
                    tvFuelValue.text = veiculo.combustivel ?: "-"
                }

            } catch (e: Exception) {
                android.util.Log.e("WealthCar", "Erro ao carregar dados do carro", e)
                showCustomToast("Não foi possível carregar os dados do veículo.")
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }
}