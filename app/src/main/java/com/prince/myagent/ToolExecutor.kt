package com.prince.myagent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject

class ToolExecutor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun execute(name: String, input: JSONObject): String {
        return try {
            when (name) {
                "send_sms" -> sendSms(input.getString("number"), input.getString("message"))
                "get_contacts" -> getContacts()
                "set_clipboard" -> setClipboard(input.getString("text"))
                "get_clipboard" -> getClipboard()
                "show_notification" -> showNotification(input.getString("title"), input.getString("content"))
                "get_location" -> getLocation()
                "open_url" -> openUrl(input.getString("url"))
                "vibrate" -> vibrate(input.optInt("duration_ms", 500))
                "toast" -> showToast(input.getString("message"))
                "get_battery" -> getBattery()
                else -> "Unknown tool: $name"
            }
        } catch (e: Exception) {
            "Error running $name: ${e.message}"
        }
    }

    private fun sendSms(number: String, message: String): String {
        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
        return "SMS sent to $number"
    }

    private fun getContacts(): String {
        val sb = StringBuilder()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, null, null, null, null
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 25) {
                val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIdx >= 0) {
                    sb.append(it.getString(nameIdx)).append("\n")
                    count++
                }
            }
        }
        return if (sb.isEmpty()) "No contacts found" else sb.toString()
    }

    private fun setClipboard(text: String): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("myagent", text))
        return "Clipboard set"
    }

    private fun getClipboard(): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        return if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).text?.toString() ?: "Clipboard empty"
        } else "Clipboard empty"
    }

    private fun showNotification(title: String, content: String): String {
        val channelId = "myagent_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "MyAgent", android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        NotificationManagerCompat.from(context).notify(1, notification)
        return "Notification shown"
    }

    private fun getLocation(): String {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p)
                if (loc != null) return "lat=${loc.latitude}, lng=${loc.longitude}"
            } catch (e: SecurityException) {
                return "Location permission not granted"
            }
        }
        return "No location fix available yet"
    }

    private fun openUrl(url: String): String {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Opened $url"
    }

    private fun vibrate(durationMs: Int): String {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs.toLong())
        }
        return "Vibrated for ${durationMs}ms"
    }

    private fun showToast(message: String): String {
        mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        return "Toast shown"
    }

    private fun getBattery(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        return "Battery: $level% (${if (charging) "charging" else "not charging"})"
    }
}
