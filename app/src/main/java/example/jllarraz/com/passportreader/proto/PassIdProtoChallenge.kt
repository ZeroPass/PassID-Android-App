package example.jllarraz.com.passportreader.proto
import android.os.Parcel
import android.os.Parcelable
import example.jllarraz.com.passportreader.utils.StringUtils.bytesToHex
import example.jllarraz.com.passportreader.utils.StringUtils.hexToBytes
import java.util.ArrayList
import java.nio.ByteBuffer


/** Challenge Id */
class CID(cid: Int) {
    private val cid: Int

    init {
        require(cid <= 0xFFFFFFFF) { "Wrong CID value" }
        this.cid = cid
    }

    fun toNumber() : Int {
        return cid
    }

    fun toBytes() : ByteArray {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.putInt(cid)
        buffer.flip()// flip, to be big-endian
        return buffer.array()
    }

    fun hex() : String {
        return bytesToHex(toBytes())
    }

    override fun equals(other: Any?) : Boolean
            = (other is CID)
            && cid == other.cid

    override fun hashCode(): Int {
        return cid.hashCode()
    }

    companion object {

        fun fromhex(hexCid: String) : CID {
            return fromBytes(hexToBytes(hexCid))
        }

        fun fromBytes(rawCid: ByteArray) : CID {
            // expects big endian raw cid
            require(rawCid.size == 4) { "Wrong cid bytes size" }
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
            buffer.put(rawCid)
            buffer.flip() // flip it to little-endian
            return CID(buffer.getInt(0))
        }
    }
}


class PassIdProtoChallenge(challenge: ByteArray) : ProtoByteObject(challenge) {
    override fun fixedSize(): Int {
        return 32
    }

    val id: CID
        get() {
            val cid = data.copyOfRange(0,4)
            return CID.fromBytes(cid)
        }

    fun getChunks() : List<ByteArray> {
        val numChunks = data.size / CHUNK_SIZE
        val ccs = ArrayList<ByteArray>()
        for(i in 0..numChunks){
            ccs.add(getChunk(i))
        }
        return ccs
    }

    private fun getChunk(chunkNum: Int) : ByteArray {
        return data.copyOfRange(
                chunkNum * CHUNK_SIZE,
                (chunkNum * CHUNK_SIZE) + CHUNK_SIZE
        )
    }

    companion object {
        private const val CHUNK_SIZE = 8

        fun fromhex(hexChallenge: String) : PassIdProtoChallenge = ProtoByteObject.fromhex(hexChallenge) {
            PassIdProtoChallenge(it)
        }

        fun fromBase64(b64Challenge: String) : PassIdProtoChallenge = ProtoByteObject.fromBase64(b64Challenge) {
            PassIdProtoChallenge(it)
        }

        @JvmField
        val CREATOR : Parcelable.Creator<*> = object : Parcelable.Creator<PassIdProtoChallenge>  {
            override fun createFromParcel(parcel: Parcel): PassIdProtoChallenge {
                return fromParcel(parcel) {
                    PassIdProtoChallenge(it)
                }
            }

            override fun newArray(size: Int): Array<PassIdProtoChallenge?> {
                return arrayOfNulls(size)
            }
        }
    }
}