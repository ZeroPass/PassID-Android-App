package example.jllarraz.com.passportreader.ui.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.data.PassIdData
import example.jllarraz.com.passportreader.proto.*
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
        pfs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
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
        var msg = "Server returned error:\n${error.message}"

        if(error.code == 401){
            msg = "Authorization failed!"
        }
        else if(error.code == 412) {
            msg = "Passport trust chain verification failed!"
        }
        if(error.code == 404) {
            // TODO: parse message and translate to system language
            msg = error.message
        }
        if(error.code == 406) {
            msg = "Passport verification failed!"
            if(error.message.equals("Invalid DG1 file", true)) {
                msg = "Server refused to accept sent personal data!"
            }
            else if(error.message.equals("Invalid DG15 file", true)) {
                msg = "Server refused to accept passport's public key!"
            }
        }
        else if(error.code == 409) {
            msg = "Account already exists!"
        }

        showFatalError(msg)
    }

    protected fun showPersonalDataNeededWarning(onRetry: () -> Unit, onCancel: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Personal data requested")
            .setMessage("Server requested personal data in order to log in.\n\nSend personal data to server?")
            .setPositiveButton("Send") { dialog, _ ->
                dialog.dismiss()
                onRetry()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .setCancelable(false)
            .show()
    }

    protected fun showFatalError(msg: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.error_dialog)

        val text = dialog.findViewById(R.id.text_dialog) as TextView
        text.text = msg

        val dialogButton = dialog.findViewById(R.id.btn_dialog) as Button
        dialogButton.setOnClickListener(View.OnClickListener {
            dialog.dismiss()
            onBackPressed()
        })

        dialog.show()
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
                    showFatalError("Unknown error occurred!")
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
                        passId!!.url = clientUrl
                        passId!!.timeout = clientTimeout
                        shouldRetry.complete(true)
                    },
                    onCancel = {
                        shouldRetry.complete(false)
                    }
                )
                shouldRetry.await()
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

    val clientUrl: String
        get() {
            return pfs.getString(getString(R.string.pf_server_url), SettingsFragment.DEFAULT_HOST)!!
        }

    val clientTimeout: Long
        get() {
            return pfs.getString(getString(R.string.pf_connection_timeout), SettingsFragment.DEFAULT_TIMEOUT)!!.toLong()
        }

    companion object {
        private val TAG = SelectionActivity::class.java.simpleName
    }
}