package com.example.puretank

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var tvGaugeNum: TextView
    private lateinit var gaugeProgress: ProgressBar
    private lateinit var tvTurbValue: TextView
    private lateinit var tvPhValue: TextView
    private lateinit var tvTempValue: TextView
    private lateinit var tvSignalValue: TextView
    private lateinit var tvTurbStatus: TextView
    private lateinit var tvTankPct: TextView
    private lateinit var tankFill: View
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabPump: FloatingActionButton
    private lateinit var waveView1: View
    private lateinit var waveView2: View

    private val CHANNEL_ID = "puretank_alerts"
    private val handler = Handler(Looper.getMainLooper())
    private var isSimulationRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // Find views
        tvGaugeNum = findViewById(R.id.tvGaugeNum)
        gaugeProgress = findViewById(R.id.gaugeProgress)
        tvTurbValue = findViewById(R.id.tvTurbValue)
        tvPhValue = findViewById(R.id.tvPhValue)
        tvTempValue = findViewById(R.id.tvTempValue)
        tvSignalValue = findViewById(R.id.tvSignalValue)
        tvTurbStatus = findViewById(R.id.tvTurbStatus)
        tvTankPct = findViewById(R.id.tvTankPct)
        tankFill = findViewById(R.id.tankFill)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabPump = findViewById(R.id.fabPump)
        waveView1 = findViewById(R.id.waveView1)
        waveView2 = findViewById(R.id.waveView2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupNavigation()
        setupQuickActions()
        startSensorSimulation()
        startWaveAnimation()

        // Central FAB for Pump control (Match HTML)
        fabPump.setOnClickListener {
            showPumpDialog()
        }
    }

    private fun showPumpDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚡ PUMP CONTROL")
            .setMessage("Water pump is currently OFF. Initiate system activation?")
            .setPositiveButton("TURN ON") { _, _ ->
                Toast.makeText(this, "Pump ON ✓ Signal Sent to ESP32", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun startWaveAnimation() {
        waveView1.animate().translationX(50f).setDuration(2000).withEndAction {
            waveView1.animate().translationX(-50f).setDuration(2000).withEndAction {
                if (isSimulationRunning) startWaveAnimation()
            }
        }.start()

        waveView2.animate().translationX(-30f).setDuration(1500).withEndAction {
            waveView2.animate().translationX(30f).setDuration(1500).start()
        }.start()
    }

    private fun setupQuickActions() {
        findViewById<View>(R.id.actionAlerts).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }
        findViewById<View>(R.id.actionMaint).setOnClickListener {
            startActivity(Intent(this, MaintenanceActivity::class.java))
        }
        findViewById<View>(R.id.actionHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.actionPump).setOnClickListener {
            showPumpDialog()
        }
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dash -> true
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Long press DASH to open Profile (As in HTML JS)
        val dashItem = bottomNavigation.findViewById<View>(R.id.nav_dash)
        dashItem?.setOnLongClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            true
        }
    }

    private fun startSensorSimulation() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isSimulationRunning) return

                val turbidity = (Random.nextDouble(0.5, 5.0) * 10).toInt() / 10.0
                val ph = (Random.nextDouble(6.0, 8.5) * 10).toInt() / 10.0
                val temp = (Random.nextDouble(24.0, 32.0) * 10).toInt() / 10.0
                val clarity = Random.nextInt(40, 95)
                val tankLevel = Random.nextInt(30, 90)

                tvTurbValue.text = "$turbidity NTU"
                tvPhValue.text = "$ph pH"
                tvTempValue.text = "$temp °C"
                tvGaugeNum.text = clarity.toString()
                gaugeProgress.progress = clarity
                tvTankPct.text = "$tankLevel%"
                
                tvTurbStatus.text = if (turbidity > 4.0) "⚠ DANGER" else if (turbidity > 2.5) "Warning" else "Normal Range"
                tvTurbStatus.setTextColor(if (turbidity > 4.0) getColor(R.color.pure_rd) else getColor(R.color.pure_dm))

                val params = tankFill.layoutParams
                params.height = (tankLevel * 1.5).toInt()
                tankFill.layoutParams = params

                if (turbidity > 4.0) {
                    showNotification("⚠ CRITICAL ALERT", "Water turbidity is too high: $turbidity NTU")
                }

                handler.postDelayed(this, 3000)
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PureTank Alerts"
            val descriptionText = "Notifications for dirty water alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random.nextInt(), builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        isSimulationRunning = false
    }
}
