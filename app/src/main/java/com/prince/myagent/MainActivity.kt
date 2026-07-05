package com.prince.myagent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: EditText
    private lateinit var taskInput: EditText
    private lateinit var logText: TextView
    private lateinit var executor: ToolExecutor
    private lateinit var prefs: android.content.SharedPreferences

    private val requiredPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.VIBRATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("myagent_prefs", MODE_PRIVATE)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        taskInput = findViewById(R.id.taskInput)
        logText = findViewById(R.id.logText)
        val runButton: Button = findViewById(R.id.runButton)

        apiKeyInput.setText(prefs.getString("api_key", ""))

        executor = ToolExecutor(applicationContext)

        val perms = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
        }

        runButton.setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            val task = taskInput.text.toString().trim()
            if (apiKey.isEmpty() || task.isEmpty()) {
                appendLog("Enter an API key and a task first.")
                return@setOnClickListener
            }
            prefs.edit().putString("api_key", apiKey).apply()
            appendLog("You: $task")

            Thread {
                AgentClient.runTask(apiKey, task, executor) { line ->
                    runOnUiThread { appendLog(line) }
                }
            }.start()
        }
    }

    private fun appendLog(line: String) {
        logText.append("\n\n$line")
    }
}
