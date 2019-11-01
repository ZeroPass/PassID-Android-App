package example.jllarraz.com.passportreader.proto

import android.os.Parcel
import android.os.Parcelable

class SessionKey(key: ByteArray) : ProtoByteObject(key) {
    override fun fixedSize(): Int {
        return 32
    }

    companion object {

        fun fromhex(hexChallenge: String): SessionKey = ProtoByteObject.fromhex(hexChallenge) {
            SessionKey(it)
        }

        fun fromBase64(b64Challenge: String) : SessionKey = ProtoByteObject.fromBase64(b64Challenge) {
            SessionKey(it)
        }

        @JvmField
        val CREATOR : Parcelable.Creator<*> = object : Parcelable.Creator<SessionKey>  {
            override fun createFromParcel(parcel: Parcel): SessionKey {
                return fromParcel(parcel) {
                    SessionKey(it)
                }
            }

            override fun newArray(size: Int): Array<SessionKey?> {
                return arrayOfNulls(size)
            }
        }
    }
}