package example.jllarraz.com.passportreader.utils

import android.util.Patterns
import android.util.Base64

object StringUtils {
    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v.ushr(4)]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun hexToBytes(hexString: String) = ByteArray(hexString.length / 2) {
        hexString.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }

    fun b64Encode(bytes: ByteArray) : String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun b64Decode(b64String: String): ByteArray = Base64.decode(b64String, Base64.NO_WRAP)

    fun isValidHttpUrl(url: String): Boolean {
        val valid = Patterns.WEB_URL.matcher(url).matches()
        return valid && "^https?://".toRegex().containsMatchIn(url)
    }
}
