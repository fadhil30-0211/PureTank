package com.example.puretank

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnExport).setOnClickListener {
            Toast.makeText(this, "Data exported as CSV to Downloads", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnReset).setOnClickListener {
            showResetDialog()
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠ FACTORY RESET")
            .setMessage("This will erase all settings and calibration data. This action cannot be undone. Proceed?")
            .setPositiveButton("CONFIRM") { _, _ ->
                Toast.makeText(this, "System Reset Initialized...", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}
