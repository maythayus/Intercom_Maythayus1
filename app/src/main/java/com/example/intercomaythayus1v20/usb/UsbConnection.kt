package fr.maythayus.intercom2sos.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

class UsbConnection(private val usbManager: UsbManager) {
    private var port: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null

    fun connect(device: UsbDevice): Boolean {
        return try {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            connection = usbManager.openDevice(device)
            port = driver.ports[0].apply {
                open(connection)
                setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            }
            true
        } catch (e: Exception) {
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            port?.close()
            connection?.close()
        } catch (e: IOException) {
            // Ignorer les erreurs de fermeture
        } finally {
            port = null
            connection = null
        }
    }

    fun send(data: String) {
        port?.write(data.toByteArray(), 1000)
    }

    fun read(): String? {
        return port?.let {
            val buffer = ByteArray(1024)
            val len = it.read(buffer, 1000)
            if (len > 0) String(buffer, 0, len) else null
        }
    }

    val isConnected: Boolean
        get() = port != null
}