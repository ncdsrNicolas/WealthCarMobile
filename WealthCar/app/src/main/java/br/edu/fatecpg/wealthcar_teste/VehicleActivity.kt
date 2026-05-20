package br.edu.fatecpg.wealthcar_teste

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/* ─── Modelo do veículo ───────────────────────────────────────────── */
@Serializable
data class VeiculoInsert(
    val id_usuario:          String,
    val nome_apelido:        String,
    val marca:               String,
    val modelo:              String,
    val ano:                 Int,
    val placa:               String,
    val combustivel:         String,
    val quilometragem_atual: Int
)

class VehicleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle)

        /* ── Views ── */
        val etApelido      = findViewById<TextInputEditText>(R.id.etVehicleApelido)
        val etMarca        = findViewById<TextInputEditText>(R.id.etVehicleMarca)
        val etModelo       = findViewById<TextInputEditText>(R.id.etVehicleModelo)
        val etAno          = findViewById<TextInputEditText>(R.id.etVehicleAno)
        val etPlaca        = findViewById<TextInputEditText>(R.id.etVehiclePlaca)
        val etQuilometragem = findViewById<TextInputEditText>(R.id.etVehicleQuilometragem)
        val spinnerCombustivel = findViewById<Spinner>(R.id.spinnerCombustivel)
        val btnSalvar      = findViewById<Button>(R.id.btnSalvarVeiculo)

        /* ── Spinner de combustível ── */
        val combustiveis = listOf("Flex", "Gasolina", "Etanol", "Diesel", "Elétrico", "Híbrido")
        spinnerCombustivel.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            combustiveis
        )

        /* ── Salvar veículo ── */
        btnSalvar.setOnClickListener {
            val apelido       = etApelido.text.toString().trim()
            val marca         = etMarca.text.toString().trim()
            val modelo        = etModelo.text.toString().trim()
            val ano           = etAno.text.toString().trim()
            val placa         = etPlaca.text.toString().trim().uppercase()
            val quilometragem = etQuilometragem.text.toString().trim()
            val combustivel   = spinnerCombustivel.selectedItem.toString()

            /* Validações */
            /* Validações */
            if (marca.isEmpty() || modelo.isEmpty() || ano.isEmpty() || placa.isEmpty()) {
                showCustomToast("Preencha ao menos marca, modelo, ano e placa")
                return@setOnClickListener
            }

            // Validação de Ano
            val anoInt = ano.toIntOrNull()
            val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) // Pega o ano atual automaticamente

            if (anoInt == null || ano.length != 4) {
                showCustomToast("Ano inválido")
                return@setOnClickListener
            }
            if (anoInt > anoAtual) {
                showCustomToast("O ano do veículo não pode ser superior a $anoAtual")
                return@setOnClickListener
            }

            // Validação de Quilometragem
            val kmInt = quilometragem.toIntOrNull()
            if (kmInt != null && kmInt < 0) {
                showCustomToast("O odômetro não pode ser negativo")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    /* Pega o usuário logado */
                    val user = SupabaseClient.client.auth.currentUserOrNull()
                        ?: run {
                            showCustomToast("Sessão expirada. Faça login novamente.")
                            startActivity(Intent(this@VehicleActivity, LoginActivity::class.java))
                            finish()
                            return@launch
                        }

                    val veiculo = VeiculoInsert(
                        id_usuario          = user.id,
                        nome_apelido        = apelido.ifEmpty { "$marca $modelo" },
                        marca               = marca,
                        modelo              = modelo,
                        ano                 = ano.toInt(),
                        placa               = placa,
                        combustivel         = combustivel,
                        quilometragem_atual = kmInt ?: 0
                    )

                    /* Insere no banco */
                    SupabaseClient.client.postgrest["veiculo"].insert(veiculo)

                    showCustomToast("Veículo cadastrado com sucesso!")

                    /* Vai para a Home limpando a pilha */
                    val intent = Intent(this@VehicleActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    android.util.Log.e("WealthCar", "Erro ao salvar veículo", e)
                    showCustomToast("Erro ao salvar: ${e.message}")
                }
            }
        }
    }
}