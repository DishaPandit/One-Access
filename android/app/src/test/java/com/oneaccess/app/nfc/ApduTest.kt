package com.oneaccess.app.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApduTest {
    @Test
    fun parseGetTokenLcData() {
        val payload = "MAIN_GATE|0123456789ABCDEF".toByteArray(Charsets.US_ASCII)
        val apdu = byteArrayOf(
            0x00, 0xCA.toByte(), 0x00, 0x00,
            payload.size.toByte(),
            *payload
        )
        assertTrue(Apdu.isGetToken(apdu))
        val data = Apdu.extractLcData(apdu)!!
        assertEquals("MAIN_GATE|0123456789ABCDEF", Apdu.ascii(data))
    }
}

