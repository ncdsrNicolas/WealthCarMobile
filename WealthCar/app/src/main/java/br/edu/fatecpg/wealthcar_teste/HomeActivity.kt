package br.edu.fatecpg.wealthcar_teste

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import br.edu.fatecpg.wealthcar_teste.CarImageHelper
import com.bumptech.glide.Glide

@Serializable
data class VeiculoResponse(
    val nome_apelido:        String? = null,
    val marca:               String? = null,
    val modelo:              String? = null,
    val placa:               String? = null,
    val quilometragem_atual: Int?    = 0,
    val ano:                 Int?    = null,
    val combustivel:         String? = null
)

class HomeActivity : AppCompatActivity() {

    private val PERM_CODE = 101
    private val permissoes = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        /* ── Views ── */
        val tvVehicleName    = findViewById<TextView>(R.id.tv_vehicle_name)
        val tvPlateValue     = findViewById<TextView>(R.id.tv_plate_value)
        val tvOdometerValue  = findViewById<TextView>(R.id.tv_odometer_value)
        val tvFuelValue      = findViewById<TextView>(R.id.tv_fuel_value)
        val tvVehicleDetails = findViewById<TextView>(R.id.tv_vehicle_details)
        val tvStatus         = findViewById<TextView>(R.id.tv_status_text)
        val fabBLE           = findViewById<FloatingActionButton>(R.id.fab_scanner)
        val btnPerfil        = findViewById<View>(R.id.card_profile)

        /* ── Logout ── */
        btnPerfil.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                    startActivity(
                        Intent(this@HomeActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                } catch (e: Exception) {
                    android.util.Log.e("WealthCar", "Erro ao fazer logout", e)
                    showCustomToast("Erro ao sair da conta.")
                }
            }
        }

        /* ── Carrega sessão e veículo ── */
        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val user    = SupabaseClient.client.auth.currentUserOrNull()

                if (session == null || user == null) {
                    startActivity(
                        Intent(this@HomeActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                    return@launch
                }

                val resultado = SupabaseClient.client
                    .postgrest["veiculo"]
                    .select { filter { eq("id_usuario", user.id) } }
                    .decodeList<VeiculoResponse>()

                if (resultado.isNotEmpty()) {
                    val v      = resultado[0]
                    val marca  = v.marca  ?: ""
                    val modelo = v.modelo ?: ""
                    val ano    = v.ano?.toString() ?: ""

                    tvVehicleName.text    = if (!v.nome_apelido.isNullOrBlank()) v.nome_apelido else "$marca $modelo"
                    tvVehicleDetails.text = "$marca $modelo • $ano".trim()
                    tvPlateValue.text     = v.placa ?: "---"
                    tvOdometerValue.text  = "${v.quilometragem_atual ?: 0} km"
                    tvFuelValue.text      = v.combustivel ?: "-"

                    val ivCar = findViewById<ImageView>(R.id.iv_car_image)
                    val imageUrl = CarImageHelper.getUrl(marca, modelo)

                    Glide.with(this@HomeActivity)
                        .load(imageUrl)
                        .override(400, 300)          // ← força resolução uniforme
                        .fitCenter()                 // ← escala mantendo proporção
                        .placeholder(R.drawable.logo_wealthcar)
                        .error(R.drawable.logo_wealthcar)
                        .into(ivCar)
                }

            } catch (e: Exception) {
                android.util.Log.e("WealthCar", "Erro ao carregar veículo", e)
                showCustomToast("Não foi possível carregar os dados do veículo.")
            }
        }

        /* ── BLE — FAB de conexão ── */
        fabBLE.setOnClickListener {
            if (temPermissoes()) {
                if (BLEManager.status.value == BLEStatus.CONECTADO) {
                    BLEManager.desconectar()
                } else {
                    BLEManager.escanear(this)
                }
            } else {
                pedirPermissoes()
            }
        }

        /* Observer — status BLE → tv_status_text + ícone do FAB */
        lifecycleScope.launch {
            BLEManager.status.collectLatest { status ->
                tvStatus.text = when (status) {
                    BLEStatus.CONECTADO    -> "ESP32 Ativo"
                    BLEStatus.ESCANEANDO   -> "Procurando..."
                    BLEStatus.CONECTANDO   -> "Conectando..."
                    BLEStatus.ERRO         -> "Erro de conexão"
                    BLEStatus.DESCONECTADO -> "Desconectado"
                }
                // Muda ícone do FAB conforme estado
                fabBLE.setImageResource(
                    if (status == BLEStatus.CONECTADO)
                        android.R.drawable.ic_menu_close_clear_cancel
                    else
                        R.drawable.ic_directions_car
                )
            }
        }

        /* Observer — dados BLE → atualiza cards existentes */
        lifecycleScope.launch {
            BLEManager.dados.collectLatest { dados ->
                dados?.let {
                    tvOdometerValue.text = "${it.hodo} km"
                    tvFuelValue.text     = "${it.comb}%"
                    // Velocidade e RPM não têm card ainda no layout
                    // Adicione cards no XML quando quiser exibi-los
                }
            }
        }

        /* Observer — erros BLE */
        lifecycleScope.launch {
            BLEManager.erro.collectLatest { erro ->
                if (erro.isNotEmpty()) showCustomToast(erro)
            }
        }

        /* Back button */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishAffinity() }
        })
    }

    private fun temPermissoes() = permissoes.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun pedirPermissoes() {
        ActivityCompat.requestPermissions(this, permissoes, PERM_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_CODE &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            BLEManager.escanear(this)
        } else {
            showCustomToast("Permissões de Bluetooth necessárias")
        }
    }
}