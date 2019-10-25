package example.jllarraz.com.passportreader.data

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable

import org.jmrtd.FeatureStatus
import org.jmrtd.VerificationStatus
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG15File

import java.util.ArrayList
import java.util.HashMap

class PassIdData : Parcelable {

    var sodFile: SODFile? = null
    var dg1File: DG1File? = null
    var dg14File: DG14File? = null
    var dg15File: DG15File? = null
    var ccSignatures: List<ByteArray>? = null // signatures made over chunks of challenge


    constructor(`in`: Parcel) {
        if (`in`.readInt() == 1) {
            sodFile = `in`.readSerializable() as SODFile
        }

        if (`in`.readInt() == 1) {
            dg1File = `in`.readSerializable() as DG1File
        }

        if (`in`.readInt() == 1) {
            dg14File = `in`.readSerializable() as DG14File
        }

        if (`in`.readInt() == 1) {
            dg15File = `in`.readSerializable() as DG15File
        }

        if (`in`.readInt() == 1) {
            ccSignatures = ArrayList()
            `in`.readList(ccSignatures, ByteArray::class.java.classLoader)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {

        dest.writeInt(if (sodFile != null) 1 else 0)
        if (sodFile != null) {
            dest.writeSerializable(sodFile)
        }

        dest.writeInt(if (dg1File != null) 1 else 0)
        if (dg1File != null) {
            dest.writeSerializable(dg1File)
        }

        dest.writeInt(if (dg14File != null) 1 else 0)
        if (dg14File != null) {
            dest.writeSerializable(dg14File)
        }

        dest.writeInt(if (dg15File != null) 1 else 0)
        if (dg15File != null) {
            dest.writeSerializable(dg15File)
        }

        dest.writeInt(if (ccSignatures != null) 1 else 0)
        if (ccSignatures != null) {
            dest.writeList(ccSignatures)
        }
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<PassIdData> {
            override fun createFromParcel(pc: Parcel): PassIdData {
                return PassIdData(pc)
            }

            override fun newArray(size: Int): Array<PassIdData?> {
                return arrayOfNulls(size)
            }
        }
    }
}
