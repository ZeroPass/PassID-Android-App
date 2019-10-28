package example.jllarraz.com.passportreader.ui.fragments

import android.content.Intent
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.ui.activities.SelectionActivity

class MainFragment : androidx.fragment.app.Fragment() {

    private var btnRegister: Button? = null
    private var btnLogin: Button? = null
    private var btnViewPassport: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnRegister = view.findViewById(R.id.btnRegister)
        btnRegister!!.setOnClickListener {
            val intent = Intent(SelectionActivity.ACTION_REGISTER)
            startActivity(intent);
        }

        btnLogin = view.findViewById(R.id.btnLogin)
        btnLogin!!.setOnClickListener {
            val intent = Intent(SelectionActivity.ACTION_LOGIN)
            startActivity(intent);
        }

        btnViewPassport = view.findViewById(R.id.btnViewPassport)
        btnViewPassport!!.setOnClickListener {
            val intent = Intent(SelectionActivity.ACTION_VIEW_PASSPORT)
            startActivity(intent);
        }
    }
}