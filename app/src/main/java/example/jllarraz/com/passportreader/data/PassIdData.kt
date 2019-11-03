package example.jllarraz.com.passportreader.data

import android.os.Parcel
import android.os.Parcelable

import org.jmrtd.lds.LDSFile
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG15File
import java.io.ByteArrayInputStream

import java.util.ArrayList

class PassIdData : Parcelable {

    var sodFile: SODFile? = null
    var dg1File: DG1File? = null
    var dg14File: DG14File? = null
    var dg15File: DG15File? = null
    var ccSignatures: List<ByteArray>? = null // signatures made over chunks of challenge


    constructor(`in`: Parcel) {

        if (`in`.readInt() == 1) {
            sodFile = `in`.readDGFile{ SODFile(it) }
        }

        if (`in`.readInt() == 1) {
            dg1File = `in`.readDGFile{ DG1File(it) }
        }

        if (`in`.readInt() == 1) {
            dg14File = `in`.readDGFile{ DG14File(it) }
        }

        if (`in`.readInt() == 1) {
            dg15File = `in`.readDGFile{ DG15File(it) }
        }

        if (`in`.readInt() == 1) {
            ccSignatures = ArrayList()
            `in`.readList(ccSignatures, ByteArray::class.java.classLoader)
        }
    }

    constructor() {
        ccSignatures = ArrayList()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {

        out.writeInt(if (sodFile != null) 1 else 0)
        if (sodFile != null) {
            out.writeDGFile(sodFile!!)
        }

        out.writeInt(if (dg1File != null) 1 else 0)
        if (dg1File != null) {
            out.writeDGFile(dg1File!!)
        }

        out.writeInt(if (dg14File != null) 1 else 0)
        if (dg14File != null) {
            out.writeDGFile(dg14File!!)
        }

        out.writeInt(if (dg15File != null) 1 else 0)
        if (dg15File != null) {
            out.writeDGFile(dg15File!!)
        }

        out.writeInt(if (ccSignatures != null) 1 else 0)
        if (ccSignatures != null) {
            out.writeList(ccSignatures)
        }
    }

    companion object {
        private fun Parcel.writeDGFile(dg: LDSFile) {
            // Note: dest.writeSerializable(dg) shouldn't be used here
            //      because some files are not properly serialized
            writeByteArray(dg.encoded)
        }

        private fun<T> Parcel.readDGFile(factory: (ByteArrayInputStream) -> T) : T {
            // Note: dest.readSerializable() as T shouldn't be used here
            //       because some files are not properly deserialized.
            //       For example SODFile is missing signedData
            val raw = createByteArray()
            return factory(raw!!.inputStream())
        }

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