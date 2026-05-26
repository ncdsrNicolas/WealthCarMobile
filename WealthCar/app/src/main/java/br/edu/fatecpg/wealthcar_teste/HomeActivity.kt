package br.edu.fatecpg.wealthcar_teste

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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

    private val PERM_CODE   = 101
    private val SELECT_CODE = 102

    /* ── CORREÇÃO: Permissões dinâmicas baseadas na versão do Android ── */
    private val permissoes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val deviceManager by lazy {
        getSystemService(CompanionDeviceManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvVehicleName    = findViewById<TextView>(R.id.tv_vehicle_name)
        val tvPlateValue     = findViewById<TextView>(R.id.tv_plate_value)
        val tvOdometerValue  = findViewById<TextView>(R.id.tv_odometer_value)
        val tvFuelValue      = findViewById<TextView>(R.id.tv_fuel_value)
        val tvVehicleDetails = findViewById<TextView>(R.id.tv_vehicle_details)
        val tvStatus         = findViewById<TextView>(R.id.tv_status_text)
        val fabBLE           = findViewById<FloatingActionButton>(R.id.fab_scanner)
        val btnPerfil        = findViewById<View>(R.id.card_profile)

        BLEManager.resetar()

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

                    val ivCar    = findViewById<ImageView>(R.id.iv_car_image)
                    val imageUrl = CarImageHelper.getUrl(marca, modelo)

                    Glide.with(this@HomeActivity)
                        .load(imageUrl)
                        .override(400, 300)
                        .fitCenter()
                        .placeholder(R.drawable.logo_wealthcar)
                        .error(R.drawable.logo_wealthcar)
                        .into(ivCar)
                }

            } catch (e: Exception) {
                android.util.Log.e("WealthCar", "Erro ao carregar veículo", e)
                showCustomToast("Não foi possível carregar os dados do veículo.")
            }
        }

        fabBLE.setOnClickListener {
            if (BLEManager.status.value == BLEStatus.CONECTADO) {
                BLEManager.desconectar()
            } else {
                if (temPermissoes()) {
                    abrirSeletorBluetooth()
                } else {
                    pedirPermissoes()
                }
            }
        }

        lifecycleScope.launch {
            BLEManager.status.collectLatest { status ->
                tvStatus.text = when (status) {
                    BLEStatus.CONECTADO    -> "ESP32 Ativo"
                    BLEStatus.ESCANEANDO   -> "Procurando..."
                    BLEStatus.CONECTANDO   -> "Conectando..."
                    BLEStatus.ERRO         -> "Erro de conexão"
                    BLEStatus.DESCONECTADO -> "Desconectado"
                }
                fabBLE.setImageResource(
                    if (status == BLEStatus.CONECTADO)
                        android.R.drawable.ic_menu_close_clear_cancel
                    else
                        R.drawable.ic_directions_car
                )
            }
        }

        lifecycleScope.launch {
            BLEManager.dados.collectLatest { dados ->
                dados?.let {
                    tvOdometerValue.text = "${it.hodo} km"
                    tvFuelValue.text     = "${it.comb}%"
                }
            }
        }

        lifecycleScope.launch {
            BLEManager.erro.collectLatest { erro ->
                if (erro.isNotEmpty()) showCustomToast(erro)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishAffinity() }
        })
    }

    private fun abrirSeletorBluetooth() {
        val filtro = BluetoothLeDeviceFilter.Builder().build()

        val request = AssociationRequest.Builder()
            .addDeviceFilter(filtro)
            .setSingleDevice(false)
            .build()

        deviceManager.associate(
            request,
            object : CompanionDeviceManager.Callback() {
                override fun onDeviceFound(chooserLauncher: IntentSender) {
                    startIntentSenderForResult(
                        chooserLauncher,
                        SELECT_CODE,
                        null, 0, 0, 0
                    )
                }

                override fun onFailure(error: CharSequence?) {
                    showCustomToast("Nenhum dispositivo WealthCar encontrado")
                    android.util.Log.e("WealthCar", "BLE associate falhou: $error")
                }
            },
            null
        )
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_CODE && resultCode == Activity.RESULT_OK) {
            val scanResult = data?.getParcelableExtra<android.bluetooth.le.ScanResult>(
                CompanionDeviceManager.EXTRA_DEVICE
            )
            val device = scanResult?.device

            if (device != null) {
                BLEManager.conectarDispositivo(this, device)
                showCustomToast("Conectando ao dispositivo...")
            } else {
                showCustomToast("Nenhum dispositivo selecionado")
            }
        }
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
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            abrirSeletorBluetooth()
        } else {
            showCustomToast("Permissões de Bluetooth/Localização necessárias")
        }
    }
}