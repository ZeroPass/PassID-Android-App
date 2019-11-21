package example.jllarraz.com.passportreader.proto
import android.os.Parcel
import android.os.Parcelable
import example.jllarraz.com.passportreader.utils.StringUtils.bytesToHex
import example.jllarraz.com.passportreader.utils.StringUtils.hexToBytes
import example.jllarraz.com.passportreader.utils.StringUtils.b64Encode
import example.jllarraz.com.passportreader.utils.StringUtils.b64Decode


abstract class ProtoByteObject(data: ByteArray) : Parcelable {

    protected abstract fun fixedSize(): Int
    protected lateinit var data: ByteArray

    init {
        assing(data)
    }

    override fun equals(other: Any?) : Boolean
            = (other is ProtoByteObject)
            && data.contentEquals(other.data)

    fun toBase64() : String {
        return b64Encode(data)
    }

    fun hex() : String {
        return bytesToHex(data)
    }

    fun bytes() : ByteArray {
        return data
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByteArray(data)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }

    fun assing(data: ByteArray) {
        require(!(fixedSize() > 0 && data.size != fixedSize())) { "Wrong data size" }
        this.data = data
    }

    companion object {
        @JvmStatic
        protected fun <T>fromhex(hexStr: String, factory: (ByteArray) -> T) : T {
            return  factory(hexToBytes(hexStr))
        }

        @JvmStatic
        protected fun <T>fromBase64(b64Str: String, factory: (ByteArray) -> T) : T {
            return  factory(b64Decode(b64Str))
        }

        @JvmStatic
        protected fun <T>fromParcel(`in`: Parcel, factory: (ByteArray) -> T) : T {
            return  factory(`in`.createByteArray()!!)
        }
    }
}