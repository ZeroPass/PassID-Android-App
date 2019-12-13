package example.jllarraz.com.passportreader.proto

import android.util.Log
import example.jllarraz.com.passportreader.PassIdApp
import example.jllarraz.com.passportreader.utils.DG14FileView
import info.laht.yajrpc.RpcError
import info.laht.yajrpc.RpcParams
import info.laht.yajrpc.RpcNoParams
import info.laht.yajrpc.RpcResponse
import kotlinx.coroutines.*
import java.lang.Exception

import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG15File

import example.jllarraz.com.passportreader.utils.StringUtils.b64Encode
import okhttp3.internal.lockAndWaitNanos
import org.jmrtd.lds.icao.DG1File
import java.io.Closeable
import java.util.*
import java.util.concurrent.Future

data class PassIdApiError(val code: Int, override val message: String, val data: Any? = null): Exception(message)

class PassIdApi(url: String) : Closeable {

    private var rpc: JsonRpcClient
    var timeout: Long = 2000
    var url: String
        set(url) {
            rpc.url = url
            PassIdApp.allowedHost = url
        }
        get() {
            return rpc.url
        }

    init {
        PassIdApp.allowedHost = url
        val httpClient = PassIdApp.getHttpClient()
        this.rpc = JsonRpcClient(httpClient, url)
    }

    override fun close() {
        this.rpc.close()
    }


    /******************************************** API CALLS *****************************************************/
    /************************************************************************************************************/

    /* API: passID.ping */
    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun ping(): Int {
        Log.d(TAG, "Sending ping request ...")

        val param = RpcParams.mapParams(mapOf(
            "ping" to Random().nextInt()
        ))

        val resp = transceive(getApiMethod("ping"), param)
        requireRespField("pong", resp)

        return (resp.getValue("pong") as Number).toInt()
    }

    /* API: passID.getChallenge */
    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun getChallenge(): PassIdProtoChallenge {
        Log.d(TAG, "Requesting challenge from server ...")

        val resp = transceive(getApiMethod("getChallenge"))
        requireRespField("challenge", resp)

        return PassIdProtoChallenge.fromBase64(resp.getValue("challenge") as String)
    }

    /* API: passID.cancelChallenge */
    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    fun cancelChallenge(challenge: PassIdProtoChallenge) {
        Log.d(TAG, "Canceling challenge request ...")
        val param = RpcParams.mapParams(mapOf(
            "challenge" to challenge.toBase64()
        ))
        rpc.notify(getApiMethod("cancelChallenge"), param)
    }

    /* API: passID.register */
    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun register(dg15: DG15File, sod: SODFile, cid: CID, csigs: List<ByteArray>, dg14: DG14FileView? = null): PassIdSession {
        Log.d(TAG, "Requesting registration session from server ...")

        val params = RpcParams.mapParams(listOfNotNull(
            "dg15" to b64Encode(dg15.encoded),
            "sod" to b64Encode(sod.encoded),
            "cid" to cid.hex(),
            "csigs" to csigs.map { b64Encode(it) },
            if (dg14 != null) "dg14" to b64Encode(dg14.encoded) else null
        ).toMap())

        val resp = transceive(getApiMethod("register"), params)
        requireRespField("uid", resp)
        requireRespField("session_key", resp)
        requireRespField("expires", resp)

        val uid = UserId.fromBase64(resp.getValue("uid") as String)
        val sessionKey = SessionKey.fromBase64(resp.getValue("session_key") as String)
        val expires = fromTimestamp(resp.getValue("expires"))
        return PassIdSession(uid, sessionKey, expires)
    }

    /* API: passID.login */
    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun login(uid: UserId, cid: CID, csigs: List<ByteArray>, dg1: DG1File? = null) : PassIdSession {
        Log.d(TAG, "Requesting login session from server ...")

        val params = RpcParams.mapParams(listOfNotNull(
            "uid" to uid.toBase64(),
            "cid" to cid.hex(),
            "csigs" to csigs.map{ b64Encode(it) },
            if (dg1 != null) "dg1" to b64Encode(dg1.encoded) else null
        ).toMap())

        val resp = transceive(getApiMethod("login"), params)
        requireRespField("session_key",resp)
        requireRespField("expires",resp)

        val sessionKey = SessionKey.fromBase64(resp.getValue("session_key") as String)
        val expires = fromTimestamp(resp.getValue("expires"))
        return PassIdSession(uid, sessionKey, expires)
    }

    /* API: passID.sayHello */
    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    suspend fun sayHello(s: PassIdSession) : String {
        Log.d(TAG, "Requesting login session from server ...")

        val mac  = s.getMAC("sayHello", s.uid.bytes())
        val params = RpcParams.mapParams(listOfNotNull(
            "uid" to s.uid.toBase64(),
            "mac" to b64Encode(mac)
        ).toMap())

        val resp = transceive(getApiMethod("sayHello"), params)
        requireRespField("msg",resp)
        return resp.getValue("msg") as String
    }

/************************************************************************************************************/
/************************************************************************************************************/

    @Throws(PassIdApiError::class, RpcConnectionTimeout::class, RpcConnectionError::class)
    private suspend inline fun transceive(method: String, params: RpcParams = RpcNoParams): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val resp: RpcResponse = rpc.write(method, params, timeout).await()
            if (resp.hasError) {
                throw resp.error!!.toPassIdApiError()
            }
            resp.getResult<Map<String, Any>>()!!
        }
    }
    
    companion object {
        private val TAG = PassIdApi::class.java.simpleName

        private fun getApiMethod(name: String) : String {
            return "passID.$name"
        }

        private fun fromTimestamp(timestamp: Any) : Date{
            return Date((timestamp as Number).toLong() * 1000)
        }

        private fun requireRespField(field: String, mapResp: Map<String, Any>){
            if (!mapResp.contains(field)) {
                Log.e(TAG, "Missing required field '$field' in response!")
                throw PassIdApiError(500, "Received bad response from server")
            }
        }

        private fun RpcError.toPassIdApiError(): PassIdApiError {
            var field = javaClass.getDeclaredField("code")
            field.isAccessible = true
            val code = field.getInt(this)

            field = javaClass.getDeclaredField("message")
            field.isAccessible = true
            val msg = field.get(this) as String

            field = javaClass.getDeclaredField("data")
            field.isAccessible = true
            val data = field.get(this)

            return PassIdApiError(code, msg, data)
        }

        private suspend fun Future<RpcResponse>.await() : RpcResponse {
            return withContext(Dispatchers.IO) {
                while(!isDone){
                    lockAndWaitNanos(100000000) // 100 ms
                    if(!isActive) {
                        cancel(true) // stop the underlying task because parent task was canceled
                    }
                    yield()
                }
                getOrThrow()
            }
        }

        private fun Future<RpcResponse>.getOrThrow() : RpcResponse {
            // RpcConnection error wrapper in case get throws and exception
            try {
                return get()
            }
            catch(e: Exception){
                if(e.cause is RpcConnectionTimeout || e.cause is RpcConnectionError) {
                    throw e.cause!!
                }
                throw e
            }
        }
    }
}