package example.jllarraz.com.passportreader.proto

import example.jllarraz.com.passportreader.data.PassIdData
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import java.io.Closeable


typealias Retry = Boolean
typealias SendPersonalData = Boolean
typealias RequirePassIdDataCallback = suspend (challenge: PassIdProtoChallenge) -> PassIdData

class RethrownException(e: Throwable) : Throwable(e)

class PassIdClient(url: String, timeout: Long = 5000) : Closeable {

    var onConnectionFailed: (suspend () -> Retry)? = null  // called when sending request to server fails
    var onPersonalDataRequested: (suspend () -> SendPersonalData)? = null
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
            dg14File = data.dg14File!! // TODO: throw PassIdProtoError
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

        session = retriableCall({e: PassIdApiError? ->
            var dg1 : DG1File? = null
            if(e != null) {
                if(e.code != 428 || !(onPersonalDataRequested?.invoke() as Boolean)) {
                    throw RethrownException(e)
                }
                dg1 = data.dg1File
            }

            api.login(
                UserId.fromAAPublicKey(data.dg15File!!),
                challenge!!.id,
                data.ccSignatures!!,
                dg1
            )
        })

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

    private suspend fun<T> retriableCall(call: suspend(apiError: PassIdApiError?) -> T, e: PassIdApiError? = null) : T {
        try {
            return call(e)
        }
        catch (e: RpcConnectionError) {
            if(onConnectionFailed?.invoke() as Boolean){
                return retriableCall(call)
            }
            throw e
        }
        catch (e: RethrownException) {
            throw e.cause!!
        }
        catch (e: PassIdApiError) {
            return retriableCall(call, e)
        }
    }

    /* If api error exception is encountered, exception will be rethrown and not be passed to callback */
    private  suspend fun<T> retriableCall(call: suspend() -> T) : T {
        return retriableCall({ e: PassIdApiError? ->
            rethrowIfError(e)
            call()
        })
    }

    companion object {
        private fun rethrowIfError(e: Throwable?) {
            if(e != null) {
                throw  RethrownException(e)
            }
        }
    }
}