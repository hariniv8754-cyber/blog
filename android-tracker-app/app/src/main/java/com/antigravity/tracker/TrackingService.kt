package com.antigravity.tracker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TrackingService : Service() {

    companion object {
        const val TAG = "QuantumTrackerService"
        const val CHANNEL_ID = "quantum_gps_tracker_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        const val EXTRA_USER_NAME = "EXTRA_USER_NAME"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mqttClient: MqttClient? = null

    private var roomId: String = "default-room"
    private var userName: String = "Friend (Android)"
    private var clientId: String = ""

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        val prefs = getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE)
        clientId = prefs.getString("client_id", null) ?: run {
            val newId = "android_" + UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString("client_id", newId).apply()
            newId
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: roomId
                    userName = intent.getStringExtra(EXTRA_USER_NAME) ?: userName

                    // Save configuration
                    getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE).edit()
                        .putString("room_id", roomId)
                        .putString("user_name", userName)
                        .putBoolean("is_tracking", true)
                        .apply()

                    startForegroundService()
                    acquireWakeLock()
                    connectMqtt()
                    startLocationUpdates()
                }
                ACTION_STOP -> {
                    stopTracking()
                }
            }
        }
        // START_STICKY: Ensures Android OS automatically resurrects the service if killed
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quantum GPS Tracker Active")
            .setContentText("Broadcasting 1s location to room: $roomId")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "QuantumGPSTracker::ForegroundWakeLock"
            ).apply {
                acquire()
            }
        }
    }

    private fun connectMqtt() {
        try {
            val serverUri = "tcp://broker.hivemq.com:1883"
            mqttClient = MqttClient(serverUri, "android_$clientId", MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = false
                connectionTimeout = 10
                keepAliveInterval = 30
            }

            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(TAG, "MQTT Connected to HiveMQ Cloud (Reconnect: $reconnect)")
                }
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "MQTT Connection lost: ${cause?.message}")
                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            Thread {
                try {
                    mqttClient?.connect(options)
                } catch (e: Exception) {
                    Log.e(TAG, "MQTT connect failed", e)
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MQTT", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdateDelayMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    broadcastLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun broadcastLocation(loc: Location) {
        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timestamp = timeFormat.format(Date(loc.time))

        val payload = JSONObject().apply {
            put("id", clientId)
            put("name", userName)
            put("lat", loc.latitude)
            put("lng", loc.longitude)
            put("accuracy", Math.round(loc.accuracy.toDouble()))
            put("speed", Math.round((loc.speed * 3.6).toDouble()))
            put("heading", Math.round(loc.bearing.toDouble()))
            put("room", roomId)
            put("timestamp", timestamp)
            put("provider", "native_android_fused")
        }

        val topic = "antigravity_tracker_v3/$roomId/$clientId"
        val message = MqttMessage(payload.toString().toByteArray()).apply {
            qos = 1
            isRetained = true
        }

        if (mqttClient?.isConnected == true) {
            try {
                mqttClient?.publish(topic, message)
                Log.d(TAG, "Broadcasted 1s location: $payload")
            } catch (e: Exception) {
                Log.e(TAG, "MQTT Publish error", e)
            }
        }
    }

    private fun stopTracking() {
        getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("is_tracking", false)
            .apply()

        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        wakeLock?.let { if (it.isHeld) it.release() }
        try {
            if (mqttClient?.isConnected == true) mqttClient?.disconnect()
        } catch (e: Exception) {}

        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Quantum GPS Background Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps location broadcasting continuously in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
