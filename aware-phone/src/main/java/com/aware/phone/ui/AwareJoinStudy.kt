package com.aware.phone.ui

import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.phone.Aware_Client
import com.aware.phone.R
import com.aware.providers.Aware_Provider
import com.aware.ui.PermissionsHandler
import com.aware.utils.*
import kotlinx.android.synthetic.main.aware_join_study.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.*


class AwareJoinStudy : AppCompatActivity(), PermissionsHandler.PermissionCallback {
    private var activePlugins: ArrayList<PluginInfo>? = null
    private var pluginsInstalled = true
    private var studyConfigs: JSONArray? = null
    private var participantId: String? = null
    private var permissions: ArrayList<String>? = null
    private var scheme: String? = null
    private var url: Uri? = null
    private lateinit var permissionsHandler: PermissionsHandler

    companion object {
        const val EXTRA_STUDY_URL = "study_url"
        private var study_url: String? = null
        private val pluginCompliance: PluginCompliance = PluginCompliance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.aware_join_study)
        initializeMembers()
        processSchemeIntent()
        handleParticipantIdDetection()
        setUpStudyData()
        registerPluginStatusReceiver()
    }

    private fun setUpStudyData() {
        val qry = Aware.getStudy(this, study_url)
        if (qry == null || !qry.moveToFirst()) {
            populateStudy(study_url!!)
        } else {
            try {
                studyConfigs =
                    JSONArray(qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_CONFIG)))
                txt_title.text =
                    qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_TITLE))
                txt_description.text = Html.fromHtml(
                    qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION)),
                    null,
                    null
                )
                txt_researcher.text =
                    qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_PI))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (!qry.isClosed) qry.close()
            if (studyConfigs != null) {
                populateStudyInfo(studyConfigs!!)
            }

            enableOnClickListeners()
        }
    }

    private fun enableOnClickListeners() {
        btn_sign_up!!.setOnClickListener {
            btn_sign_up!!.isEnabled = false
            btn_sign_up!!.alpha = 0.5f
            val study = Aware.getStudy(applicationContext, study_url)
            if (study != null && study.moveToFirst()) {
                val studyData = ContentValues()
                studyData.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
                    applicationContext, Aware_Preferences.DEVICE_ID
                )
                )
                studyData.put(
                    Aware_Provider.Aware_Studies.STUDY_JOINED,
                    System.currentTimeMillis()
                )
                studyData.put(Aware_Provider.Aware_Studies.STUDY_EXIT, 0)
                contentResolver.update(
                    Aware_Provider.Aware_Studies.CONTENT_URI,
                    studyData,
                    Aware_Provider.Aware_Studies.STUDY_URL + " LIKE '" + study_url + "'",
                    null
                )
            }
            if (study != null && !study.isClosed) study.close()
            joinStudy()
        }
        btn_quit_study!!.setOnClickListener {
            val dbStudy = Aware.getStudy(applicationContext, study_url)
            if (dbStudy != null && dbStudy.moveToFirst()) {
                val complianceEntry = ContentValues()
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
                    System.currentTimeMillis()
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
                        applicationContext, Aware_Preferences.DEVICE_ID
                    )
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_KEY,
                    dbStudy.getInt(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_KEY))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_API,
                    dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_API))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_URL,
                    dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_URL))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_PI,
                    dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_PI))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_CONFIG,
                    dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_CONFIG))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_JOINED,
                    dbStudy.getLong(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_JOINED))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_EXIT,
                    dbStudy.getLong(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_EXIT))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_TITLE,
                    dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_TITLE))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                    dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION))
                )
                complianceEntry.put(
                    Aware_Provider.Aware_Studies.STUDY_COMPLIANCE,
                    "attempt to quit study"
                )
                contentResolver.insert(
                    Aware_Provider.Aware_Studies.CONTENT_URI,
                    complianceEntry
                )
            }
            if (dbStudy != null && !dbStudy.isClosed) dbStudy.close()
            AlertDialog.Builder(this@AwareJoinStudy)
                .setMessage("Are you sure you want to quit the study?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialogInterface, i ->
                    btn_quit_study!!.isEnabled = false
                    btn_quit_study!!.alpha = 1f
                    btn_sign_up!!.isEnabled = false
                    btn_sign_up!!.alpha = 1f
                    val dbStudy = Aware.getStudy(
                        applicationContext,
                        Aware.getSetting(
                            applicationContext,
                            Aware_Preferences.WEBSERVICE_SERVER
                        )
                    )
                    if (dbStudy != null && dbStudy.moveToFirst()) {
                        val complianceEntry = ContentValues()
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
                            System.currentTimeMillis()
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
                                applicationContext, Aware_Preferences.DEVICE_ID
                            )
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_KEY,
                            dbStudy.getInt(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_KEY))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_API,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_API))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_URL,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_URL))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_PI,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_PI))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_CONFIG,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_CONFIG))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_JOINED,
                            dbStudy.getLong(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_JOINED))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_EXIT,
                            System.currentTimeMillis()
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_TITLE,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_TITLE))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_COMPLIANCE,
                            "quit study"
                        )
                        contentResolver.insert(
                            Aware_Provider.Aware_Studies.CONTENT_URI,
                            complianceEntry
                        )
                    }
                    if (dbStudy != null && !dbStudy.isClosed) dbStudy.close()
                    dialogInterface.dismiss()
                    quitStudy()
                }
                .setNegativeButton("No") { dialogInterface, i ->
                    val dbStudy = Aware.getStudy(
                        applicationContext,
                        Aware.getSetting(
                            applicationContext,
                            Aware_Preferences.WEBSERVICE_SERVER
                        )
                    )
                    if (dbStudy != null && dbStudy.moveToFirst()) {
                        val complianceEntry = ContentValues()
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
                            System.currentTimeMillis()
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
                                applicationContext, Aware_Preferences.DEVICE_ID
                            )
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_KEY,
                            dbStudy.getInt(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_KEY))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_API,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_API))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_URL,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_URL))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_PI,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_PI))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_CONFIG,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_CONFIG))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_JOINED,
                            dbStudy.getLong(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_JOINED))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_EXIT,
                            dbStudy.getLong(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_EXIT))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_TITLE,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_TITLE))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                            dbStudy.getString(dbStudy.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION))
                        )
                        complianceEntry.put(
                            Aware_Provider.Aware_Studies.STUDY_COMPLIANCE,
                            "canceled quit"
                        )
                        contentResolver.insert(
                            Aware_Provider.Aware_Studies.CONTENT_URI,
                            complianceEntry
                        )
                    }
                    if (dbStudy != null && !dbStudy.isClosed) dbStudy.close()
                    dialogInterface.dismiss()
                }
                .setOnDismissListener { //Sync to server the studies statuses
                    val sync = Bundle()
                    sync.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    sync.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    ContentResolver.requestSync(
                        Aware.getAWAREAccount(applicationContext), Aware_Provider.getAuthority(
                            applicationContext
                        ), sync
                    )
                }
                .show()
        }
    }

    private fun handleParticipantIdDetection() {
        if (participantId == null) participantId = url?.getQueryParameter("pid")
        if (participantId != null) {
            if (Aware.DEBUG) Log.d(
                Aware.TAG,
                "AWARE Study participant ID detected: $participantId"
            )
            Aware.setSetting(applicationContext, Aware_Preferences.DEVICE_ID, participantId)
            study_url = study_url!!.substring(0, study_url!!.indexOf("pid") - 1)
            if (Aware.DEBUG) Log.d(Aware.TAG, "AWARE Study URL: $study_url")

            participant_id!!.setText(participantId)
            participant_id!!.isFocusable = false
            participant_id!!.isEnabled = false
            participant_id!!.isCursorVisible = false
            participant_id!!.keyListener = null
            participant_id!!.setBackgroundColor(Color.TRANSPARENT)
        } else {
            if (Aware.DEBUG) Log.d(
                Aware.TAG,
                "AWARE Study participant ID NOT detected"
            )
            participant_id!!.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isNotEmpty()) {
                        Aware.setSetting(
                            applicationContext,
                            Aware_Preferences.DEVICE_ID,
                            s.toString()
                        )
                        btn_sign_up!!.isEnabled = true
                    } else {
                        btn_sign_up!!.isEnabled = false
                    }
                }
                override fun afterTextChanged(s: Editable) {}
            })
        }
        if (Aware.DEBUG) Log.d(Aware.TAG, "Study URL: $study_url")
    }

    private fun processSchemeIntent() {
        //If we are getting here from an AWARE study link
        if (scheme != null) {
            if (Aware.DEBUG) Log.d(
                Aware.TAG,
                "AWARE Link detected: " + intent.dataString + " SCHEME: " + scheme
            )
            study_url = intent.dataString
            if (scheme.equals("aware", ignoreCase = true)) {
                study_url = intent.dataString?.replace("aware://", "http://")
            } else if (scheme.equals("aware-ssl", ignoreCase = true)) {
                study_url = intent.dataString?.replace("aware-ssl://", "https://")
            }
        }
    }

    private fun initializeMembers() {
        study_url = intent.getStringExtra(EXTRA_STUDY_URL)
        permissionsHandler = PermissionsHandler(this)
        scheme = intent.scheme
        url = Uri.parse(study_url)
    }

    private fun registerPluginStatusReceiver() {
        val pluginStatuses = IntentFilter()
        pluginStatuses.addAction(Aware.ACTION_AWARE_PLUGIN_INSTALLED)
        pluginStatuses.addAction(Aware.ACTION_AWARE_PLUGIN_UNINSTALLED)
        registerReceiver(pluginCompliance, pluginStatuses)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults)
    }

//    private open inner class PopulateStudy : AsyncTask<String?, Void?, JSONObject?>() {
//        var mPopulating: ProgressDialog? = null
//        private var study_url = ""
//        private var study_api_key = ""
//        private var study_id = ""
//        private var study_config = ""
//        override fun onPreExecute() {
//            super.onPreExecute()
//            mPopulating = ProgressDialog(this@AwareJoinStudy)
//            mPopulating!!.setMessage("Retrieving study information, please wait.")
//            mPopulating!!.setCancelable(false)
//            mPopulating!!.setInverseBackgroundForced(false)
//            mPopulating!!.show()
//        }
//
//        protected override fun doInBackground(vararg params: String?): JSONObject? {
//            study_url = params[0].toString()
//            if (study_url.length == 0) return null
//            if (Aware.DEBUG) Log.d(Aware.TAG, "Aware_QRCode study_url: $study_url")
//            val study_uri = Uri.parse(study_url)
//            val protocol = study_uri.scheme
//            val path_segments = study_uri.pathSegments
//            if (path_segments.size > 0) {
//                study_api_key = path_segments[path_segments.size - 1]
//                study_id = path_segments[path_segments.size - 2]
//                Log.d(Aware.TAG, "Study API: $study_api_key Study ID: $study_id")
//                val request: String?
//                request = if (protocol == "https") {
//                    //Note: Joining a study always downloads the certificate.
//                    SSLManager.handleUrl(applicationContext, study_url, true)
//                    while (!SSLManager.hasCertificate(applicationContext, study_uri.host)) {
//                        //wait until we have the certificate downloaded
//                    }
//                    try {
//                        Https(
//                            SSLManager.getHTTPS(
//                                applicationContext,
//                                study_url
//                            )
//                        ).dataGET(
//                            study_url.substring(
//                                0,
//                                study_url.indexOf("/index.php")
//                            ) + "/index.php/webservice/client_get_study_info/" + study_api_key, true
//                        )
//                    } catch (e: FileNotFoundException) {
//                        Log.d(Aware.TAG, "Failed to load certificate: " + e.message)
//                        null
//                    }
//                } else {
//                    Http().dataGET(
//                        study_url.substring(
//                            0,
//                            study_url.indexOf("/index.php")
//                        ) + "/index.php/webservice/client_get_study_info/" + study_api_key, true
//                    )
//                }
//                if (request != null) {
//                    try {
//                        if (request == "[]") {
//                            return null
//                        }
//                        val study_data = JSONObject(request)
//
//
//                        //Automatically register this device on the study and create credentials for this device ID!
//                        val data = Hashtable<String, String>()
//                        data[Aware_Preferences.DEVICE_ID] =
//                            Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID)
//                        data["platform"] = "android"
//                        try {
//                            val package_info = applicationContext.packageManager.getPackageInfo(
//                                applicationContext.packageName, 0
//                            )
//                            data["package_name"] = package_info.packageName
//                            data["package_version_code"] = package_info.versionCode.toString()
//                            data["package_version_name"] = package_info.versionName.toString()
//                        } catch (e: PackageManager.NameNotFoundException) {
//                            Log.d(Aware.TAG, "Failed to put package info: $e")
//                            e.printStackTrace()
//                        }
//                        val answer: String?
//                        answer = if (protocol == "https") {
//                            try {
//                                Https(SSLManager.getHTTPS(applicationContext, study_url)).dataPOST(
//                                    study_url,
//                                    data,
//                                    true
//                                )
//                            } catch (e: FileNotFoundException) {
//                                null
//                            }
//                        } else {
//                            Http().dataPOST(study_url, data, true)
//                        }
//                        if (answer != null) {
//                            try {
//                                val configs_study = JSONArray(answer)
//                                if (!configs_study.getJSONObject(0).has("message")) {
//                                    study_config = configs_study.toString()
//                                }
//                            } catch (e: JSONException) {
//                                e.printStackTrace()
//                            }
//                        } else return null
//                        return study_data
//                    } catch (e: JSONException) {
//                        e.printStackTrace()
//                    }
//                }
//            } else {
//                Toast.makeText(
//                    this@AwareJoinStudy,
//                    "Missing API key or study ID. Scanned: $study_url",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//            return null
//        }
//
//        override fun onPostExecute(result: JSONObject?) {
//            super.onPostExecute(result)
//            if (result == null) {
//                mPopulating!!.dismiss()
//                val builder = android.app.AlertDialog.Builder(this@AwareJoinStudy)
//                builder.setPositiveButton("OK") { dialog, which ->
//                    setResult(RESULT_CANCELED)
//
//                    //Reset the webservice server status because this one is not valid
//                    Aware.setSetting(applicationContext, Aware_Preferences.STATUS_WEBSERVICE, false)
//                    val resetClient = Intent(applicationContext, Aware_Client::class.java)
//                    resetClient.flags =
//                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//                    startActivity(resetClient)
//                    finish()
//                }
//                builder.setTitle("Study information")
//                builder.setMessage("Unable to retrieve this study information: $study_url\nTry again.")
//                builder.show()
//            } else {
//                try {
//                    val dbStudy = Aware.getStudy(applicationContext, study_url)
//                    if (Aware.DEBUG) Log.d(Aware.TAG, DatabaseUtils.dumpCursorToString(dbStudy))
//                    if (dbStudy == null || !dbStudy.moveToFirst()) {
//                        val studyData = ContentValues()
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
//                                applicationContext, Aware_Preferences.DEVICE_ID
//                            )
//                        )
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
//                            System.currentTimeMillis()
//                        )
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, study_id)
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_API, study_api_key)
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, study_url)
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_PI,
//                            """${result.getString("researcher_first")} ${result.getString("researcher_last")}
//                            Contact: ${result.getString("researcher_contact")}"""
//                        )
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, study_config)
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_TITLE,
//                            result.getString("study_name")
//                        )
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
//                            result.getString("study_description")
//                        )
//                        contentResolver.insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData)
//                        if (Aware.DEBUG) {
//                            Log.d(Aware.TAG, "New study data: $studyData")
//                        }
//                    } else {
//                        //Update the information to the latest
//                        val studyData = ContentValues()
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
//                                applicationContext, Aware_Preferences.DEVICE_ID
//                            )
//                        )
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
//                            System.currentTimeMillis()
//                        )
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_JOINED, 0)
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_EXIT, 0)
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, study_id)
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_API, study_api_key)
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, study_url)
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_PI,
//                            """${result.getString("researcher_first")} ${result.getString("researcher_last")}
//                            Contact: ${result.getString("researcher_contact")}"""
//                        )
//                        studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, study_config)
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_TITLE,
//                            result.getString("study_name")
//                        )
//                        studyData.put(
//                            Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
//                            result.getString("study_description")
//                        )
//                        contentResolver.insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData)
//                        if (Aware.DEBUG) {
//                            Log.d(Aware.TAG, "Re-scanned study data: $studyData")
//                        }
//                    }
//                    if (dbStudy != null && !dbStudy.isClosed) dbStudy.close()
//                    mPopulating!!.dismiss()
//
//                    //Reload join study wizard. We already have the study info on the database.
//                    val studyInfo = Intent(applicationContext, AwareJoinStudy::class.java)
//                    studyInfo.putExtra(EXTRA_STUDY_URL, intent.getStringExtra(EXTRA_STUDY_URL))
//                    studyInfo.flags =
//                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//                    finish()
//                    startActivity(studyInfo)
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }
//
//    private inner class QuitStudyAsync : AsyncTask<Void?, Void?, Void?>() {
//        var mQuitting: ProgressDialog? = null
//        override fun onPreExecute() {
//            super.onPreExecute()
//            mQuitting = ProgressDialog(this@AwareJoinStudy)
//            mQuitting!!.setMessage("Quitting study, please wait.")
//            mQuitting!!.setCancelable(false)
//            mQuitting!!.setInverseBackgroundForced(false)
//            mQuitting!!.show()
//            mQuitting!!.setOnDismissListener {
//                finish()
//
//                //Redirect the user to the main UI
//                val mainUI = Intent(applicationContext, Aware_Client::class.java)
//                mainUI.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(mainUI)
//            }
//        }
//
//        protected override fun doInBackground(vararg params: Void?): Void? {
//            Aware.reset(applicationContext)
//            return null
//        }
//
//        override fun onPostExecute(aVoid: Void?) {
//            super.onPostExecute(aVoid)
//            mQuitting!!.dismiss()
//        }
//    }
//
//    /**
//     * Join study asynchronously
//     */
//    private inner class JoinStudyAsync : AsyncTask<Void?, Void?, Void?>() {
//        var mLoading: ProgressDialog? = null
//        override fun onPreExecute() {
//            super.onPreExecute()
//            mLoading = ProgressDialog(this@AwareJoinStudy)
//            mLoading!!.setMessage("Joining study, please wait.")
//            mLoading!!.setCancelable(false)
//            mLoading!!.setInverseBackgroundForced(false)
//            mLoading!!.show()
//            mLoading!!.setOnDismissListener {
//                finish()
//                //Redirect the user to the main UI
//                val mainUI = Intent(applicationContext, Aware_Client::class.java)
//                mainUI.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(mainUI)
//            }
//        }
//
//        protected override fun doInBackground(vararg params: Void?): Void? {
//            StudyUtils.applySettings(applicationContext, studyConfigs)
//            return null
//        }
//
//        override fun onPostExecute(aVoid: Void?) {
//            super.onPostExecute(aVoid)
//            mLoading!!.dismiss()
//        }
//    }

    private fun populateStudy(studyUrl: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            val progressDialog = ProgressDialog(this@AwareJoinStudy).apply {
                setMessage("Retrieving study information, please wait.")
                setCancelable(false)
                setInverseBackgroundForced(false)
                show()
            }

            try {
                val studyData = withContext(Dispatchers.IO) {
                    fetchStudyData(studyUrl)
                }

                handleStudyData(studyData, studyUrl)

            } catch (e: Exception) {
                handleError(e, studyUrl)
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private suspend fun fetchStudyData(studyUrl: String): JSONObject? {
        val studyUri = Uri.parse(studyUrl)
        val protocol = studyUri.scheme
        val pathSegments = studyUri.pathSegments
        if (pathSegments.size > 0) {
            val studyApiKey = pathSegments[pathSegments.size - 1]
            val studyId = pathSegments[pathSegments.size - 2]

            // Getting the study information
            val request: String? = if (protocol == "https") {
                //Note: Joining a study always downloads the certificate.
                SSLManager.handleUrl(applicationContext, studyUrl, true)
                while (!SSLManager.hasCertificate(applicationContext, studyUri.host)) {
                    //wait until we have the certificate downloaded
                }
                try {
                    Https(
                        SSLManager.getHTTPS(
                            applicationContext,
                            studyUrl
                        )
                    ).dataGET(
                        studyUrl.substring(
                            0,
                            studyUrl.indexOf("/index.php")
                        ) + "/index.php/webservice/client_get_study_info/" + studyApiKey, true
                    )
                } catch (e: FileNotFoundException) {
                    Log.d(Aware.TAG, "Failed to load certificate: " + e.message)
                    null
                }
            } else {
                Http().dataGET(
                    studyUrl.substring(
                        0,
                        studyUrl.indexOf("/index.php")
                    ) + "/index.php/webservice/client_get_study_info/" + studyApiKey, true
                )
            }

            if (request != null) {
                try {
                    if (request == "[]") {
                        return null
                    }
                    val studyData = JSONObject(request)

                    //Automatically register this device on the study and create credentials for this device ID!
                    val data = Hashtable<String, String>()
                    data[Aware_Preferences.DEVICE_ID] =
                        Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID)
                    data["platform"] = "android"

                    try {
                        val packageInfo = applicationContext.packageManager.getPackageInfo(
                            applicationContext.packageName, 0
                        )
                        data["package_name"] = packageInfo.packageName
                        data["package_version_code"] = packageInfo.versionCode.toString()
                        data["package_version_name"] = packageInfo.versionName.toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.d(Aware.TAG, "Failed to put package info: $e")
                        e.printStackTrace()
                    }

                    val answer: String? = if (protocol == "https") {
                        try {
                            Https(SSLManager.getHTTPS(applicationContext, studyUrl)).dataPOST(
                                studyUrl,
                                data,
                                true
                            )
                        } catch (e: FileNotFoundException) {
                            null
                        }
                    } else {
                        Http().dataPOST(studyUrl, data, true)
                    }

                    if (answer != null) {
                        try {
                            val configsStudy = JSONArray(answer)
                            if (!configsStudy.getJSONObject(0).has("message")) {
                                studyData.put("study_config", configsStudy.toString())
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    return studyData
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@AwareJoinStudy,
                    "Missing API key or study ID. Scanned: $studyUrl",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return null
    }

    private fun handleStudyData(studyData: JSONObject?, studyUrl: String) {
        // Similar logic to your original onPostExecute
        if (studyData == null) {
            val builder = AlertDialog.Builder(this@AwareJoinStudy)
            builder.setPositiveButton("OK") { _, _ ->
                setResult(RESULT_CANCELED)

                //Reset the webservice server status because this one is not valid
                Aware.setSetting(applicationContext, Aware_Preferences.STATUS_WEBSERVICE, false)
                val resetClient = Intent(applicationContext, Aware_Client::class.java)
                resetClient.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(resetClient)
                finish()
            }
            builder.setTitle("Study information")
            builder.setMessage("Unable to retrieve this study information: $studyUrl\nTry again.")
            builder.show()
        } else {
            try {
                // [Your original handling of studyData from onPostExecute]
                // ...
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun handleError(e: Exception, studyUrl: String) {
        // Log error, notify user, or handle error as appropriate
        Log.e(Aware.TAG, "Error fetching study data for URL: $studyUrl", e)
    }

    private fun quitStudy() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Equivalent to onPreExecute
            val progressDialog = ProgressDialog(this@AwareJoinStudy).apply {
                setMessage("Quitting study, please wait.")
                setCancelable(false)
                setInverseBackgroundForced(false)
                show()
                setOnDismissListener {
                    finish()
                    val mainUI = Intent(applicationContext, Aware_Client::class.java)
                    mainUI.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(mainUI)
                }
            }

            try {
                // Equivalent to doInBackground
                withContext(Dispatchers.IO) {
                    Aware.reset(applicationContext)
                }
            } catch (e: Exception) {
                // Exception handling logic here, if necessary.
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun joinStudy() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Equivalent to onPreExecute
            val progressDialog = ProgressDialog(this@AwareJoinStudy).apply {
                setMessage("Joining study, please wait.")
                setCancelable(false)
                setInverseBackgroundForced(false)
                show()
                setOnDismissListener {
                    finish()
                    val mainUI = Intent(applicationContext, Aware_Client::class.java)
                    mainUI.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(mainUI)
                }
            }

            try {
                // Equivalent to doInBackground
                withContext(Dispatchers.IO) {
                    StudyUtils.applySettings(applicationContext, studyConfigs)
                }
            } catch (e: Exception) {
                // Exception handling logic here, if necessary.
            } finally {
                progressDialog.dismiss()
            }
        }
    }




    class PluginCompliance : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(Aware.ACTION_AWARE_PLUGIN_INSTALLED, ignoreCase = true)) {
                val joinStudy = Intent(context, AwareJoinStudy::class.java)
                joinStudy.putExtra(EXTRA_STUDY_URL, study_url)
                context.startActivity(joinStudy)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (pluginCompliance != null) {
            try {
                unregisterReceiver(pluginCompliance)
            } catch (e: IllegalArgumentException) {
                //no-op we can get here if we still need to retrieve the study.
            }
        }
    }

    private fun populateStudyInfo(study_config: JSONArray) {
        var plugins = JSONArray()
        var sensors = JSONArray()
        for (i in 0 until study_config.length()) {
            try {
                val element = study_config.getJSONObject(i)
                if (element.has("plugins")) {
                    plugins = element.getJSONArray("plugins")
                }
                if (element.has("sensors")) {
                    sensors = element.getJSONArray("sensors")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        permissions = populatePermissionsList(plugins, sensors)
        permissionsHandler.requestPermissions(permissions!!, this)

        //Show the plugins' information
        activePlugins = ArrayList()
        for (i in 0 until plugins.length()) {
            try {
                val plugin_config = plugins.getJSONObject(i)
                val package_name = plugin_config.getString("plugin")
                val installed = PluginsManager.isInstalled(this, package_name)
                if (installed == null) {
                    activePlugins!!.add(PluginInfo(package_name, package_name, false))
                } else {
                    activePlugins!!.add(
                        PluginInfo(
                            PluginsManager.getPluginName(
                                applicationContext, package_name
                            ), package_name, true
                        )
                    )
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun populatePermissionsList(plugins: JSONArray, sensors: JSONArray): ArrayList<String> {
        var permissions: ArrayList<String> = ArrayList()
        for (i in 0 until plugins.length()) {
            try {
                val plugin = plugins.getJSONObject(i)
                permissions?.addAll(
                    PermissionsHandler.Companion.getPermissions(plugin.getString("plugin"))
                )  //sends in "com.aware.plugin...."
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        for (i in 0 until sensors.length()) {
            try {
                val sensor = sensors.getJSONObject(i)
                permissions?.addAll(
                    PermissionsHandler.Companion.getPermissions(sensor.getString("setting"))
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        permissions?.addAll(PermissionsHandler.Companion.getRequiredPermissions())

        return ArrayList(permissions?.distinct())

    }

    override fun onResume() {
        super.onResume()
        setPluginsInstalledStatus()
        if (activePlugins == null) return
        updateLayout()
        if (Aware.getSetting(this, Aware_Preferences.INTERFACE_LOCKED) == "true") {
            aware_bottombar.visibility = View.GONE
        }
    }

    private fun updateLayout() {
        val qry = Aware.getStudy(this, study_url)
        if (qry != null && qry.moveToFirst()) {
            if (pluginsInstalled) {
                btn_sign_up!!.alpha = 1f
                pluginsInstalled = true
                if (participant_id!!.text.isNotEmpty()) btn_sign_up!!.isEnabled = true
                btn_sign_up!!.visibility = View.VISIBLE

            } else {
                btn_sign_up!!.isEnabled = false
                btn_sign_up!!.alpha = .3f
                btn_sign_up!!.visibility = View.GONE
            }
            if (Aware.isStudy(applicationContext)) {
                btn_quit_study!!.visibility = View.VISIBLE
                btn_sign_up!!.setOnClickListener { finish() }
                btn_sign_up!!.text = "OK"
            } else {
                btn_quit_study!!.visibility = View.GONE
            }
            qry.close()
        }
    }

    private fun setPluginsInstalledStatus() {
        if(permissions != null){
            pluginsInstalled =  true
            for(p in permissions!!){
                if(ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED){
                    pluginsInstalled = false
                    break
                }
            }
        }
    }

    inner class PluginInfo(var pluginName: String, var packageName: String, var installed: Boolean)

    override fun onPermissionGranted() {
        pluginsInstalled = true;
        if (!Aware.is_watch(this)) {
            Applications.isAccessibilityServiceActive(this)
        }
        val whitelisting = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        whitelisting.data = Uri.parse("package:$packageName")
        startActivity(whitelisting)

    }

    override fun onPermissionDenied(deniedPermissions: List<String>?) {
    }

    override fun onPermissionDeniedWithRationale(deniedPermissions: List<String>?) {

        AlertDialog.Builder(this)
            .setTitle("Permissions Required to Join Study")
            .setMessage(
                "Permissions are required to join study. Press OK " +
                        "to review the required permissions. Please select \"Allow\" or \"While using the app\" or \"Only this time\" for all permissions."
            )
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                permissionsHandler.requestPermissions(
                    deniedPermissions!!,
                    this@AwareJoinStudy
                )
            }
            .show()
    }
}