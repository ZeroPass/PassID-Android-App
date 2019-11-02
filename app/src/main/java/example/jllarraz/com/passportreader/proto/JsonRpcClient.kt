package example.jllarraz.com.passportreader.proto

import android.util.Log
import info.laht.yajrpc.*
import info.laht.yajrpc.net.RpcClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.util.*
import java.util.concurrent.*



typealias RpcCallback = (RpcResponse?, IOException?) -> Unit

typealias RpcConnectionError = IOException
class RpcConnectionTimeout(e: String) : RpcConnectionError(e)


class JsonRpcClient  constructor(
    private val client: OkHttpClient,
    var url: String = ""
) : RpcClient {

    var userAgent: String? = null
    var origin: String? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val callbacks = mutableMapOf<String, RpcCallback>()

    override fun notify(methodName: String, params: RpcParams) {
        send(RpcRequestOut(methodName, params).apply {
            id = UUID.randomUUID().toString()
        })
        var zz: Map<String, String>
    }

    @Throws(TimeoutException::class)
    override fun write(methodName: String, params: RpcParams, timeout: Long): Future<RpcResponse> {
        fun task(): () -> RpcResponse = {
            var reqid = ""
            try {
                var response: RpcResponse? = null
                var ioError: IOException? = null

                val latch = CountDownLatch(1)
                val request = RpcRequestOut(methodName, params).apply {
                    id = UUID.randomUUID().toString()
                    reqid = id.toString()
                    callbacks[reqid] = { resp: RpcResponse?, error: IOException? ->
                        response = resp
                        ioError = error
                        latch.countDown()
                    }
                }

                val call = send(request)
                if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                    call.cancel()
                    throw RpcConnectionTimeout("Timeout!")
                }
                else if(ioError != null) {
                    throw RpcConnectionError(ioError)
                }

                response!!
            }
            finally {
                callbacks.remove(reqid)
            }
        }

        return executor.submit(task())
    }

    private fun post(rpcReq: RpcRequestOut, callback: Callback): Call {
        val body = rpcReq.toRequestBody()
        val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .addHeader("Content-type","application/json")
                .addUserAgent(userAgent)
                .addOrigin(origin)
                .addContentLength(body.contentLength())
                .post(body)
                .build()

        val call = client.newCall(req)
        call.enqueue(callback)
        return call
    }

    private fun send(request: RpcRequestOut): Call {
        return post(request, object: Callback {
            override fun onResponse(call: Call, resp: Response) {
                val rpcResp = resp.toRpcResponse()
                call(rpcResp.id, rpcResp)
            }
            override fun onFailure(call: Call, e: IOException) {
                val r = call.request()
                if(!e.message.equals("Canceled", ignoreCase = true)) {
                    val req = call.request().toRpcRequest()
                    call(req.id, null, e)
                }
            }
        })
    }

    override fun close() {
        executor.shutdown()
    }

    private fun call(rpcId: Any?, resp: RpcResponse?, error: IOException? = null) {
        if(rpcId != null) { // id can be null
            val id = rpcId.toString()
            if (callbacks.contains(id)) {
                callbacks[id]?.also { callback ->
                    callback.invoke(resp, error)
                }
            }
        }
        else {
            Log.e(TAG, "Could not call callback, id not found")
            if(error != null) {
                Log.e(TAG, "ioerror: ${error.message}")
            }else{
                Log.e(TAG, "rpc resp: ${resp.toString()}")
            }
        }
    }

    companion object {
        private val TAG = JsonRpcClient::class.java.simpleName
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private fun Request.Builder.addOrigin(origin: String?): Request.Builder {
            if (origin != null) {
                this.addHeader("Origin", origin)
            }
            return this
        }

        private fun Request.Builder.addUserAgent(agent: String?): Request.Builder {
            if (agent != null) {
                this.addHeader("User-Agent", agent)
            }
            return this
        }

        private fun Request.Builder.addContentLength(length: Long): Request.Builder {
            this.addHeader("Content-Length", length.toString())
            return this
        }

        private fun RpcRequestOut.toRequestBody(): RequestBody {
            return this.toJson().toRequestBody(JSON)
        }

        private fun Request.toRpcRequest() : RpcRequestOut {
            val buffer = Buffer()
            this.body?.writeTo(buffer)
            return YAJRPC.fromJson(buffer.readUtf8())
        }

        private fun Response.toRpcResponse() : RpcResponse {
            return YAJRPC.fromJson(this.body?.string() as String)
        }
    }
}