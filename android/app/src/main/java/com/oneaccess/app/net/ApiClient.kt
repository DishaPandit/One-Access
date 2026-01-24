package com.oneaccess.app.net

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ApiClient(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun login(email: String): String {
        val bodyJson = JSONObject().put("email", email).toString()
        val req = Request.Builder()
            .url("$baseUrl/auth/login")
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("Login failed: ${resp.code} $body")
            val token = JSONObject(body).getString("accessToken")
            return token
        }
    }

    fun issueQrToken(accessToken: String, gateId: String, readerNonce: String, deviceId: String): QrTokenResponse {
        val bodyJson = JSONObject()
            .put("gateId", gateId)
            .put("readerNonce", readerNonce)
            .put("deviceId", deviceId)
            .toString()
        val req = Request.Builder()
            .url("$baseUrl/qr/token")
            .header("Authorization", "Bearer $accessToken")
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("Token failed: ${resp.code} $body")
            val json = JSONObject(body)
            return QrTokenResponse(
                token = json.getString("token"),
                expEpochSeconds = json.getLong("expEpochSeconds")
            )
        }
    }

    fun createDelegation(accessToken: String, delegateeEmail: String, gateIds: List<String>, hours: Int): String {
        val bodyJson = JSONObject()
            .put("delegateeEmail", delegateeEmail)
            .put("gateIds", gateIds)
            .put("hours", hours)
            .toString()
        val req = Request.Builder()
            .url("$baseUrl/delegation/create")
            .header("Authorization", "Bearer $accessToken")
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("Delegation failed: ${resp.code} $body")
            return JSONObject(body).getString("delegationId")
        }
    }

    fun createVisitorPass(accessToken: String, visitorName: String, visitorPhone: String, gateIds: List<String>, hours: Int): String {
        val bodyJson = JSONObject()
            .put("visitorName", visitorName)
            .put("visitorPhone", visitorPhone)
            .put("gateIds", gateIds)
            .put("hours", hours)
            .toString()
        val req = Request.Builder()
            .url("$baseUrl/visitor/create")
            .header("Authorization", "Bearer $accessToken")
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("Visitor pass failed: ${resp.code} $body")
            return JSONObject(body).getString("passId")
        }
    }

    fun getVisitorQrToken(passId: String, gateId: String, readerNonce: String): VisitorQrTokenResponse {
        val bodyJson = JSONObject()
            .put("passId", passId)
            .put("gateId", gateId)
            .put("readerNonce", readerNonce)
            .toString()
        val req = Request.Builder()
            .url("$baseUrl/visitor/token")
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("Visitor token failed: ${resp.code} $body")
            val json = JSONObject(body)
            return VisitorQrTokenResponse(
                token = json.getString("token"),
                expEpochSeconds = json.getLong("expEpochSeconds"),
                visitorName = json.getString("visitorName"),
                remainingUses = json.getInt("remainingUses")
            )
        }
    }
}

data class QrTokenResponse(
    val token: String,
    val expEpochSeconds: Long
)

data class VisitorQrTokenResponse(
    val token: String,
    val expEpochSeconds: Long,
    val visitorName: String,
    val remainingUses: Int
)


