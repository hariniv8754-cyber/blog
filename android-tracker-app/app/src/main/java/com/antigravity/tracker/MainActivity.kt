package com.antigravity.tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.antigravity.tracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val postNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (fineLocationGranted) {
            checkBackgroundLocationPermission()
        } else {
            Toast.makeText(this, "Fine Location permission is required for accurate tracking.", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startTrackingService()
        } else {
            // Still proceed with foreground tracking
            startTrackingService()
            Toast.makeText(this, "Set Location to 'Allow all the time' in Settings for 24/7 tracking.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedData()
        setupListeners()
        updateUIState()
    }

    private fun loadSavedData() {
        val prefs = getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE)
        binding.inputRoomId.setText(prefs.getString("room_id", "room-alpha"))
        binding.inputUserName.setText(prefs.getString("user_name", "Friend (Android)"))
    }

    private fun setupListeners() {
        binding.btnToggleTracking.setOnClickListener {
            val prefs = getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE)
            val isTracking = prefs.getBoolean("is_tracking", false)

            if (isTracking) {
                stopTrackingService()
            } else {
                val roomId = binding.inputRoomId.text.toString().trim()
                val userName = binding.inputUserName.text.toString().trim()

                if (roomId.isEmpty() || userName.isEmpty()) {
                    Toast.makeText(this, "Please enter both Room ID and your Name.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Save
                prefs.edit()
                    .putString("room_id", roomId)
                    .putString("user_name", userName)
                    .apply()

                requestPermissionsAndStart()
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            checkBackgroundLocationPermission()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                return
            }
        }
        startTrackingService()
    }

    private fun startTrackingService() {
        val roomId = binding.inputRoomId.text.toString().trim()
        val userName = binding.inputUserName.text.toString().trim()

        val serviceIntent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_ROOM_ID, roomId)
            putExtra(TrackingService.EXTRA_USER_NAME, userName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        updateUIState()
        Toast.makeText(this, "🟢 24/7 Location Broadcaster Started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopTrackingService() {
        val serviceIntent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        startService(serviceIntent)
        updateUIState()
        Toast.makeText(this, "Broadcaster Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIState() {
        val prefs = getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE)
        val isTracking = prefs.getBoolean("is_tracking", false)

        if (isTracking) {
            binding.statusDot.setBackgroundResource(R.drawable.dot_online)
            binding.statusText.text = "🟢 Broadcasting 24/7 (Non-Killable)"
            binding.statusSubText.text = "GPS streams every 1s. You can swipe away this app."
            binding.btnToggleTracking.text = "Stop Tracking Service"
            binding.btnToggleTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.red_danger))
            binding.inputRoomId.isEnabled = false
            binding.inputUserName.isEnabled = false
        } else {
            binding.statusDot.setBackgroundResource(R.drawable.dot_standby)
            binding.statusText.text = "GPS Standby"
            binding.statusSubText.text = "Enter Room ID & Name, then tap Start."
            binding.btnToggleTracking.text = "Start 24/7 Tracking Service"
            binding.btnToggleTracking.setBackgroundColor(ContextCompat.getColor(this, R.color.cyber_cyan))
            binding.inputRoomId.isEnabled = true
            binding.inputUserName.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }
}
