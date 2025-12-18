package fr.maythayus.intercom2sos.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

class BleHeartRateManager(
    private val context: Context,
    private val onStatus: (String) -> Unit,
    private val onHeartRate: (Int) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val hrServiceUuid: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val hrMeasurementUuid: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var gatt: BluetoothGatt? = null
    private var scannerCallback: ScanCallback? = null

    private fun postStatus(text: String) {
        mainHandler.post { onStatus(text) }
    }

    private fun postHeartRate(bpm: Int) {
        mainHandler.post { onHeartRate(bpm) }
    }

    fun isConnected(): Boolean = gatt != null

    fun connect() {
        if (!hasRequiredPermissions()) {
            postStatus("BLE: permissions manquantes")
            return
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            postStatus("BLE: Bluetooth désactivé")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            postStatus("BLE: scanner indisponible")
            return
        }

        disconnect()

        postStatus("BLE: scan…")

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(hrServiceUuid))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                try {
                    scanner.stopScan(this)
                } catch (_: Exception) {
                }

                scannerCallback = null

                postStatus("BLE: connexion…")
                gatt = if (Build.VERSION.SDK_INT >= 23) {
                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    @Suppress("DEPRECATION")
                    device.connectGatt(context, false, gattCallback)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                postStatus("BLE: scan échec ($errorCode)")
            }
        }

        scannerCallback = cb
        try {
            scanner.startScan(listOf(filter), settings, cb)
        } catch (_: Exception) {
            scannerCallback = null
            postStatus("BLE: scan indisponible")
            return
        }

        mainHandler.postDelayed({
            val current = scannerCallback
            if (current == cb) {
                try {
                    scanner.stopScan(cb)
                } catch (_: Exception) {
                }
                scannerCallback = null
                if (gatt == null) {
                    postStatus("BLE: aucun périphérique trouvé")
                }
            }
        }, 15000L)
    }

    fun disconnect() {
        scannerCallback = null
        gatt?.let {
            try {
                it.disconnect()
            } catch (_: Exception) {
            }
            try {
                it.close()
            } catch (_: Exception) {
            }
        }
        gatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                postStatus("BLE: erreur connexion ($status)")
                disconnect()
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                postStatus("BLE: connecté")
                gatt.discoverServices()
            } else {
                postStatus("BLE: déconnecté")
                disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                postStatus("BLE: services échec ($status)")
                return
            }
            val service = gatt.getService(hrServiceUuid)
            val characteristic = service?.getCharacteristic(hrMeasurementUuid)
            if (characteristic == null) {
                postStatus("BLE: HR non trouvé")
                return
            }

            gatt.setCharacteristicNotification(characteristic, true)
            val cccd = characteristic.getDescriptor(cccdUuid)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid != hrMeasurementUuid) return
            val bytes = characteristic.value ?: return
            if (bytes.isEmpty()) return

            val flags = bytes[0].toInt() and 0xFF
            val hrFormatUInt16 = (flags and 0x01) != 0
            val hr = if (hrFormatUInt16) {
                if (bytes.size >= 3) {
                    ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
                } else null
            } else {
                if (bytes.size >= 2) (bytes[1].toInt() and 0xFF) else null
            }

            if (hr != null) {
                postHeartRate(hr)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            val scanOk = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val connectOk = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            scanOk && connectOk
        } else {
            val btOk = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            val adminOk = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            val locOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            btOk && adminOk && locOk
        }
    }
}
