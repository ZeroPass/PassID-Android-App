package example.jllarraz.com.passportreader.proto

import info.laht.yajrpc.*
import info.laht.yajrpc.net.RpcClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import java.util.concurrent.*

typealias Consumer<T> = (T) -> Unit


class JsonRpcClient  constructor(
    private val client: OkHttpClient,
    var url: String = ""
) : RpcClient {

    var userAgent: String? = null
    var origin: String? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val callbacks = mutableMapOf<String, Consumer<RpcResponse>>()

    override fun notify(methodName: String, params: RpcParams) {
        send(RpcRequestOut(methodName, params).apply {
            id = UUID.randomUUID().toString()
        })
    }

    @Throws(TimeoutException::class)
    override fun write(methodName: String, params: RpcParams, timeout: Long): Future<RpcResponse> {
        fun task(): () -> RpcResponse = {
            var reqid = ""
            try {
                var response: RpcResponse? = null
                val latch = CountDownLatch(1)
                val request = RpcRequestOut(methodName, params).apply {
                    id = UUID.randomUUID().toString()
                    reqid = id.toString()
                    callbacks[reqid] = {
                        response = it
                        latch.countDown()
                    }
                }

                val call = send(request)
                if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                    call.cancel()
                    throw TimeoutException("Timeout!")
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
                if(rpcResp.id != null) { // id can be null
                    val id = rpcResp.id.toString()
                    if (callbacks.contains(id)) {
                        callbacks[id]?.also { callback ->
                            callback.invoke(rpcResp)
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                if(!e.message.equals("Canceled", ignoreCase = true)) {
                    throw e
                }
            }
        })
    }

    override fun close() {
        executor.shutdown()
    }

    companion object {
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

        private fun Response.toRpcResponse() : RpcResponse {
            return YAJRPC.fromJson<RpcResponse>(this.body?.string() as String)
        }
    }
}