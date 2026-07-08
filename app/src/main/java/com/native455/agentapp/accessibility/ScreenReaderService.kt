package com.native455.agentapp.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenReaderService : AccessibilityService() {

    private val tag = "ScreenReader"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(tag, "ScreenReaderService connected — read-only mode, no actions will be performed")
        try {
            val dir = getExternalFilesDir(null)
            logFile = File(dir, "screen_log.txt")
            logLine("=== ScreenReaderService started ===")
        } catch (e: Exception) {
            Log.e(tag, "Could not open log file: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName?.toString() ?: "unknown"
                logLine("SCREEN CHANGE in $packageName")
                dumpScreenElements()
            }
        }
    }

    private fun dumpScreenElements() {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            logLine("  (no root node available)")
            return
        }
        walkNode(root, depth = 0)
    }

    private fun walkNode(node: AccessibilityNodeInfo, depth: Int) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val className = node.className?.toString() ?: "unknown"
        val clickable = node.isClickable
        val editable = node.isEditable

        if (!text.isNullOrEmpty() || !desc.isNullOrEmpty()) {
            val indent = "  ".repeat(depth)
            val label = text ?: desc
            val role = when {
                editable -> "text_field"
                clickable -> "button"
                else -> "label"
            }
            logLine("$indent[$role] \"$label\" (class=$className, clickable=$clickable, editable=$editable)")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            walkNode(child, depth + 1)
            child.recycle()
        }
    }

    private fun logLine(line: String) {
        val timestamped = "${dateFormat.format(Date())} $line"
        Log.i(tag, timestamped)
        try {
            logFile?.appendText("$timestamped\n")
        } catch (e: Exception) {
            Log.e(tag, "Failed to write log file: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.w(tag, "ScreenReaderService interrupted")
    }
}
