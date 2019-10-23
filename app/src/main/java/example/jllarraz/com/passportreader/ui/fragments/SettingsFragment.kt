package example.jllarraz.com.passportreader.ui.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.utils.StringUtils


class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val serverUrl: EditTextPreference? = findPreference(getString(R.string.setting_server_url))
        bindPreferenceSummaryToValue(serverUrl as Preference, DEFAULT_HOST)
    }

    private fun bindPreferenceSummaryToValue(preference: Preference, defaultValue: String = "") {
        preference.onPreferenceChangeListener = this
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.context)
                        .getString(preference.key, defaultValue))
    }

    override fun onPreferenceChange(preference: Preference?, value: Any?): Boolean {
        var stringValue = value.toString()

        if (preference?.key == getString(R.string.setting_server_url)) {
            if (stringValue.isEmpty()) {
                stringValue = DEFAULT_HOST
            }
            else if (!StringUtils.isValidHttpUrl(stringValue)) {
                Toast.makeText(context, "Invalid URL.  Example: http://example.com", Toast.LENGTH_LONG).show();
                return false
            }
        }

        preference?.summary = stringValue
        return true
    }

    companion object {
        private const val DEFAULT_HOST = "http://127.0.0.1"
    }
}
