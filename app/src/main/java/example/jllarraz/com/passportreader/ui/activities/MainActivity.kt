package example.jllarraz.com.passportreader.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import example.jllarraz.com.passportreader.ui.fragments.MainFragment
import example.jllarraz.com.passportreader.R
import android.view.Menu

class MainActivity : AppActivityWithOptionsMenu() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment())
                    .commit()
        }

    }
}