package example.jllarraz.com.passportreader.proto

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


data class PassIdSession(val uid: UserId, val key: SessionKey, val expires: Date) {
    private val mac = Mac.getInstance("HmacSHA256")
    private var nonce: UInt = 0u

    init {
        val skey = SecretKeySpec(key.bytes(), mac.algorithm)
        mac.init(skey)
    }

    fun getMAC(apiName: String, rawParams: ByteArray): ByteArray {
        val msg = getEncodedNonce() + apiName.toByteArray(Charsets.US_ASCII) + rawParams
        incrementNonce()
        return mac.doFinal(msg)
    }

    private fun incrementNonce() {
        nonce++
    }

    private fun getEncodedNonce() : ByteArray {
        val bufferSize = UInt.SIZE_BYTES
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.order(ByteOrder.BIG_ENDIAN) // BIG_ENDIAN is default byte order, so it is not necessary.
        buffer.putInt(nonce.toInt())
        return buffer.array()
    }
}