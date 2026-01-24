package com.oneaccess.app.nfc

object Apdu {
    val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())

    fun isSelectAid(apdu: ByteArray): Boolean {
        // SELECT by DF name: 00 A4 04 00 Lc <AID> 00?
        if (apdu.size < 6) return false
        return apdu[0] == 0x00.toByte() &&
            apdu[1] == 0xA4.toByte() &&
            apdu[2] == 0x04.toByte() &&
            apdu[3] == 0x00.toByte()
    }

    fun isGetToken(apdu: ByteArray): Boolean {
        // proprietary INS 0xCA
        if (apdu.size < 4) return false
        return apdu[0] == 0x00.toByte() && apdu[1] == 0xCA.toByte()
    }

    fun extractLcData(apdu: ByteArray): ByteArray? {
        // Basic case: CLA INS P1 P2 Lc Data...
        if (apdu.size < 5) return null
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return null
        return apdu.copyOfRange(5, 5 + lc)
    }

    fun ascii(bytes: ByteArray): String = bytes.toString(Charsets.US_ASCII)
}

