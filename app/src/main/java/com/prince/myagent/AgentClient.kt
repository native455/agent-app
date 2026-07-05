package com.prince.myagent

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AgentClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-sonnet-5"

    private fun tool(name: String, desc: String, props: JSONObject, required: JSONArray): JSONObject {
        val schema = JSONObject()
        schema.put("type", "object")
        schema.put("properties", props)
        schema.put("required", required)
        val t = JSONObject()
        t.put("name", name)
        t.put("description", desc)
        t.put("input_schema", schema)
        return t
    }

    private fun toolDefinitions(): JSONArray {
        val tools = JSONArray()

        tools.put(tool("send_sms", "Send an SMS text message",
            JSONObject().put("number", JSONObject().put("type", "string"))
                .put("message", JSONObject().put("type", "string")),
            JSONArray().put("number").put("message")))

        tools.put(tool("get_contacts", "List contacts stored on the phone",
            JSONObject(), JSONArray()))

        tools.put(tool("set_clipboard", "Copy text to the clipboard",
            JSONObject().put("text", JSONObject().put("type", "string")),
            JSONArray().put("text")))

        tools.put(tool("get_clipboard", "Read current clipboard text",
            JSONObject(), JSONArray()))

        tools.put(tool("show_notification", "Show an Android notification",
            JSONObject().put("title", JSONObject().put("type", "string"))
                .put("content", JSONObject().put("type", "string")),
            JSONArray().put("title").put("content")))

        tools.put(tool("get_location", "Get last known GPS location",
            JSONObject(), JSONArray()))

        tools.put(tool("open_url", "Open a URL or app link",
            JSONObject().put("url", JSONObject().put("type", "string")),
            JSONArray().put("url")))

        tools.put(tool("vibrate", "Vibrate the phone",
            JSONObject().put("duration_ms", JSONObject().put("type", "integer")),
            JSONArray()))

        tools.put(tool("toast", "Show a short on-screen toast message",
            JSONObject().put("message", JSONObject().put("type", "string")),
            JSONArray().put("message")))

        tools.put(tool("get_battery", "Get battery level and charging status",
            JSONObject(), JSONArray()))

        return tools
    }

    fun runTask(apiKey: String, task: String, executor: ToolExecutor, log: (String) -> Unit) {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "user").put("content", task))

        var turns = 0
        while (turns < 8) {
            turns++
            val body = JSONObject()
            body.put("model", MODEL)
            body.put("max_tokens", 1024)
            body.put("tools", toolDefinitions())
            body.put("messages", messages)

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                log("Network error: ${e.message}")
                return
            }

            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                log("API error (${response.code}): $responseBody")
                return
            }

            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
            val assistantContent = JSONArray()
            var hasToolUse = false
            val toolResults = JSONArray()

            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                assistantContent.put(block)
                when (block.getString("type")) {
                    "text" -> log(block.getString("text"))
                    "tool_use" -> {
                        hasToolUse = true
                        val toolName = block.getString("name")
                        val toolId = block.getString("id")
                        val input = block.optJSONObject("input") ?: JSONObject()
                        log("[running tool: $toolName]")
                        val result = executor.execute(toolName, input)
                        toolResults.put(
                            JSONObject()
                                .put("type", "tool_result")
                                .put("tool_use_id", toolId)
                                .put("content", result)
                        )
                    }
                }
            }

            messages.put(JSONObject().put("role", "assistant").put("content", assistantContent))

            if (!hasToolUse) {
                break
            }
            messages.put(JSONObject().put("role", "user").put("content", toolResults))
        }
    }
}
