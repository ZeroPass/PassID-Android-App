package example.jllarraz.com.passportreader.proto

import example.jllarraz.com.passportreader.data.PassIdData
import org.bouncycastle.asn1.cmp.Challenge
import org.jmrtd.lds.icao.DG14File
import java.io.Closeable
import java.io.IOException


typealias Retry = Boolean
typealias RequirePassIdDataCallback = suspend (challenge: PassIdProtoChallenge) -> PassIdData

class PassIdClient(url: String, timeout: Long = 5000) : Closeable {

    var onConnectionFailed: (suspend () -> Retry)? = null  // called when sending request to server fails
    var session: PassIdSession? = null

    var url: String
        set(url)  {
            api.url = url
        }
        get() {
            return api.url
        }

    var timeout: Long
        set(timeout)  {
            api.timeout = timeout
        }
        get() {
            return api.timeout
        }


    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun register( onRequirePassIdData: RequirePassIdDataCallback) {
        challenge = retriableCall{api.getChallenge()}
        val data = onRequirePassIdData(challenge!!)

        var dg14File: DG14File? = null
        if(data.dg15File!!.publicKey.algorithm != "RSA") {
            dg14File = data!!.dg14File // TODO: throw PassIdProtoError
        }

        session = retriableCall{
            api.register(
                data.dg15File!!,
                data.sodFile!!,
                challenge!!.id,
                data.ccSignatures!!,
                dg14File
        )}

        resetChallenge()
    }

    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun login(onRequirePassIdData: RequirePassIdDataCallback) {
        challenge = retriableCall{api.getChallenge()}
        val data = onRequirePassIdData(challenge!!)

        session = retriableCall{
            api.login(
                UserId.fromAAPublicKey(data.dg15File!!),
                challenge!!.id,
                data.ccSignatures!!
            )}

        resetChallenge()
    }

    private fun hasChallenge() : Boolean {
        return challenge != null
    }

    private fun resetChallenge() {
        challenge = null
    }

    override fun close() {
        if(hasChallenge()) {
            // TODO: if challenge is not null, notify server to cancel challenge
            resetChallenge()
        }
        session = null
        api.close()
    }

    private var challenge: PassIdProtoChallenge? = null
    private var api: PassIdApi = PassIdApi(url)

    init {
        api.timeout = timeout
    }

    private suspend fun<T> retriableCall(call: suspend () -> T) : T {
        try {
            return call()
        }
        catch (e: RpcConnectionError) {
            if(onConnectionFailed?.invoke() as Boolean){
                return retriableCall(call)
            }
            throw e
        }
    }
}