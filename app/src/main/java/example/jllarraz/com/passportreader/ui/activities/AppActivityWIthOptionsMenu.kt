package example.jllarraz.com.passportreader.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import example.jllarraz.com.passportreader.R


@SuppressLint("Registered")
open class AppActivityWithOptionsMenu : AppCompatActivity() {

    private lateinit var menu: Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        this.menu = menu
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

    fun showSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun hideSettingsMenu() {
        val settings = menu.findItem(R.id.action_settings)
        settings.isVisible = false
    }
}