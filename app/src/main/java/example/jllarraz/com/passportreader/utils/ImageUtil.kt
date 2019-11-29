package example.jllarraz.com.passportreader.utils

import android.graphics.*

import com.gemalto.jp2.JP2Decoder

import org.jnbis.internal.WsqDecoder

import java.io.IOException
import java.io.InputStream

object ImageUtil {
    var JPEG2000_MIME_TYPE = "image/jp2"
    var JPEG2000_ALT_MIME_TYPE = "image/jpeg2000"
    var WSQ_MIME_TYPE = "image/x-wsq"

    @Throws(IOException::class)
    fun decodeImage(inputStream: InputStream, imageLength: Int, mimeType: String): Bitmap {
        if (JPEG2000_MIME_TYPE.equals(mimeType, ignoreCase = true) || JPEG2000_ALT_MIME_TYPE.equals(mimeType, ignoreCase = true)) {
            val bitmap = JP2Decoder(inputStream).decode();
            return bitmap
        } else if (WSQ_MIME_TYPE.equals(mimeType, ignoreCase = true)) {
            val wsqDecoder = WsqDecoder()
            val bitmap = wsqDecoder.decode(inputStream.readBytes())
            val byteData = bitmap.pixels
            val intData = IntArray(byteData.size)
            for (j in byteData.indices) {
                intData[j] = -0x1000000 or ((byteData[j].toInt() and 0xFF) shl 16) or ((byteData[j].toInt() and 0xFF) shl 8) or (byteData[j].toInt() and 0xFF)
            }
            return Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        } else {
            return BitmapFactory.decodeStream(inputStream)
        }
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}