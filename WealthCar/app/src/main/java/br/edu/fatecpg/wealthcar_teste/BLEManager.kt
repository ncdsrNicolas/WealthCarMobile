package br.edu.fatecpg.wealthcar_teste

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID

/* ─── UUIDs — confirmar com o colega de hardware ─────────────────── */
val SERVICE_UUID        = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

/* ─── Dados recebidos via BLE ─────────────────────────────────────── */
data class DadosBLE(
    val vel:  Int    = 0,
    val rpm:  Int    = 0,
    val comb: Int    = 0,
    val hodo: Int    = 0,
)

/* ─── Status da conexão ───────────────────────────────────────────── */
enum class BLEStatus {
    DESCONECTADO, ESCANEANDO, CONECTANDO, CONECTADO, ERRO
}

@SuppressLint("MissingPermission")
object BLEManager {

    private val _status = MutableStateFlow(BLEStatus.DESCONECTADO)
    val status: StateFlow<BLEStatus> = _status

    private var currentScanCallback: ScanCallback? = null

    private val _dados = MutableStateFlow<DadosBLE?>(null)
    val dados: StateFlow<DadosBLE?> = _dados

    private val _erro = MutableStateFlow("")
    val erro: StateFlow<String> = _erro

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner?  = null

    /* ── Iniciar escaneamento ── */
    fun escanear(context: Context) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (adapter == null || !adapter.isEnabled) {
            _erro.value = "Bluetooth desativado. Ative e tente novamente."
            _status.value = BLEStatus.ERRO
            return
        }

        _status.value = BLEStatus.ESCANEANDO
        _erro.value = ""
        scanner = adapter.bluetoothLeScanner

        val filtro = ScanFilter.Builder()
            .setDeviceName("WealthCar-ESP32")
            .build()

        val config = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = scanCallback(context)
        currentScanCallback = callback // Salva a referência corretamente

        // CORREÇÃO: Apenas UMA chamada de startScan
        scanner?.startScan(listOf(filtro), config, callback)
    }

    /* ── Callback do escaneamento ── */
    private fun scanCallback(context: Context) = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            scanner?.stopScan(this)
            conectar(context, result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            _erro.value = "Falha no escaneamento (código $errorCode)"
            _status.value = BLEStatus.ERRO
        }
    }

    /* ── Conectar ao dispositivo ── */
    private fun conectar(context: Context, device: BluetoothDevice) {
        _status.value = BLEStatus.CONECTANDO

        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _status.value = BLEStatus.CONECTADO
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _status.value = BLEStatus.DESCONECTADO
                        _dados.value  = null
                        bluetoothGatt = null
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val characteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)

                if (characteristic == null) {
                    _erro.value  = "Característica BLE não encontrada"
                    _status.value = BLEStatus.ERRO
                    return
                }

                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val texto = String(characteristic.value, Charsets.UTF_8)
                parsearDados(texto)
            }
        })
    }

    /* ── Parsear JSON do ESP32 ── */
    private fun parsearDados(json: String) {
        try {
            // CORREÇÃO: Usando JSONObject nativo para evitar falhas silenciosas de Regex
            val jsonObject = JSONObject(json)
            val vel  = jsonObject.optInt("vel", 0)
            val rpm  = jsonObject.optInt("rpm", 0)
            val comb = jsonObject.optInt("comb", 0)
            val hodo = jsonObject.optInt("hodo", 0)

            _dados.value = DadosBLE(vel, rpm, comb, hodo)
        } catch (e: Exception) {
            android.util.Log.e("WealthCar", "JSON inválido ou incompleto: $json", e)
        }
    }

    /* ── Desconectar ── */
    fun desconectar() {
        currentScanCallback?.let { scanner?.stopScan(it) }
        currentScanCallback = null
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _status.value = BLEStatus.DESCONECTADO
        _dados.value  = null
    }

    fun resetar() {
        currentScanCallback?.let { scanner?.stopScan(it) }
        currentScanCallback = null
        _status.value = BLEStatus.DESCONECTADO
        _erro.value   = ""
    }

    fun conectarDispositivo(context: Context, device: BluetoothDevice) {
        _status.value = BLEStatus.CONECTANDO
        conectar(context, device)
    }
}