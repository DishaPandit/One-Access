package com.oneaccess.app.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.oneaccess.app.net.ApiClient
import com.oneaccess.app.ui.AppState

/**
 * HCE service:
 * - SELECT AID: acknowledge
 * - GET_TOKEN (INS 0xCA): reader sends ASCII "GATE_ID|READER_NONCE"
 *   we call backend /hce/token and return ASCII JWT token + 9000
 */
class OneAccessHceService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return Apdu.SW_CONDITIONS_NOT_SATISFIED

        return try {
            when {
                Apdu.isSelectAid(commandApdu) -> {
                    // Minimal response: just SW_OK
                    Apdu.SW_OK
                }
                Apdu.isGetToken(commandApdu) -> {
                    val data = Apdu.extractLcData(commandApdu) ?: return Apdu.SW_CONDITIONS_NOT_SATISFIED
                    val payload = Apdu.ascii(data)
                    val parts = payload.split("|")
                    if (parts.size != 2) return Apdu.SW_CONDITIONS_NOT_SATISFIED
                    val gateId = parts[0].trim()
                    val readerNonce = parts[1].trim()

                    val accessToken = AppState.accessToken(this) ?: return Apdu.SW_CONDITIONS_NOT_SATISFIED
                    val baseUrl = AppState.backendUrl(this).trim().trimEnd('/')
                    val deviceId = AppState.deviceId(this)

                    val api = ApiClient(baseUrl)
                    val token = api.issueHceToken(
                        accessToken = accessToken,
                        gateId = gateId,
                        readerNonce = readerNonce,
                        deviceId = deviceId
                    )

                    val tokenBytes = token.toByteArray(Charsets.US_ASCII)
                    tokenBytes + Apdu.SW_OK
                }
                else -> Apdu.SW_CONDITIONS_NOT_SATISFIED
            }
        } catch (t: Throwable) {
            Log.w("OneAccessHce", "APDU processing failed", t)
            Apdu.SW_CONDITIONS_NOT_SATISFIED
        }
    }

    override fun onDeactivated(reason: Int) {
        // No-op (MVP)
    }
}

