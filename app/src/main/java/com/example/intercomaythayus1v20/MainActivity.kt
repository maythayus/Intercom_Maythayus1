package fr.maythayus.intercom2sos.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.maythayus.intercom2sos.ble.BleHeartRateManager
import fr.maythayus.intercom2sos.usb.UsbService
import fr.maythayus.intercom2sos.ui.theme.IntercomAyThayus1V20Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var usbService: UsbService? = null
    private var bound = false
    private var bindRequested = false

    private var tts: TextToSpeech? = null

    private var bleManager: BleHeartRateManager? = null
    private var lastHeartRateBpm: Int? = null

    private var lastLat: Double? = null
    private var lastLon: Double? = null

    private val requestCodeLocation = 1001
    private val requestCodeBle = 1002

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as UsbService.LocalBinder
            usbService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            usbService = null
        }
    }

    override fun onStart() {
        super.onStart()
        bindUsbService()
    }

    override fun onStop() {
        if (bindRequested) {
            try {
                unbindService(serviceConnection)
            } catch (_: IllegalArgumentException) {
                // Not bound (race), ignore
            } finally {
                bound = false
                bindRequested = false
            }
        }
        super.onStop()
    }

    private fun emergencyNumber(): String {
        val country = Locale.getDefault().country.uppercase(Locale.ROOT)
        return when (country) {
            "US", "CA", "MX" -> "911"
            else -> "112"
        }
    }

    private fun buildEmergencyText(lat: Double, lon: Double): String {
        val hr = lastHeartRateBpm
        return if (hr != null) {
            "URGENT. Position GPS: $lat, $lon. FC: $hr bpm."
        } else {
            "URGENT. Position GPS: $lat, $lon."
        }
    }

    private fun shareEmergencyMessage(setStatus: (String) -> Unit) {
        if (!hasLocationPermission()) {
            setStatus("Autoriser la localisation pour partager")
            requestLocationPermission()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(this)
        val token = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    setStatus("Partage: GPS indisponible")
                    return@addOnSuccessListener
                }

                lastLat = loc.latitude
                lastLon = loc.longitude

                val text = buildEmergencyText(loc.latitude, loc.longitude)
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(share, "Partager message d'urgence"))
                setStatus("Message prêt à partager")
            }
            .addOnFailureListener {
                setStatus("Partage: erreur GPS")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Démarrer le service USB (le bind se fait dans onStart)
        startService(Intent(this, UsbService::class.java))

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRENCH
            }
        }

        setContent {
            val sosStatus = remember { mutableStateOf<String?>(null) }
            val bleStatus = remember { mutableStateOf<String?>(null) }
            val heartRate = remember { mutableStateOf<Int?>(null) }

            if (bleManager == null) {
                bleManager = BleHeartRateManager(
                    context = this,
                    onStatus = { msg ->
                        runOnUiThread { bleStatus.value = msg }
                    },
                    onHeartRate = { bpm ->
                        lastHeartRateBpm = bpm
                        runOnUiThread { heartRate.value = bpm }
                    },
                )
            }

            IntercomAyThayus1V20Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RadioApp(
                        isUsbConnected = { usbService?.isConnected() == true },
                        onPttDown = { usbService?.pttDown() },
                        onPttUp = { usbService?.pttUp() },
                        sosStatus = sosStatus.value,
                        onSosConfirm = { testMode ->
                            if (usbService?.isConnected() != true) {
                                sosStatus.value = "USB non connecté"
                            } else {
                                startSos(
                                    testMode = testMode,
                                    setStatus = { sosStatus.value = it },
                                    clearStatus = { sosStatus.value = null },
                                )
                            }
                        },
                        onCallEmergencyClick = {
                            val number = emergencyNumber()
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                            sosStatus.value = "Appel urgences: $number"
                        },
                        onShareEmergencyClick = {
                            shareEmergencyMessage(
                                setStatus = { sosStatus.value = it },
                            )
                        },
                        bleStatus = bleStatus.value,
                        heartRateBpm = heartRate.value,
                        onBleConnectClick = {
                            if (!hasBlePermissions()) {
                                bleStatus.value = "Autoriser Bluetooth pour la bague"
                                requestBlePermissions()
                            } else {
                                bleManager?.connect()
                            }
                        },
                        onBleDisconnectClick = {
                            bleManager?.disconnect()
                            heartRate.value = null
                            lastHeartRateBpm = null
                            bleStatus.value = "BLE: déconnecté"
                        },
                    )
                }
            }
        }
    }

    private fun startSos(
        testMode: Boolean,
        setStatus: (String) -> Unit,
        clearStatus: () -> Unit,
    ) {
        if (!hasLocationPermission()) {
            setStatus("Autoriser la localisation pour SOS")
            requestLocationPermission()
            return
        }

        setStatus(if (testMode) "TEST: récupération GPS…" else "SOS: récupération GPS…")

        val fused = LocationServices.getFusedLocationProviderClient(this)
        val token = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    setStatus("SOS: GPS indisponible")
                    return@addOnSuccessListener
                }

                val lat = loc.latitude
                val lon = loc.longitude

                lastLat = lat
                lastLon = lon

                // Demande au Pico de se placer sur la fréquence d'urgence (CH16 Marine)
                usbService?.send("FREQ 156.800 156.800\n")
                usbService?.send("GPS $lat $lon\n")

                if (testMode) {
                    setStatus("TEST: GPS envoyé (sans émission)")
                    return@addOnSuccessListener
                }

                val hr = lastHeartRateBpm
                val msg = if (hr != null) {
                    "Urgence. Position GPS. Latitude $lat. Longitude $lon. Fréquence cardiaque $hr battements par minute."
                } else {
                    "Urgence. Position GPS. Latitude $lat. Longitude $lon."
                }
                setStatus("SOS: émission…")

                usbService?.pttDown()
                speakAndReleasePtt(msg, onDone = {
                    usbService?.pttUp()
                    setStatus("SOS envoyé")
                })
            }
            .addOnFailureListener {
                setStatus("SOS: erreur GPS")
            }
    }

    private fun speakAndReleasePtt(text: String, onDone: () -> Unit) {
        val engine = tts ?: run {
            onDone()
            return
        }

        val utteranceId = "sos"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) {
                runOnUiThread { onDone() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                runOnUiThread { onDone() }
            }
        })

        @Suppress("DEPRECATION")
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            requestCodeLocation,
        )
    }

    private fun hasBlePermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            val scan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            val connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            scan == PackageManager.PERMISSION_GRANTED && connect == PackageManager.PERMISSION_GRANTED
        } else {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            fine == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBlePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
                requestCodeBle,
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCodeBle,
            )
        }
    }

    private fun bindUsbService() {
        Intent(this, UsbService::class.java).also { intent ->
            bindRequested = true
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}