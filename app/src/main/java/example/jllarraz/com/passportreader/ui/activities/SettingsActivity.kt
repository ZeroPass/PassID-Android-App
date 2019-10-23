package example.jllarraz.com.passportreader.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.ui.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
    }
}
