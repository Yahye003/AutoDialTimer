package com.example.autodialtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_USSD = "*806*9299367*4#"
        const val PERM_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(0xFFF0F4FF.toInt())
        }

        val title = TextView(this).apply {
            text = "Auto Dial Timer"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1565C0.toInt())
            setPadding(20, 30, 20, 30)
            gravity = android.view.Gravity.CENTER
        }

        val labelUssd = TextView(this).apply {
            text = "USSD / Phone Number"
            textSize = 14f
            setTextColor(0xFF1565C0.toInt())
            setPadding(0, 30, 0, 8)
        }

        val etUssd = EditText(this).apply {
            setText(DEFAULT_USSD)
            textSize = 16f
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(16, 16, 16, 16)
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }

        val labelInterval = TextView(this).apply {
            text = "Interval Between Calls (seconds)"
            textSize = 14f
            setTextColor(0xFF1565C0.toInt())
            setPadding(0, 30, 0, 8)
        }

        val etInterval = EditText(this).apply {
            setText("30")
            textSize = 16f
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(16, 16, 16, 16)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val labelMax = TextView(this).apply {
            text = "Maximum Calls (0 = unlimited)"
            textSize = 14f
            setTextColor(0xFF1565C0.toInt())
            setPadding(0, 30, 0, 8)
        }

        val etMaxCalls = EditText(this).apply {
            setText("0")
            textSize = 16f
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(16, 16, 16, 16)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val tvStatus = TextView(this).apply {
            text = "Status: Stopped"
            textSize = 15f
            setTextColor(0xFF0D47A1.toInt())
            setBackgroundColor(0xFFE3F2FD.toInt())
            setPadding(20, 20, 20, 20)
            gravity = android.view.Gravity.CENTER
        }

        val btnStart = Button(this).apply {
            text = "START"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1565C0.toInt())
        }

        val btnStop = Button(this).apply {
            text = "STOP"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFB71C1C.toInt())
            isEnabled = false
        }

        btnStart.setOnClickListener {
            val ussd = etUssd.text.toString().trim()
            val interval = etInterval.text.toString().toLongOrNull() ?: 30L
            val maxCalls = etMaxCalls.text.toString().toIntOrNull() ?: 0

            if (ussd.isEmpty()) {
                Toast.makeText(this, "Enter a USSD or number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!hasPermissions()) { requestPermissions(); return@setOnClickListener }

            btnStart.isEnabled = false
            btnStop.isEnabled = true
            tvStatus.text = "Status: Dialing…"

            val intent = Intent(this, DialService::class.java).apply {
                putExtra("USSD", ussd)
                putExtra("INTERVAL", interval)
                putExtra("MAX_CALLS", maxCalls)
            }
            ContextCompat.startForegroundService(this, intent)
        }

        btnStop.setOnClickListener {
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            tvStatus.text = "Status: Stopped"
            stopService(Intent(this, DialService::class.java))
        }

        layout.addView(title)
        layout.addView(labelUssd)
        layout.addView(etUssd)
        layout.addView(labelInterval)
        layout.addView(etInterval)
        layout.addView(labelMax)
        layout.addView(etMaxCalls)
        layout.addView(tvStatus, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 30, 0, 30) })
        layout.addView(btnStart, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 150))
        layout.addView(btnStop, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 150).apply { setMargins(0, 20, 0, 0) })

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }

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
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            findViewById<Button>(android.R.id.button1)?.performClick()
        }
    }
}
