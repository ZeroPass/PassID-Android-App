/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example.jllarraz.com.passportreader.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

import org.jmrtd.lds.icao.MRZInfo

import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.common.IntentData
import example.jllarraz.com.passportreader.ui.fragments.SelectionFragment
import kotlinx.coroutines.*
import example.jllarraz.com.passportreader.data.PassIdData
import android.view.Menu
import androidx.core.content.edit
import example.jllarraz.com.passportreader.proto.*
import example.jllarraz.com.passportreader.ui.fragments.SuccessFragment


class SelectionActivity : PassIdBaseActivity(),  SelectionFragment.SelectionFragmentListener {

    private var nfcIntentExtras: Bundle? = null
    private var dPassIdData:  CompletableDeferred<PassIdData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SelectionFragment(), TAG_SELECTION_FRAGMENT)
                .commit()
        }

        handleAction()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val res = super.onCreateOptionsMenu(menu)
        if(intent.action == ACTION_VIEW_PASSPORT){
            hideSettingsMenu()
        }
        return res
    }

    private fun handleAction() {
        when {
            intent.action == ACTION_REGISTER -> {
                setTitle(R.string.selection_activity_register)
                register()
            }
            intent.action == ACTION_LOGIN -> {
                setTitle(R.string.selection_activity_login)
                login()
            }
            intent.action == ACTION_VIEW_PASSPORT -> {
                setTitle(R.string.selection_activity_view)
            }
            else ->{
                Log.e(TAG, "Unknown action '${intent.action}'")
                finish()
            }
        }
    }

    /**
     * Return data needed for passID protocol.
     * What it does is sets challenge to be signed by passport for nfc intent
     * then waits for nfc intent to return passID data.
     * */
    override suspend fun getPassIdData(challenge: PassIdProtoChallenge) : PassIdData {
        nfcIntentExtras = Bundle()
        nfcIntentExtras!!.putParcelable(IntentData.KEY_PASSID_CHALLENGE, challenge)

        dPassIdData = CompletableDeferred<PassIdData>()
        dPassIdData!!.await()
        val data = dPassIdData!!.getCompleted()

        dPassIdData = null
        return data
    }

    override fun onLoginSucceed(uid: UserId) {
        requestGreeting()
    }

    override fun onRegisterSucceed(uid: UserId) {
        requestGreeting()
    }

    override fun onRequestGreetingFinished(serverMsg: String) {
        var title = "Registration Succeeded"
        if(intent.action == ACTION_LOGIN) {
            title = "Login Succeeded"
        }
        showSuccessScreen(title, serverMsg, passId!!.session!!.uid)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var data = data
        if (data == null) {
            data = Intent()
        }
        when (requestCode) {
            REQUEST_MRZ -> {
                hideProgressBar()
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        onPassportRead(data.getSerializableExtra(IntentData.KEY_MRZ_INFO) as MRZInfo)
                    }
                    Activity.RESULT_CANCELED -> {
                        val fragmentByTag = supportFragmentManager.findFragmentByTag(TAG_SELECTION_FRAGMENT)
                        if (fragmentByTag is SelectionFragment) {
                            fragmentByTag.selectManualToggle()
                        }
                    }
                    else -> {
                        val fragmentByTag = supportFragmentManager.findFragmentByTag(TAG_SELECTION_FRAGMENT)
                        if (fragmentByTag is SelectionFragment) {
                            fragmentByTag.selectManualToggle()
                        }
                    }
                }
            }
            REQUEST_NFC -> {
                val fragmentByTag = supportFragmentManager.findFragmentByTag(TAG_SELECTION_FRAGMENT)
                if (fragmentByTag is SelectionFragment) {
                    fragmentByTag.selectManualToggle()
                }

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if(dPassIdData != null) {
                            dPassIdData!!.complete(
                                data.getParcelableExtra<PassIdData>(
                                    IntentData.KEY_PASSID_DATA)!!
                            )
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPassportRead(mrzInfo: MRZInfo) {
        pfs.edit{
            putString(PF_DOC_NUM, mrzInfo.documentNumber)
            putString(PF_DOC_DOB, mrzInfo.dateOfBirth)
            putString(PF_DOC_DOE, mrzInfo.dateOfExpiry)
        }

        val intent = Intent(this, NfcActivity::class.java)
        intent.putExtra(IntentData.KEY_MRZ_INFO, mrzInfo)
        if(nfcIntentExtras != null) {
            intent.putExtras(nfcIntentExtras!!)
        }
        startActivityForResult(intent, REQUEST_NFC)
    }

    override fun onMrzRequest() {
        showProgressBar()
        val intent = Intent(this, CameraActivity::class.java)
        startActivityForResult(intent, REQUEST_MRZ)
    }

    private fun showSuccessScreen(title: String, serverMsg: String, uid: UserId) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, SuccessFragment.newInstance(title, serverMsg, uid))
            .commitAllowingStateLoss()
    }

    companion object {
        private val TAG = SelectionActivity::class.java.simpleName
        private const val REQUEST_MRZ = 12
        private const val REQUEST_NFC = 11

        private const val TAG_SELECTION_FRAGMENT = "TAG_SELECTION_FRAGMENT"

        const val ACTION_REGISTER      = "example.jllarraz.com.passportreader.ui.activities.ACTION_REGISTER"
        const val ACTION_LOGIN         = "example.jllarraz.com.passportreader.ui.activities.ACTION_LOGIN"
        const val ACTION_VIEW_PASSPORT = "example.jllarraz.com.passportreader.ui.activities.ACTION_VIEW_PASSPORT"

        const val PF_DOC_NUM = "DOCUMENT_NUMBER"
        const val PF_DOC_DOB = "DOCUMENT DATE_OF_BIRTH"
        const val PF_DOC_DOE = "DOCUMENT EXPIRY DATE"
    }
}