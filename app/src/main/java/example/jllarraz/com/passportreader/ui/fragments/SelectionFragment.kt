package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.widget.AppCompatEditText
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.preference.PreferenceManager

import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator

import net.sf.scuba.data.Gender

import org.jmrtd.lds.icao.MRZInfo

import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.ui.activities.SelectionActivity
import example.jllarraz.com.passportreader.ui.validators.DateRule
import example.jllarraz.com.passportreader.ui.validators.DocumentNumberRule


class SelectionFragment : androidx.fragment.app.Fragment(), Validator.ValidationListener {

    private var radioGroup: RadioGroup? = null
    private var rbManual: AppCompatRadioButton? = null
    private var linearLayoutManual: LinearLayout? = null
    private var linearLayoutAutomatic: LinearLayout? = null
    private var appCompatEditTextDocumentNumber: AppCompatEditText? = null
    private var appCompatEditTextDocumentExpiration: AppCompatEditText? = null
    private var appCompatEditTextDateOfBirth: AppCompatEditText? = null
    private var btnFillData: Button? = null
    private var buttonReadNFC: Button? = null

    private var mValidator: Validator? = null
    private var selectionFragmentListener: SelectionFragmentListener? = null

    lateinit var pfs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        pfs = PreferenceManager.getDefaultSharedPreferences(activity!!.applicationContext)
        val inflatedView = inflater.inflate(R.layout.fragment_selection, container, false)
        return inflatedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        radioGroup = view.findViewById(R.id.radioButtonDataEntry)
        rbManual = view.findViewById<AppCompatRadioButton>(        R.id.radioButtonManual)
        linearLayoutManual = view.findViewById(R.id.layoutManual)
        linearLayoutAutomatic = view.findViewById(R.id.layoutAutomatic)
        appCompatEditTextDocumentNumber = view.findViewById(R.id.documentNumber)
        appCompatEditTextDocumentExpiration = view.findViewById(R.id.documentExpiration)
        appCompatEditTextDateOfBirth = view.findViewById(R.id.documentDateOfBirth)

        btnFillData = view.findViewById(R.id.btnFillData)
        if(!pfs.contains(SelectionActivity.PF_DOC_NUM) &&
                !pfs.contains(SelectionActivity.PF_DOC_DOB) &&
                !pfs.contains(SelectionActivity.PF_DOC_DOE)) {
            btnFillData!!.visibility = View.GONE
        }

        buttonReadNFC = view.findViewById(R.id.buttonReadNfc)

        radioGroup!!.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioButtonManual -> {
                    linearLayoutManual!!.visibility = View.VISIBLE
                    linearLayoutAutomatic!!.visibility = View.GONE
                }
                R.id.radioButtonOcr -> {
                    linearLayoutManual!!.visibility = View.GONE
                    linearLayoutAutomatic!!.visibility = View.VISIBLE
                    if (selectionFragmentListener != null) {
                        selectionFragmentListener!!.onMrzRequest()
                    }
                }
            }
        }

        btnFillData!!.setOnClickListener {
            appCompatEditTextDocumentNumber!!.setText(pfs.getString(SelectionActivity.PF_DOC_NUM, ""))
            appCompatEditTextDateOfBirth!!.setText(pfs.getString(SelectionActivity.PF_DOC_DOB, ""))
            appCompatEditTextDocumentExpiration!!.setText(pfs.getString(SelectionActivity.PF_DOC_DOE, ""))
        }

        buttonReadNFC!!.setOnClickListener { validateFields() }

        mValidator = Validator(this)
        mValidator!!.setValidationListener(this)

        mValidator!!.put(appCompatEditTextDocumentNumber!!, DocumentNumberRule())
        mValidator!!.put(appCompatEditTextDocumentExpiration!!, DateRule())
        mValidator!!.put(appCompatEditTextDateOfBirth!!, DateRule())
    }

    protected fun validateFields() {
        try {
            mValidator!!.removeRules(appCompatEditTextDocumentNumber!!)
            mValidator!!.removeRules(appCompatEditTextDocumentExpiration!!)
            mValidator!!.removeRules(appCompatEditTextDateOfBirth!!)

            mValidator!!.put(appCompatEditTextDocumentNumber!!, DocumentNumberRule())
            mValidator!!.put(appCompatEditTextDocumentExpiration!!, DateRule())
            mValidator!!.put(appCompatEditTextDateOfBirth!!, DateRule())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mValidator!!.validate()
    }

    fun selectManualToggle() {
        rbManual!!.isChecked = true
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is SelectionFragment.SelectionFragmentListener) {
            selectionFragmentListener = activity
        }
    }

    override fun onDetach() {
        selectionFragmentListener = null
        super.onDetach()

    }


    override fun onValidationSucceeded() {

        val documentNumber = appCompatEditTextDocumentNumber!!.text!!.toString()
        val dateOfBirth = appCompatEditTextDateOfBirth!!.text!!.toString()
        val documentExpiration = appCompatEditTextDocumentExpiration!!.text!!.toString()

        val mrzInfo = MRZInfo("P",
                "ESP",
                "DUMMY",
                "DUMMY",
                documentNumber,
                "ESP",
                dateOfBirth,
                Gender.MALE,
                documentExpiration,
                "DUMMY"
        )
        if (selectionFragmentListener != null) {
            selectionFragmentListener!!.onPassportRead(mrzInfo)
        }
    }

    override fun onValidationFailed(errors: List<ValidationError>) {
        for (error in errors) {
            val view = error.view
            val message = error.getCollatedErrorMessage(context)

            // Display error messages ;)
            if (view is EditText) {
                view.error = message
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    interface SelectionFragmentListener {
        fun onPassportRead(mrzInfo: MRZInfo)
        fun onMrzRequest()
    }

    companion object {

        fun newInstance(mrzInfo: MRZInfo, face: Bitmap): PassportDetailsFragment {
            val myFragment = PassportDetailsFragment()
            val args = Bundle()
            myFragment.arguments = args
            return myFragment
        }
    }
}
