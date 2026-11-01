package com.example.autodialtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.autodialtimer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isDialing = false

    companion object {
        const val DEFAULT_USSD = "*806*9299367*4#"
        const val DEFAULT_INTERVAL = 30L       // seconds
        const val PERM_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill defaults
        binding.etUssd.setText(DEFAULT_USSD)
        binding.etInterval.setText(DEFAULT_INTERVAL.toString())
        binding.etMaxCalls.setText("0")   // 0 = unlimited

        binding.btnStart.setOnClickListener { handleStart() }
        binding.btnStop.setOnClickListener  { handleStop()  }
    }

    private fun handleStart() {
        val ussd     = binding.etUssd.text.toString().trim()
        val interval = binding.etInterval.text.toString().toLongOrNull() ?: DEFAULT_INTERVAL
        val maxCalls = binding.etMaxCalls.text.toString().toIntOrNull() ?: 0

        if (ussd.isEmpty()) {
            toast("Please enter a USSD / phone number"); return
        }

        if (!hasPermissions()) { requestPermissions(); return }

        isDialing = true
        updateUi()

        val intent = Intent(this, DialService::class.java).apply {
            putExtra("USSD",      ussd)
            putExtra("INTERVAL",  interval)
            putExtra("MAX_CALLS", maxCalls)
        }
        ContextCompat.startForegroundService(this, intent)
        toast("Auto-dialing started")
    }

    private fun handleStop() {
        isDialing = false
        updateUi()
        stopService(Intent(this, DialService::class.java))
        toast("Auto-dialing stopped")
    }

    private fun updateUi() {
        binding.btnStart.isEnabled = !isDialing
        binding.btnStop.isEnabled  =  isDialing
        binding.tvStatus.text      = if (isDialing) "Status: Dialing…" else "Status: Stopped"
    }

    // ── Permissions ──────────────────────────────────────────────────────────
    private fun hasPermissions(): Boolean {
        val needed = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            needed += Manifest.permission.POST_NOTIFICATIONS

        return needed.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val needed = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            needed += Manifest.permission.POST_NOTIFICATIONS

        ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            handleStart()
        } else {
            toast("Permissions required to dial")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
