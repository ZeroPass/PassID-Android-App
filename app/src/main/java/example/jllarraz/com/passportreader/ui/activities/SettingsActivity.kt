package example.jllarraz.com.passportreader.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.proto.PassIdApi
import example.jllarraz.com.passportreader.ui.fragments.SettingsFragment
import kotlinx.android.synthetic.main.fragment_selection.*
import kotlinx.android.synthetic.main.layout_progress_bar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity(), Preference.OnPreferenceClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (null == savedInstanceState) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_container_content, SettingsFragment(), "SettingsFragmet")
                    .commit()
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        if(preference?.key == getString(R.string.pf_btn_test_connection)) {
            testConnection()
            return true
        }
        return false
    }

    private fun showToast(msg: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(applicationContext, msg, duration).show();
    }

    fun hideProgressBar() {
        llProgressBar?.visibility = View.GONE
    }

    fun showProgressBar(msg: String = "") {
        var msg = msg
        if (msg.isEmpty()) {
            msg = getString(R.string.label_please_wait)
        }
        llProgressBar?.message?.text = msg
        llProgressBar?.visibility = View.VISIBLE
    }

    private fun testConnection() {
        val pm = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val url = pm.getString(getString(R.string.pf_server_url), SettingsFragment.DEFAULT_HOST) as String
        val timeout = pm.getString(getString(R.string.pf_connection_timeout), SettingsFragment.DEFAULT_TIMEOUT)?.toLong()!!

        showProgressBar("Trying to connect to server ...")
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val api = PassIdApi(url)
                api.timeout = timeout
                api.ping()
                showToast("Connection succeeded")
            }
            catch (e: Throwable){
                showToast("Connection failed")
            }

            hideProgressBar()
        }
    }
}
