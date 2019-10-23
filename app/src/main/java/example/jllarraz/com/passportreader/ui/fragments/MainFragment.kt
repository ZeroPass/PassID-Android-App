package example.jllarraz.com.passportreader.ui.fragments

import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageButton


import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.ui.activities.SelectionActivity
import example.jllarraz.com.passportreader.ui.activities.SettingsActivity

class MainFragment : androidx.fragment.app.Fragment() {

    private var btnViewPassport: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnViewPassport = view.findViewById(R.id.btnViewPassport)
        btnViewPassport!!.setOnClickListener {
            val intent = Intent(context, SelectionActivity::class.java)
            startActivity(intent);
        }
    }
}