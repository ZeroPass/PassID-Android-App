package example.jllarraz.com.passportreader.utils

import android.util.Log
import example.jllarraz.com.passportreader.proto.PassIdProtoChallenge

import net.sf.scuba.smartcards.CardServiceException

import org.jmrtd.BACKey
import org.jmrtd.PACEKeySpec
import org.jmrtd.PassportService

import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.cert.Certificate
import java.util.ArrayList
import java.util.TreeSet

import org.jmrtd.FeatureStatus
import org.jmrtd.VerificationStatus
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.COMFile
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG15File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.MRZInfo
import org.jmrtd.protocol.BACResult
import org.jmrtd.protocol.PACEResult

class PassIdPassportNFCError(msg: String): IOException(msg)

class PassIdPassportNFC @Throws(GeneralSecurityException::class, PassIdPassportNFCError::class)
private constructor() {

    /**
     * Gets the supported features (such as: BAC, AA, EAC) as
     * discovered during initialization of this document.
     *
     * @return the supported features
     *
     * @since 0.4.9
     */
    /* The feature status has been created in constructor. */
    val features: FeatureStatus

    /**
     * Gets the verification status thus far.
     *
     * @return the verification status
     *
     * @since 0.4.9
     */
    val verificationStatus: VerificationStatus

    private var service: PassportService?=null

    var sodFile: SODFile? = null
        private set
    var dg1File: DG1File? = null
        private set
    var dg14File: DG14File? = null
        private set
    var dg15File: DG15File? = null
        private set
    var ccSignatures: List<ByteArray>? = null // signatures made over chunks of challenge
        private set


    init {
        this.features = FeatureStatus()
        this.verificationStatus = VerificationStatus()
    }


    /**
     * Creates a document by reading it from a service.
     *
     * @param ps the service to read from
     * @param trustManager the trust manager (CSCA, CVCA)
     * @param mrzInfo the BAC entries
     *
     * @throws CardServiceException on error
     * @throws GeneralSecurityException if certain security primitives are not supported
     */
    @Throws(CardServiceException::class, GeneralSecurityException::class, PassIdPassportNFCError::class)
    constructor(ps: PassportService?, mrzInfo: MRZInfo, challange: PassIdProtoChallenge?) : this() {
        requireNotNull(ps) { "Service cannot be null" }

        this.service = ps

        val hasSAC: Boolean
        var isSACSucceeded = false
        var paceResult: PACEResult? = null
        try {
            (service as PassportService).open()

            /* Find out whether this MRTD supports SAC. */
            try {
                Log.i(TAG, "Inspecting card access file")
                val cardAccessFile = CardAccessFile(ps.getInputStream(PassportService.EF_CARD_ACCESS))
                val securityInfos = cardAccessFile.securityInfos
                for (securityInfo in securityInfos) {
                    if (securityInfo is PACEInfo) {
                        features.setSAC(FeatureStatus.Verdict.PRESENT)
                    }
                }
            } catch (e: Exception) {
                /* NOTE: No card access file, continue to test for BAC. */
                Log.i(TAG, "DEBUG: failed to get card access file: " + e.message)
                e.printStackTrace()
            }

            hasSAC = features.hasSAC() == FeatureStatus.Verdict.PRESENT

            if (hasSAC) {
                try {
                    paceResult = doPACE(ps, mrzInfo)
                    isSACSucceeded = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.i(TAG, "PACE failed, falling back to BAC")
                    isSACSucceeded = false
                }

            }
            (service as PassportService).sendSelectApplet(isSACSucceeded)
        } catch (cse: CardServiceException) {
            throw cse
        } catch (e: Exception) {
            e.printStackTrace()
            throw CardServiceException("Cannot open document. " + e.message)
        }

        /* Find out whether this MRTD supports BAC. */
        try {
            /* Attempt to read EF.COM before BAC. */
            COMFile((service as PassportService).getInputStream(PassportService.EF_COM))

            if (isSACSucceeded) {
                verificationStatus.setSAC(VerificationStatus.Verdict.SUCCEEDED, "Succeeded")
                features.setBAC(FeatureStatus.Verdict.UNKNOWN)
                verificationStatus.setBAC(VerificationStatus.Verdict.NOT_CHECKED, "Using SAC, BAC not checked", EMPTY_TRIED_BAC_ENTRY_LIST)
            } else {
                /* We failed SAC, and we failed BAC. */
                features.setBAC(FeatureStatus.Verdict.NOT_PRESENT)
                verificationStatus.setBAC(VerificationStatus.Verdict.NOT_PRESENT, "Non-BAC document", EMPTY_TRIED_BAC_ENTRY_LIST)
            }
        } catch (e: Exception) {
            Log.i(TAG, "Attempt to read EF.COM before BAC failed with: " + e.message)
            features.setBAC(FeatureStatus.Verdict.PRESENT)
            verificationStatus.setBAC(VerificationStatus.Verdict.NOT_CHECKED, "BAC document", EMPTY_TRIED_BAC_ENTRY_LIST)
        }

        /* If we have to do BAC, try to do BAC. */
        val hasBAC = features.hasBAC() == FeatureStatus.Verdict.PRESENT

        if (hasBAC && !(hasSAC && isSACSucceeded)) {
            val bacKey = BACKey(mrzInfo.documentNumber, mrzInfo.dateOfBirth, mrzInfo.dateOfExpiry)
            val triedBACEntries = ArrayList<BACKey>()
            triedBACEntries.add(bacKey)
            try {
                doBAC(service as PassportService, mrzInfo)
                verificationStatus.setBAC(VerificationStatus.Verdict.SUCCEEDED, "BAC succeeded with key $bacKey", triedBACEntries)
            } catch (e: Exception) {
                verificationStatus.setBAC(VerificationStatus.Verdict.FAILED, "BAC failed", triedBACEntries)
            }

        }


        /* Pre-read these files that are always present. */

        val dgNumbersAlreadyRead = TreeSet<Int>()

        try {
            sodFile = getSodFile(ps)
            dg1File = getDG1File(ps)
            dgNumbersAlreadyRead.add(1)
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            Log.w(TAG, "Could not read file")
        }

        try {
            dg14File = getDG14File(ps)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        /* Get the list of DGs from EF.SOd, we don't trust EF.COM. */
        val dgNumbers = ArrayList<Int>()
        if (sodFile != null) {
            dgNumbers.addAll(sodFile!!.dataGroupHashes.keys)
        }
        // TODO: else throw an exception, because we need SOD file for passId

        dgNumbers.sort() /* NOTE: need to sort it, since we get keys as a set. */

        Log.i(TAG, "Found DGs: $dgNumbers")

        /* Check AA support by DG15 presence. */
        if (dgNumbers.contains(15)) {
            features.setAA(FeatureStatus.Verdict.PRESENT)
        } else {
            features.setAA(FeatureStatus.Verdict.NOT_PRESENT)
        }
        val hasAA = features.hasAA() == FeatureStatus.Verdict.PRESENT
        if (hasAA) {
            try {
                dg15File = getDG15File(ps)
                dgNumbersAlreadyRead.add(15)
            } catch (ioe: IOException) {
                ioe.printStackTrace()
                Log.w(TAG, "Could not read file")
            } catch (e: Exception) {
                verificationStatus.setAA(VerificationStatus.Verdict.NOT_CHECKED, "Failed to read DG15")
            }

        } else {
            // TODO: Passport doesn't support crucial function for passID, throw an exception
            /* Feature status says: no AA, so verification status should say: no AA. */
            verificationStatus.setAA(VerificationStatus.Verdict.NOT_PRESENT, "AA is not supported")
        }

        signChallenge(challange)
    }

    private fun signChallenge(challenge: PassIdProtoChallenge?) {
        if (challenge != null) {
            val ccsigs = ArrayList<ByteArray>()
            for(cc in challenge.getChunks()) {
                val aaResult = (service as PassportService)
                        .doAA(dg15File!!.publicKey, sodFile!!.digestAlgorithm, sodFile!!.signerInfoDigestAlgorithm, cc)

                ccsigs.add(aaResult.response)
                Log.i(TAG, "Challenge chunk: " + StringUtils.bytesToHex(cc))
                Log.i(TAG, "    chunk sig: " + StringUtils.bytesToHex(aaResult.response))
            }

            ccSignatures = ccsigs
        }
    }

    ////////////////////////////

    @Throws(IOException::class, CardServiceException::class, GeneralSecurityException::class)
    private fun doPACE(ps: PassportService, mrzInfo: MRZInfo): PACEResult? {
        var paceResult: PACEResult? = null
        var isCardAccessFile: InputStream? = null
        try {
            val bacKey = BACKey(mrzInfo.documentNumber, mrzInfo.dateOfBirth, mrzInfo.dateOfExpiry)
            val paceKeySpec = PACEKeySpec.createMRZKey(bacKey)
            isCardAccessFile = ps.getInputStream(PassportService.EF_CARD_ACCESS)

            val cardAccessFile = CardAccessFile(isCardAccessFile)
            val securityInfos = cardAccessFile.securityInfos
            val securityInfo = securityInfos.iterator().next()
            val paceInfos = ArrayList<PACEInfo>()
            if (securityInfo is PACEInfo) {
                paceInfos.add(securityInfo)
            }

            if (paceInfos.size > 0) {
                val paceInfo = paceInfos.iterator().next()
                paceResult = ps.doPACE(paceKeySpec, paceInfo.objectIdentifier, PACEInfo.toParameterSpec(paceInfo.parameterId))
            }
        } finally {
            if (isCardAccessFile != null) {
                isCardAccessFile.close()
                isCardAccessFile = null
            }
        }
        return paceResult
    }

    @Throws(CardServiceException::class)
    private fun doBAC(ps: PassportService, mrzInfo: MRZInfo): BACResult {
        val bacKey = BACKey(mrzInfo.documentNumber, mrzInfo.dateOfBirth, mrzInfo.dateOfExpiry)
        return ps.doBAC(bacKey)
    }

    @Throws(CardServiceException::class, IOException::class)
    private fun getSodFile(ps: PassportService): SODFile {
        //SOD FILE
        var isSodFile: InputStream? = null
        try {
            isSodFile = ps.getInputStream(PassportService.EF_SOD)
            return LDSFileUtil.getLDSFile(PassportService.EF_SOD, isSodFile) as SODFile
        } finally {
            if (isSodFile != null) {
                isSodFile.close()
                isSodFile = null
            }
        }
    }

    @Throws(CardServiceException::class, IOException::class)
    private fun getDG1File(ps: PassportService): DG1File {
        // Basic data
        var isDG1: InputStream? = null
        try {
            isDG1 = ps.getInputStream(PassportService.EF_DG1)
            return LDSFileUtil.getLDSFile(PassportService.EF_DG1, isDG1) as DG1File
        } finally {
            if (isDG1 != null) {
                isDG1.close()
                isDG1 = null
            }
        }
    }

    @Throws(CardServiceException::class, IOException::class)
    private fun getDG14File(ps: PassportService): DG14File {
        // Basic data
        var isDG14: InputStream? = null
        try {
            isDG14 = ps.getInputStream(PassportService.EF_DG14)
            return LDSFileUtil.getLDSFile(PassportService.EF_DG14, isDG14) as DG14File
        } finally {
            if (isDG14 != null) {
                isDG14.close()
                isDG14 = null
            }
        }
    }

    @Throws(CardServiceException::class, IOException::class)
    private fun getDG15File(ps: PassportService): DG15File {
        // Basic data
        var isDG15: InputStream? = null
        try {
            isDG15 = ps.getInputStream(PassportService.EF_DG15)
            return LDSFileUtil.getLDSFile(PassportService.EF_DG15, isDG15) as DG15File
        } finally {
            if (isDG15 != null) {
                isDG15.close()
                isDG15 = null
            }
        }
    }

    companion object {

        private val TAG = PassIdPassportNFC::class.java.simpleName

        private val EMPTY_TRIED_BAC_ENTRY_LIST = emptyList<BACKey>()
        private val EMPTY_CERTIFICATE_CHAIN = emptyList<Certificate>()
    }

}
