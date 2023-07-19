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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.phone.Aware_Client
import com.aware.phone.R
import com.aware.phone.ui.PermissionUtils.getPermissions
import com.aware.providers.Aware_Provider
import com.aware.ui.PermissionsHandler
import com.aware.utils.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.*


class Aware_Join_Study : AppCompatActivity(), PermissionsHandler.PermissionCallback {
    private var active_plugins: ArrayList<PluginInfo>? = null
    private var pluginsRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var pluginsInstalled = true
    private var participantIdEditText: EditText? = null
    private var btnAction: Button? = null
    private var btnQuit: Button? = null
    private var btnPermissions: Button? = null
    private var txtJoinDisabled: TextView? = null
    private var llPluginsRequired: LinearLayout? = null
    private var study_configs: JSONArray? = null
    private var participantId: String? = null
    private var permissions: ArrayList<String>? = null
    //private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionsHandler: PermissionsHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aware_join_study)
        val txtStudyTitle = findViewById<View>(R.id.txt_title) as TextView
        val txtStudyDescription = findViewById<View>(R.id.txt_description) as TextView
        val txtStudyResearcher = findViewById<View>(R.id.txt_researcher) as TextView
        btnAction = findViewById<View>(R.id.btn_sign_up) as Button
        btnQuit = findViewById<View>(R.id.btn_quit_study) as Button
        btnPermissions = findViewById<View>(R.id.btn_go_to_permissions) as Button
        txtJoinDisabled = findViewById(R.id.txt_join_disabled) as TextView
        participantIdEditText = findViewById(R.id.participant_id)
        pluginsRecyclerView = findViewById<View>(R.id.rv_plugins) as RecyclerView
        mLayoutManager = LinearLayoutManager(this)
        pluginsRecyclerView!!.layoutManager = mLayoutManager
        llPluginsRequired = findViewById<View>(R.id.ll_plugins_required) as LinearLayout
        study_url = intent.getStringExtra(EXTRA_STUDY_URL)

        permissionsHandler = PermissionsHandler(this)
        btnPermissions!!.visibility = View.GONE
        //If we are getting here from an AWARE study link
        val scheme = intent.scheme
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

        val url = Uri.parse(study_url)
        if (participantId == null) participantId = url.getQueryParameter("pid")
        if (participantId != null) {
            if (Aware.DEBUG) Log.d(
                Aware.TAG,
                "AWARE Study participant ID detected: " + participantId
            )
            Aware.setSetting(applicationContext, Aware_Preferences.DEVICE_ID, participantId)
            study_url = study_url!!.substring(0, study_url!!.indexOf("pid") - 1)
            if (Aware.DEBUG) Log.d(Aware.TAG, "AWARE Study URL: " + study_url)

            participantIdEditText!!.setText(participantId)
            participantIdEditText!!.isFocusable = false
            participantIdEditText!!.isEnabled = false
            participantIdEditText!!.isCursorVisible = false
            participantIdEditText!!.keyListener = null
            participantIdEditText!!.setBackgroundColor(Color.TRANSPARENT)
        } else {
            if (Aware.DEBUG) Log.d(
                Aware.TAG,
                "AWARE Study participant ID NOT detected"
            )
            participantIdEditText!!.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isNotEmpty()) {
                        Aware.setSetting(
                            applicationContext,
                            Aware_Preferences.DEVICE_ID,
                            s.toString()
                        )
                        btnAction!!.isEnabled = true
                    } else {
                        btnAction!!.isEnabled = false
                    }
                }
                override fun afterTextChanged(s: Editable) {}
            })
        }
        if (Aware.DEBUG) Log.d(Aware.TAG, "Study URL:" + study_url)
        val qry = Aware.getStudy(this, study_url)
        if (qry == null || !qry.moveToFirst()) {
            PopulateStudy().execute(study_url)
        } else {
            try {
                study_configs =
                    JSONArray(qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_CONFIG)))
                txtStudyTitle.text =
                    qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_TITLE))
                txtStudyDescription.text = Html.fromHtml(
                    qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION)),
                    null,
                    null
                )
                txtStudyResearcher.text =
                    qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_PI))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (!qry.isClosed) qry.close()
            if (study_configs != null) {
                populateStudyInfo(study_configs!!)
            }



            btnAction!!.setOnClickListener {
                btnAction!!.isEnabled = false
                btnAction!!.alpha = 0.5f
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
                JoinStudyAsync().execute()
            }
            btnQuit!!.setOnClickListener {
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
                AlertDialog.Builder(this@Aware_Join_Study)
                    .setMessage("Are you sure you want to quit the study?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialogInterface, i ->
                        btnQuit!!.isEnabled = false
                        btnQuit!!.alpha = 1f
                        btnAction!!.isEnabled = false
                        btnAction!!.alpha = 1f
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
                        QuitStudyAsync().execute()
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
        val pluginStatuses = IntentFilter()
        pluginStatuses.addAction(Aware.ACTION_AWARE_PLUGIN_INSTALLED)
        pluginStatuses.addAction(Aware.ACTION_AWARE_PLUGIN_UNINSTALLED)
        registerReceiver(pluginCompliance, pluginStatuses)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    private open inner class PopulateStudy : AsyncTask<String?, Void?, JSONObject?>() {
        var mPopulating: ProgressDialog? = null
        private var study_url = ""
        private var study_api_key = ""
        private var study_id = ""
        private var study_config = ""
        override fun onPreExecute() {
            super.onPreExecute()
            mPopulating = ProgressDialog(this@Aware_Join_Study)
            mPopulating!!.setMessage("Retrieving study information, please wait.")
            mPopulating!!.setCancelable(false)
            mPopulating!!.setInverseBackgroundForced(false)
            mPopulating!!.show()
        }

        protected override fun doInBackground(vararg params: String?): JSONObject? {
            study_url = params[0].toString()
            if (study_url.length == 0) return null
            if (Aware.DEBUG) Log.d(Aware.TAG, "Aware_QRCode study_url: $study_url")
            val study_uri = Uri.parse(study_url)
            val protocol = study_uri.scheme
            val path_segments = study_uri.pathSegments
            if (path_segments.size > 0) {
                study_api_key = path_segments[path_segments.size - 1]
                study_id = path_segments[path_segments.size - 2]
                Log.d(Aware.TAG, "Study API: $study_api_key Study ID: $study_id")
                val request: String?
                request = if (protocol == "https") {
                    //Note: Joining a study always downloads the certificate.
                    SSLManager.handleUrl(applicationContext, study_url, true)
                    while (!SSLManager.hasCertificate(applicationContext, study_uri.host)) {
                        //wait until we have the certificate downloaded
                    }
                    try {
                        Https(
                            SSLManager.getHTTPS(
                                applicationContext,
                                study_url
                            )
                        ).dataGET(
                            study_url.substring(
                                0,
                                study_url.indexOf("/index.php")
                            ) + "/index.php/webservice/client_get_study_info/" + study_api_key, true
                        )
                    } catch (e: FileNotFoundException) {
                        Log.d(Aware.TAG, "Failed to load certificate: " + e.message)
                        null
                    }
                } else {
                    Http().dataGET(
                        study_url.substring(
                            0,
                            study_url.indexOf("/index.php")
                        ) + "/index.php/webservice/client_get_study_info/" + study_api_key, true
                    )
                }
                if (request != null) {
                    try {
                        if (request == "[]") {
                            return null
                        }
                        val study_data = JSONObject(request)


                        //Automatically register this device on the study and create credentials for this device ID!
                        val data = Hashtable<String, String>()
                        data[Aware_Preferences.DEVICE_ID] =
                            Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID)
                        data["platform"] = "android"
                        try {
                            val package_info = applicationContext.packageManager.getPackageInfo(
                                applicationContext.packageName, 0
                            )
                            data["package_name"] = package_info.packageName
                            data["package_version_code"] = package_info.versionCode.toString()
                            data["package_version_name"] = package_info.versionName.toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.d(Aware.TAG, "Failed to put package info: $e")
                            e.printStackTrace()
                        }
                        val answer: String?
                        answer = if (protocol == "https") {
                            try {
                                Https(SSLManager.getHTTPS(applicationContext, study_url)).dataPOST(
                                    study_url,
                                    data,
                                    true
                                )
                            } catch (e: FileNotFoundException) {
                                null
                            }
                        } else {
                            Http().dataPOST(study_url, data, true)
                        }
                        if (answer != null) {
                            try {
                                val configs_study = JSONArray(answer)
                                if (!configs_study.getJSONObject(0).has("message")) {
                                    study_config = configs_study.toString()
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        } else return null
                        return study_data
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } else {
                Toast.makeText(
                    this@Aware_Join_Study,
                    "Missing API key or study ID. Scanned: $study_url",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return null
        }

        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)
            if (result == null) {
                mPopulating!!.dismiss()
                val builder = android.app.AlertDialog.Builder(this@Aware_Join_Study)
                builder.setPositiveButton("OK") { dialog, which ->
                    setResult(RESULT_CANCELED)

                    //Reset the webservice server status because this one is not valid
                    Aware.setSetting(applicationContext, Aware_Preferences.STATUS_WEBSERVICE, false)
                    val resetClient = Intent(applicationContext, Aware_Client::class.java)
                    resetClient.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(resetClient)
                    finish()
                }
                builder.setTitle("Study information")
                builder.setMessage("Unable to retrieve this study information: $study_url\nTry again.")
                builder.show()
            } else {
                try {
                    val dbStudy = Aware.getStudy(applicationContext, study_url)
                    if (Aware.DEBUG) Log.d(Aware.TAG, DatabaseUtils.dumpCursorToString(dbStudy))
                    if (dbStudy == null || !dbStudy.moveToFirst()) {
                        val studyData = ContentValues()
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
                                applicationContext, Aware_Preferences.DEVICE_ID
                            )
                        )
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
                            System.currentTimeMillis()
                        )
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, study_id)
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_API, study_api_key)
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, study_url)
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_PI,
                            """${result.getString("researcher_first")} ${result.getString("researcher_last")}
                            Contact: ${result.getString("researcher_contact")}"""
                        )
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, study_config)
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_TITLE,
                            result.getString("study_name")
                        )
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                            result.getString("study_description")
                        )
                        contentResolver.insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData)
                        if (Aware.DEBUG) {
                            Log.d(Aware.TAG, "New study data: $studyData")
                        }
                    } else {
                        //Update the information to the latest
                        val studyData = ContentValues()
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(
                                applicationContext, Aware_Preferences.DEVICE_ID
                            )
                        )
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_TIMESTAMP,
                            System.currentTimeMillis()
                        )
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_JOINED, 0)
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_EXIT, 0)
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, study_id)
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_API, study_api_key)
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, study_url)
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_PI,
                            """${result.getString("researcher_first")} ${result.getString("researcher_last")}
                            Contact: ${result.getString("researcher_contact")}"""
                        )
                        studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, study_config)
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_TITLE,
                            result.getString("study_name")
                        )
                        studyData.put(
                            Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                            result.getString("study_description")
                        )
                        contentResolver.insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData)
                        if (Aware.DEBUG) {
                            Log.d(Aware.TAG, "Re-scanned study data: $studyData")
                        }
                    }
                    if (dbStudy != null && !dbStudy.isClosed) dbStudy.close()
                    mPopulating!!.dismiss()

                    //Reload join study wizard. We already have the study info on the database.
                    val studyInfo = Intent(applicationContext, Aware_Join_Study::class.java)
                    studyInfo.putExtra(EXTRA_STUDY_URL, intent.getStringExtra(EXTRA_STUDY_URL))
                    studyInfo.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    finish()
                    startActivity(studyInfo)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class QuitStudyAsync : AsyncTask<Void?, Void?, Void?>() {
        var mQuitting: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            mQuitting = ProgressDialog(this@Aware_Join_Study)
            mQuitting!!.setMessage("Quitting study, please wait.")
            mQuitting!!.setCancelable(false)
            mQuitting!!.setInverseBackgroundForced(false)
            mQuitting!!.show()
            mQuitting!!.setOnDismissListener {
                finish()

                //Redirect the user to the main UI
                val mainUI = Intent(applicationContext, Aware_Client::class.java)
                mainUI.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainUI)
            }
        }

        protected override fun doInBackground(vararg params: Void?): Void? {
            Aware.reset(applicationContext)
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            mQuitting!!.dismiss()
        }
    }

    /**
     * Join study asynchronously
     */
    private inner class JoinStudyAsync : AsyncTask<Void?, Void?, Void?>() {
        var mLoading: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            mLoading = ProgressDialog(this@Aware_Join_Study)
            mLoading!!.setMessage("Joining study, please wait.")
            mLoading!!.setCancelable(false)
            mLoading!!.setInverseBackgroundForced(false)
            mLoading!!.show()
            mLoading!!.setOnDismissListener {
                finish()
                //Redirect the user to the main UI
                val mainUI = Intent(applicationContext, Aware_Client::class.java)
                mainUI.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainUI)
            }
        }

        protected override fun doInBackground(vararg params: Void?): Void? {
            StudyUtils.applySettings(applicationContext, study_configs)
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            mLoading!!.dismiss()
        }
    }


    class PluginCompliance : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(Aware.ACTION_AWARE_PLUGIN_INSTALLED, ignoreCase = true)) {
                val joinStudy = Intent(context, Aware_Join_Study::class.java)
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
        permissionsHandler.requestPermissions(permissions, this)

        //Show the plugins' information
        active_plugins = ArrayList()
        for (i in 0 until plugins.length()) {
            try {
                val plugin_config = plugins.getJSONObject(i)
                val package_name = plugin_config.getString("plugin")
                val installed = PluginsManager.isInstalled(this, package_name)
                if (installed == null) {
                    active_plugins!!.add(PluginInfo(package_name, package_name, false))
                } else {
                    active_plugins!!.add(
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
        mAdapter = PluginsAdapter(active_plugins!!)
        pluginsRecyclerView!!.adapter = mAdapter
    }

    private fun populatePermissionsList(plugins: JSONArray, sensors: JSONArray): ArrayList<String> {
        var permissions: ArrayList<String> = ArrayList()
        for (i in 0 until plugins.length()) {
            try {
                val plugin = plugins.getJSONObject(i)
                permissions?.addAll(
                    getPermissions(plugin.getString("plugin"))
                )  //sends in "com.aware.plugin...."
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        for (i in 0 until sensors.length()) {
            try {
                val sensor = sensors.getJSONObject(i)
                permissions?.addAll(
                    getPermissions(sensor.getString("setting"))
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        permissions?.addAll(PermissionUtils.getRequiredPermissions())

        return ArrayList(permissions?.distinct())

    }


//    private fun requestStudyPermissions(permissions: ArrayList<String>){
//        val permissionRequest: MutableList<String> = ArrayList()
//        for(p in permissions){
//            if(ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED){
//                permissionRequest.add(p)
//            }
//        }
//        if(permissionRequest.isNotEmpty()){
//            permissionLauncher.launch(permissionRequest.toTypedArray())
//        }
//    }
//
//    private fun showPermissionRationale(permissions: ArrayList<String>){
//        pluginsInstalled = false
//        if(permissions.isNotEmpty()){
//            val builder = AlertDialog.Builder(this@Aware_Join_Study)
//            builder.setTitle("Permissions Required")
//            builder.setMessage("In order to participate in this study you must accept all permissions. " +
//                    "Please go into your settings and accept permissions.")
//            builder.setCancelable(false)
//            builder.setPositiveButton("Ok", object: DialogInterface.OnClickListener{
//
//                override fun onClick(p0: DialogInterface?, p1: Int) {
//                    permissionLauncher.launch(permissions.toTypedArray())
//                }
//            })
//            builder.show()
//        }
//    }

    override fun onResume() {
        super.onResume()
        if(permissions != null){
            pluginsInstalled =  true
            for(p in permissions!!){
                if(ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED){
                    pluginsInstalled = false
                    break
                }
            }
        }
        if (active_plugins == null) return
        val qry = Aware.getStudy(this, study_url)
        if (qry != null && qry.moveToFirst()) {
            llPluginsRequired!!.visibility = View.GONE
            if (pluginsInstalled) {
                btnAction!!.alpha = 1f
                pluginsInstalled = true
                txtJoinDisabled!!.isEnabled = false
                txtJoinDisabled!!.visibility = View.GONE
                btnPermissions!!.visibility = View.GONE
                btnPermissions!!.isEnabled = false
                if (participantIdEditText!!.text.isNotEmpty()) btnAction!!.isEnabled = true
                btnAction!!.visibility = View.VISIBLE

            } else {
                btnAction!!.isEnabled = false
                btnAction!!.alpha = .3f
                btnAction!!.visibility = View.GONE
            }
            if (Aware.isStudy(applicationContext)) {
                btnQuit!!.visibility = View.VISIBLE
                btnAction!!.setOnClickListener { finish() }
                btnAction!!.text = "OK"
            } else {
                btnQuit!!.visibility = View.GONE
            }
            qry.close()
        }
        if (Aware.getSetting(this, Aware_Preferences.INTERFACE_LOCKED) == "true") {
            val bottomNavigationView =
                findViewById<View>(R.id.aware_bottombar) as BottomNavigationView
            bottomNavigationView.visibility = View.GONE
        }
    }

    private fun verifyInstalledPlugins(): Boolean {
        var result = true
        for (plugin in active_plugins!!) {
            val installed = PluginsManager.isInstalled(this, plugin.packageName)
            if (installed != null) {
                plugin.installed = true
            } else {
                plugin.installed = false
                result = false
            }
        }
        mAdapter!!.notifyDataSetChanged()
        return result
    }

    inner class PluginsAdapter(private val mDataset: ArrayList<PluginInfo>) :
        RecyclerView.Adapter<PluginsAdapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var txtPackageName: TextView
            var btnInstall: Button
            var cbInstalled: CheckBox

            init {
                txtPackageName = v.findViewById<View>(R.id.txt_package_name) as TextView
                btnInstall = v.findViewById<View>(R.id.btn_install) as Button
                cbInstalled = v.findViewById<View>(R.id.cb_installed) as CheckBox
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.plugins_installation_list_item, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.txtPackageName.text = mDataset[position].pluginName
            holder.btnInstall.setOnClickListener {
                Toast.makeText(this@Aware_Join_Study, "Installing...", Toast.LENGTH_SHORT).show()
                Aware.downloadPlugin(
                    applicationContext,
                    mDataset[position].packageName,
                    study_url,
                    false
                )
            }
            if (mDataset[position].installed) {
                holder.btnInstall.visibility = View.INVISIBLE
                holder.cbInstalled.visibility = View.VISIBLE
            } else {
                holder.btnInstall.visibility = View.VISIBLE
                holder.cbInstalled.visibility = View.INVISIBLE
            }
        }

        override fun getItemCount(): Int {
            return mDataset.size
        }
    }

    inner class PluginInfo(var pluginName: String, var packageName: String, var installed: Boolean)
    companion object {
        const val EXTRA_STUDY_URL = "study_url"
        private var study_url: String? = null
        private val pluginCompliance: PluginCompliance? = PluginCompliance()
    }

    override fun onPermissionGranted() {
        pluginsInstalled = true;
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
        txtJoinDisabled!!.isEnabled = true
        txtJoinDisabled!!.visibility = View.VISIBLE
        btnPermissions!!.visibility = View.VISIBLE
        btnPermissions!!.isEnabled = true

        btnPermissions!!.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
        }
    }

    override fun onPermissionDeniedWithRationale(deniedPermissions: MutableList<String>?) {

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires the following ")
            .setPositiveButton(
                "Retry"
            ){_, _ -> permissionsHandler.requestPermissions(deniedPermissions, this@Aware_Join_Study)}
            .show()
    }
}