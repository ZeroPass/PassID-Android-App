package example.jllarraz.com.passportreader.utils

import org.jmrtd.lds.icao.DG14File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 *  Wrapper around DG14File class to store raw data read from input stream into local member.
 *  It fixes serializing to output stream the elements of SecurityInfos to match the order from
 *  the input stream. With this fix the correct hash can be computed which should match the one
 *  stored in the SOD file.
 */
class DG14FileView(inputStream: InputStream) : DG14File(inputStream) {
    private var data: ByteArray? = null

    @Throws(IOException::class)
    override fun readContent(inputStream: InputStream) {
        data = inputStream.readBytes()
        super.readContent(data?.inputStream())
    }

    @Throws(IOException::class)
    override fun writeContent(outputStream: OutputStream) {
        outputStream.write(data)
    }
}