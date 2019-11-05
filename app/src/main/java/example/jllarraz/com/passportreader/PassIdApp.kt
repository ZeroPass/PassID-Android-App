package example.jllarraz.com.passportreader

import androidx.multidex.MultiDexApplication
import example.jllarraz.com.passportreader.utils.TrustManagerFactory
import okhttp3.OkHttpClient
import okhttp3.internal.toCanonicalHost
import java.io.InputStream
import javax.net.ssl.*
import java.net.URI


class PassIdApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val trustManager = getPassIdServerTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
        val sslSocketFactory = sslContext.socketFactory

        httpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier(HostnameVerifier{ host: String, sslSession: SSLSession ->
                if(host == allowedHost) {
                    return@HostnameVerifier true
                }
                false
            })
            .build()
    }

    companion object {
        private lateinit var instance: PassIdApp
        private lateinit var httpClient: OkHttpClient
        var allowedHost: String = ""
        set(url: String) {
            val uri = URI(url)
            field = uri.host
        }


        fun getInstance() : PassIdApp {
            return instance
        }

        fun getHttpClient() : OkHttpClient {
            return httpClient
        }

        private fun getPassIdServerTrustManager() : X509TrustManager {
            return TrustManagerFactory.getInstanceWithFixedCertificateSet(
                    getPassIdServerCertificatesInputStream()
            )
        }
        private fun getPassIdServerCertificatesInputStream() : InputStream {
            return instance.assets.open("certs/passid_server.cer")
        }
    }
}
