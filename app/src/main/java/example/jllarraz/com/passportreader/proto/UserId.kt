package example.jllarraz.com.passportreader.proto

import android.os.Parcel
import android.os.Parcelable
import org.jmrtd.lds.icao.DG15File

import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA512tDigest


class UserId(userIdBytes: ByteArray) : ProtoByteObject(userIdBytes) {
    override fun fixedSize(): Int {
        return ripemd160.digestSize
    }

    companion object {
        private val ripemd160: RIPEMD160Digest = RIPEMD160Digest()

        fun fromhex(hexChallenge: String) : UserId = ProtoByteObject.fromhex(hexChallenge) {
            UserId(it)
        }

        fun fromBase64(b64Challenge: String) : UserId = ProtoByteObject.fromBase64(b64Challenge) {
            UserId(it)
        }

        fun fromAAPublicKey(dg15: DG15File) : UserId {
            val rawAAPubKey = dg15.publicKey.encoded

            val sha512_256 = SHA512tDigest(256)
            sha512_256.update(rawAAPubKey, 0, rawAAPubKey.size)
            var digest = ByteArray(sha512_256.digestSize)
            sha512_256.doFinal(digest, 0)

            ripemd160.update(digest, 0, digest.size)
            digest = ByteArray(ripemd160.digestSize)
            ripemd160.doFinal(digest, 0)

            return UserId(digest)
        }

        @JvmField
        val CREATOR : Parcelable.Creator<*> = object : Parcelable.Creator<ProtoByteObject>  {
            override fun createFromParcel(parcel: Parcel): ProtoByteObject {
                return fromParcel(parcel) {
                    UserId(it)
                }
            }

            override fun newArray(size: Int): Array<ProtoByteObject?> {
                return arrayOfNulls(size)
            }
        }
    }
}