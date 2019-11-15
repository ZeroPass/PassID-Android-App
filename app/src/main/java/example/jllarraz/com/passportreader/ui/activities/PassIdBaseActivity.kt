package example.jllarraz.com.passportreader.ui.activities

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.PreferenceManager
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.data.PassIdData
import example.jllarraz.com.passportreader.proto.*
import example.jllarraz.com.passportreader.ui.dialogs.ErrorAlert
import example.jllarraz.com.passportreader.ui.dialogs.InfoAlert
import example.jllarraz.com.passportreader.ui.fragments.SettingsFragment
import kotlinx.android.synthetic.main.fragment_selection.*
import kotlinx.android.synthetic.main.layout_progress_bar.view.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class FinishActivityException() : Throwable()

abstract class PassIdBaseActivity : AppActivityWithOptionsMenu(), CoroutineScope {

    override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main + job

    private lateinit var job: Job
    private var passId: PassIdClient? = null
    protected lateinit var pfs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_camera)
        pfs = PreferenceManager.getDefaultSharedPreferences(this)
    }

    abstract suspend fun getPassIdData(challenge: PassIdProtoChallenge) : PassIdData
    abstract fun onRegisterSucceed(uid: UserId)
    abstract fun onLoginSucceed(uid: UserId)


    protected fun register() = passIdScope {
        showProgressBar("Please wait ...")
        passId!!.register { challenge ->
            hideProgressBar()
            val data = getPassIdData(challenge)
            showProgressBar("Registering new account ...")
            data
        }

        hideProgressBar()
        Log.i(TAG, "register succeeded")
        onRegisterSucceed(passId!!.session!!.uid)
    }

    protected fun login() = passIdScope {
        showProgressBar("Please wait ...")
        passId!!.login() { challenge ->
            hideProgressBar()
            val data = getPassIdData(challenge)
            showProgressBar("Logging in ...")
            data
        }

        hideProgressBar()
        Log.i(TAG, "log-in succeeded")
        onLoginSucceed(passId!!.session!!.uid)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        cancel() // cancel any remaining suspended tasks
        passId?.close()
        super.onDestroy()
    }

    protected fun hideProgressBar() {
        llProgressBar?.visibility = View.GONE
    }

    protected fun showProgressBar(msg: String = "") {
        var msg = msg
        if (msg.isEmpty()) {
            msg = getString(R.string.label_please_wait)
        }
        llProgressBar?.message?.text = msg
        llProgressBar?.visibility = View.VISIBLE
    }

    protected fun showConnectionError(onRetry: () -> Unit, onCancel: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Connection Error")
            .setMessage("Failed to connect to server!")
            .setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                onRetry()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .setNeutralButton("Settings"){ dialog, _ ->
                showSettings()
                showConnectionError(onRetry, onCancel)
            }
            .setCancelable(false)
            .show()
    }

    protected fun showProtoError(error: PassIdApiError) {
        var title: String? = "PassID Error"
        var msg = "Server returned error:\n${error.message}"

        when {
            error.code == 401 -> msg = "Authorization failed!"
            error.code == 412 -> msg = "Passport trust chain verification failed!"
            error.code == 404 -> {
                // TODO: parse message and translate to system language
                msg = error.message
            }
            error.code == 406 -> {
                msg = "Passport verification failed!"
                when {
                    error.message.equals("Invalid DG1 file", true) ->
                        msg = "Server refused to accept sent personal data!"
                    error.message.equals("Invalid DG15 file", true) ->
                        msg = "Server refused to accept passport's public key!"
                }
            }
            error.code == 409 -> msg = "Account already exists!"
            else -> title = null
        }

        showFatalError(title, msg)
    }

    protected fun showPersonalDataNeededWarning(onRetry: () -> Unit, onCancel: () -> Unit) {
        val a = InfoAlert(this)
        a.setTitle("PERSONAL INFORMATION REQUESTED")
        a.message =  "Server requested your personal information.\nSend personal data to server?"
        a.setPositiveButton("Send") { dialog, _ ->
            dialog.dismiss()
            onRetry()
        }
        a.setNegativeButton("Go Back") { dialog, _ ->
            dialog.dismiss()
            onCancel()
        }
        a.show()
    }

    protected fun showFatalError(title: String?, msg: String) {
        val a = ErrorAlert(this)
        if(title == null) {
            a.setTitle(msg)
        }
        else {
            a.setTitle(title)
            a.message = msg
        }
        a.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
            onBackPressed()
        }
        a.show()
    }

    /**
     * Function acts as scope. It calls suspendable callback and handles any unhandled exception,
     * This scope is used for any API call to passID client which needs to communicate with server.
     * Exceptions are handled by showing error dialog box to the user and closing this activity.
     **/
    protected fun passIdScope(callback: suspend () -> Unit) = launch {

        initPassIdClient()
        try {
            callback.invoke()
        }
        catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }

            if(e is FinishActivityException) {
                onBackPressed()
            }
            else{
                e.printStackTrace()
                if(e is PassIdApiError) {
                    showProtoError(e)
                }
                else {
                    showFatalError(null, "Unknown error occurred!")
                }
            }
        }
    }

    private fun initPassIdClient() {
        if (passId == null) {
            passId = PassIdClient(clientUrl, clientTimeout)
            passId!!.onConnectionFailed = {
                // If connection fails for any reason this functions shows connection error popup dialog,
                // and notifies back passID client if it should retry sending request to the server or give up.
                val shouldRetry = CompletableDeferred<Boolean>()
                showConnectionError(
                    onRetry = {
                        shouldRetry.complete(true)
                    },
                    onCancel = {
                        shouldRetry.complete(false)
                    }
                )
                shouldRetry.await()
                if(!shouldRetry.getCompleted()){
                    throw FinishActivityException()
                }
                true
            }

            passId!!.onPersonalDataRequested = {
                val sendPersonalData = CompletableDeferred<Boolean>()
                showPersonalDataNeededWarning(
                    onRetry = {
                        sendPersonalData.complete(true)
                    },
                    onCancel = {
                        sendPersonalData.complete(false)
                    }
                )
                sendPersonalData.await()
                if(!sendPersonalData.getCompleted()){
                    throw FinishActivityException()
                }
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(passId != null) {
            passId!!.url = clientUrl
            passId!!.timeout = clientTimeout
        }
    }

    private val clientUrl: String
        get() {
            return pfs.getString(getString(R.string.pf_server_url), SettingsFragment.DEFAULT_HOST)!!
        }

    private val clientTimeout: Long
        get() {
            return pfs.getString(getString(R.string.pf_connection_timeout), SettingsFragment.DEFAULT_TIMEOUT)!!.toLong()
        }

    companion object {
        private val TAG = SelectionActivity::class.java.simpleName
    }
}