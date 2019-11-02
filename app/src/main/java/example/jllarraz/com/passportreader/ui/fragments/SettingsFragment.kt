package example.jllarraz.com.passportreader.ui.fragments

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.preference.*
import example.jllarraz.com.passportreader.utils.StringUtils
import example.jllarraz.com.passportreader.R
import android.text.InputFilter
import example.jllarraz.com.passportreader.ui.activities.SettingsActivity



class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val serverUrl = findPreference<EditTextPreference>(getString(R.string.pf_server_url))!!
        bindPreferenceSummaryToValue(serverUrl, DEFAULT_HOST)
        serverUrl.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            it.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        val timeout = findPreference<EditTextPreference>(getString(R.string.pf_connection_timeout))!!
        bindPreferenceSummaryToValue(timeout, DEFAULT_TIMEOUT)

        timeout.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.imeOptions = EditorInfo.IME_ACTION_DONE
            it.filters = arrayOf(InputFilter.LengthFilter(5))
        }

        val btnTestConn = findPreference<Preference>(getString(R.string.pf_btn_test_connection))!!
        btnTestConn.onPreferenceClickListener = getSettingsActivity()
    }

    private fun bindPreferenceSummaryToValue(preference: Preference, defaultValue: String = "") {
        preference.onPreferenceChangeListener = this
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.context)
                        .getString(preference.key, defaultValue))
    }

    override fun onPreferenceChange(preference: Preference?, value: Any?): Boolean {
        val stringValue = value.toString()

        if (preference?.key == getString(R.string.pf_server_url)) {
            if (stringValue.isEmpty()) {
                return false
            }
            else if (!StringUtils.isValidHttpUrl(stringValue)) {
                showToast("Invalid URL.  Example: http://example.com")
                return false
            }
        }
        else if (preference?.key == getString(R.string.pf_connection_timeout)) {
            if (stringValue.isEmpty() || stringValue.toInt() < 1000) {
                return false
            }
        }

        preference?.summary = stringValue
        return true
    }

    private fun showToast(msg: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, msg, duration).show();
    }

    private fun getSettingsActivity() : SettingsActivity {
        return activity as SettingsActivity
    }

    companion object {
        const val DEFAULT_HOST = "http://127.0.0.1"
        const val DEFAULT_TIMEOUT = "5000"
    }
}