package fr.maythayus.intercom2sos.usb
 
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fr.maythayus.intercom2sos.BuildConfig
import fr.maythayus.intercom2sos.R
import fr.maythayus.intercom2sos.ui.MainActivity
import java.util.concurrent.atomic.AtomicBoolean
 
class UsbService : Service() {
    private val binder = LocalBinder()
 
    private val usbManager: UsbManager by lazy {
        getSystemService(Context.USB_SERVICE) as UsbManager
    }
 
    private var usbConnection: UsbConnection? = null
    private var connectedDevice: UsbDevice? = null
    private var isForeground = false

    private val readLoopRunning = AtomicBoolean(false)
    private var readThread: Thread? = null

    @Volatile
    private var lastPicoLine: String? = null
 
    private val permissionAction = "${BuildConfig.APPLICATION_ID}.USB_PERMISSION"
 
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                permissionAction -> {
                    val device = intent.parcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (device != null && granted) {
                        connect(device)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.parcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    if (device != null && device == connectedDevice) {
                        disconnect()
                    }
                }
            }
        }
    }
 
    inner class LocalBinder : Binder() {
        fun getService(): UsbService = this@UsbService
    }
 
    override fun onCreate() {
        super.onCreate()
        usbConnection = UsbConnection(usbManager)
 
        val filter = IntentFilter().apply {
            addAction(permissionAction)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }
 
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device = intent?.parcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        if (device != null) {
            requestPermissionAndConnect(device)
        }
        return START_STICKY
    }
 
    override fun onBind(intent: Intent): IBinder = binder
 
    override fun onDestroy() {
        disconnect()
        unregisterReceiver(receiver)
        super.onDestroy()
    }
 
    fun isConnected(): Boolean = usbConnection?.isConnected == true
 
    fun send(data: String) {
        usbConnection?.send(data)
    }

    fun initPico() {
        if (!isConnected()) return
        send("INIT\n")
        send("SAFE TXTO=2500 IDLE=30000\n")
    }

    fun setFreq(txMHz: String, rxMHz: String) {
        if (!isConnected()) return
        send("FREQ $txMHz $rxMHz\n")
    }

    fun setVol(vol: Int) {
        if (!isConnected()) return
        send("VOL $vol\n")
    }

    fun setSql(sql: Int) {
        if (!isConnected()) return
        send("SQL $sql\n")
    }

    fun requestStatus() {
        if (!isConnected()) return
        send("STATUS\n")
    }

    fun recoverPico() {
        if (!isConnected()) return
        send("RECOVER\n")
    }

    fun lastStatusLine(): String? = lastPicoLine
    fun pttDown() {
        if (!isConnected()) return
        send("PTT 1\n")
    }

    fun pttUp() {
        if (!isConnected()) return
        send("PTT 0\n")
    }

    fun requestPermissionAndConnect(device: UsbDevice) {
        ensureForeground("USB prêt")
        if (usbManager.hasPermission(device)) {
            connect(device)
            return
        }
 
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(permissionAction),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        usbManager.requestPermission(device, pendingIntent)
    }
 
    private fun connect(device: UsbDevice) {
        ensureForeground("USB prêt")
        connectedDevice = device
        val ok = usbConnection?.connect(device) == true
        val text = if (ok) "USB connecté" else "USB échec connexion"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification(text))

        if (ok) {
            startReadLoop()
            initPico()
        }
    }
 
    private fun disconnect() {
        stopReadLoop()
        usbConnection?.disconnect()
        connectedDevice = null
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification("USB déconnecté"))
    }

    private fun startReadLoop() {
        if (readLoopRunning.getAndSet(true)) return
        readThread = Thread {
            val sb = StringBuilder()
            while (readLoopRunning.get()) {
                val chunk = try {
                    usbConnection?.read()
                } catch (_: Exception) {
                    null
                } ?: continue

                sb.append(chunk)
                while (true) {
                    val idx = sb.indexOf("\n")
                    if (idx < 0) break
                    val line = sb.substring(0, idx).trimEnd('\r')
                    sb.delete(0, idx + 1)
                    if (line.isNotBlank()) {
                        lastPicoLine = line
                    }
                }
            }
        }.apply { isDaemon = true }
        readThread?.start()
    }

    private fun stopReadLoop() {
        readLoopRunning.set(false)
        readThread?.interrupt()
        readThread = null
    }

    private fun ensureForeground(text: String) {
        if (isForeground) return
        val notification = buildNotification(text)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                @Suppress("DEPRECATION")
                startForeground(1, notification)
            }
            isForeground = true
        } catch (_: SecurityException) {
            // If the OS refuses connectedDevice type (e.g., no physical device), keep service non-foreground.
        }
    }
 
    private fun buildNotification(text: String) = NotificationCompat.Builder(this, ensureChannel())
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(text)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            )
        )
        .setOngoing(true)
        .build()
 
    private fun ensureChannel(): String {
        val channelId = "usb"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(channelId, "USB", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return channelId
    }

    private fun <T> Intent.parcelableExtra(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }
}