package example.jllarraz.com.passportreader.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.proto.UserId
import kotlinx.android.synthetic.main.fragment_success.*


class SuccessFragment :  androidx.fragment.app.Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_success, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title!!.text = arguments?.getString("title")
        uid!!.text = arguments?.getParcelable<UserId>("uid")!!.hex()
    }

    companion object {
        fun newInstance(title: String, uid: UserId): SuccessFragment {
            val myFragment = SuccessFragment()
            val args = Bundle()
            args.putString("title", title)
            args.putParcelable("uid", uid)
            myFragment.arguments = args
            return myFragment
        }
    }
}