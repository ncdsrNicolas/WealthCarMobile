package br.edu.fatecpg.wealthcar_teste

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


/* ─── Modelo ─────────────────────────────────────────────────────── */
@Serializable
data class VeiculoInsert(
    val id_usuario:          String,
    val nome_apelido:        String,
    val marca:               String,
    val modelo:              String,
    val ano:                 Int,
    val placa:               String,
    val combustivel:         String,
    val quilometragem_atual: Int,
)

/* ─── Dados de marca/modelo ──────────────────────────────────────── */
val MARCAS_MODELOS = mapOf(
    "Chevrolet"    to listOf("Onix","Onix Plus","Tracker","S10","Cruze","Spin","Montana","Equinox","Camaro"),
    "Fiat"         to listOf("Argo","Cronos","Mobi","Pulse","Fastback","Strada","Toro","Uno","Palio"),
    "Ford"         to listOf("Ka","EcoSport","Territory","Ranger","Maverick","Mustang"),
    "Honda"        to listOf("Civic","City","City Hatchback","HR-V","CR-V","WR-V","Fit"),
    "Hyundai"      to listOf("HB20","HB20S","Creta","Tucson","Santa Fe"),
    "Jeep"         to listOf("Renegade","Compass","Commander","Wrangler","Gladiator"),
    "Kia"          to listOf("Stonic","Sportage","Sorento","Carnival","EV6"),
    "Mitsubishi"   to listOf("Lancer","Lancer Evolution","Outlander","Eclipse Cross","ASX","Pajero","L200"),
    "Nissan"       to listOf("Kicks","Versa","Sentra","Frontier"),
    "Renault"      to listOf("Kwid","Sandero","Logan","Duster","Captur","Oroch"),
    "Toyota"       to listOf("Corolla","Corolla Cross","Yaris","Hilux","SW4","RAV4","Prius"),
    "Volkswagen"   to listOf("Gol","Polo","Virtus","Nivus","T-Cross","Taos","Tiguan","Amarok","Saveiro"),
    "BYD"          to listOf("Dolphin","Seal","Tan","Han","Atto 3"),
    "BMW"          to listOf("Série 1","Série 3","Série 5","X1","X3","X5"),
    "Audi"         to listOf("A3","A4","A5","Q3","Q5","Q7"),
    "Outro"        to listOf("Outro"),
)

val MARCAS = MARCAS_MODELOS.keys.sorted()
val COMBUSTIVEIS = listOf("Flex","Gasolina","Etanol","Diesel","Elétrico","Híbrido")

class VehicleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle)

        val etApelido        = findViewById<TextInputEditText>(R.id.etVehicleApelido)
        val spinnerMarca     = findViewById<Spinner>(R.id.spinnerMarca)
        val spinnerModelo    = findViewById<Spinner>(R.id.spinnerModelo)
        val etAno            = findViewById<TextInputEditText>(R.id.etVehicleAno)
        val etPlaca          = findViewById<TextInputEditText>(R.id.etVehiclePlaca)
        val etKm             = findViewById<TextInputEditText>(R.id.etVehicleQuilometragem)
        val spinnerCombust   = findViewById<Spinner>(R.id.spinnerCombustivel)
        val btnSalvar        = findViewById<Button>(R.id.btnSalvarVeiculo)

        /* ── Spinner Marca ── */
        spinnerMarca.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Selecione a marca") + MARCAS
        )

        /* ── Spinner Modelo — atualiza quando marca muda ── */
        spinnerMarca.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                val marca = if (pos == 0) null else MARCAS[pos - 1]
                val modelos = if (marca != null) MARCAS_MODELOS[marca] ?: emptyList() else emptyList()
                spinnerModelo.adapter = ArrayAdapter(
                    this@VehicleActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    if (modelos.isEmpty()) listOf("Selecione a marca primeiro")
                    else listOf("Selecione o modelo") + modelos
                )
                spinnerModelo.isEnabled = modelos.isNotEmpty()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        /* ── Spinner Combustível ── */
        spinnerCombust.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, COMBUSTIVEIS
        )

        /* ── Placa — somente maiúsculas e números ── */
        etPlaca.filters = arrayOf(
            InputFilter.AllCaps(),
            InputFilter { source, _, _, _, _, _ ->
                source.filter { it.isLetterOrDigit() }
            },
            InputFilter.LengthFilter(8)
        )

        /* ── Quilometragem — formatação com pontos ── */
        etKm.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                isFormatting = true
                val digits = s.toString().replace(".", "")
                val formatted = digits.reversed().chunked(3).joinToString(".").reversed()
                etKm.setText(formatted)
                etKm.setSelection(formatted.length)
                isFormatting = false
            }
        })

        /* ── Salvar ── */
        btnSalvar.setOnClickListener {
            val apelido  = etApelido.text.toString().trim()
            val marcaPos = spinnerMarca.selectedItemPosition
            val marca    = if (marcaPos == 0) "" else MARCAS[marcaPos - 1]
            val modeloPos = spinnerModelo.selectedItemPosition
            val modelos  = if (marca.isNotEmpty()) MARCAS_MODELOS[marca] ?: emptyList() else emptyList()
            val modelo   = if (modeloPos == 0 || modelos.isEmpty()) "" else modelos[modeloPos - 1]
            val ano      = etAno.text.toString().trim()
            val placa    = etPlaca.text.toString().trim()
            val km       = etKm.text.toString().replace(".", "").toIntOrNull() ?: 0
            val combust  = spinnerCombust.selectedItem.toString()

            if (marca.isEmpty() || modelo.isEmpty() || ano.isEmpty() || placa.isEmpty()) {
                showCustomToast("Preencha marca, modelo, ano e placa")
                return@setOnClickListener
            }
            if (ano.length != 4 || ano.toIntOrNull() == null) {
                showCustomToast("Ano inválido")
                return@setOnClickListener
            }
            if (placa.length < 7) {
                showCustomToast("Placa inválida — mínimo 7 caracteres")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val user = SupabaseClient.client.auth.currentUserOrNull()
                        ?: run { startActivity(Intent(this@VehicleActivity, LoginActivity::class.java)); finish(); return@launch }

                    SupabaseClient.client.postgrest["veiculo"].insert(
                        VeiculoInsert(
                            id_usuario          = user.id,
                            nome_apelido        = apelido.ifEmpty { "$marca $modelo" },
                            marca               = marca,
                            modelo              = modelo,
                            ano                 = ano.toInt(),
                            placa               = placa,
                            combustivel         = combust,
                            quilometragem_atual = km,
                        )
                    )
                    showCustomToast("Veículo cadastrado com sucesso!")
                    startActivity(Intent(this@VehicleActivity, HomeActivity::class.java)
                        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                    finish()
                } catch (e: Exception) {
                    android.util.Log.e("WealthCar", "Erro ao salvar", e)
                    showCustomToast("Erro: ${e.message}")
                }
            }
        }
    }
}