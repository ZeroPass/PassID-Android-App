package example.jllarraz.com.passportreader.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import example.jllarraz.com.passportreader.ui.fragments.MainFragment
import example.jllarraz.com.passportreader.R
import android.view.Menu


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment())
                    .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }



    override fun onResume() {
        super.onResume()

//        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
//        et_allow_notification.setText(sharedPref.getBoolean(getString(R.string.pref_key_allow_notification), false).toString())
//        et_zipcode.setText(sharedPref.getString(getString(R.string.pref_key_zipcode), ""))
//        et_unit.setText(sharedPref.getString(getString(R.string.pref_key_unit), ""))
    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//
//        return when (item.itemId) {
//            R.id.btnSettings -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
}