package com.aware.phone.ui

import com.aware.providers.Aware_Provider.Aware_Studies as Key
import android.app.Dialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.Color
import android.net.Uri
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
    private lateinit var permissionsHandler: PermissionsHandler

    companion object {
        const val EXTRA_STUDY_URL = "study_url"
        const val CONSTANT_KEY = "Aware_Provider.Aware_Studies"
        private var studyUrl: String? = null
        private val pluginCompliance: PluginCompliance? = PluginCompliance()
    }

    data class PluginInfo(val pluginName: String, val packageName: String, val installed: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.aware_join_study)
        studyUrl = intent.getStringExtra(EXTRA_STUDY_URL)
        permissionsHandler = PermissionsHandler(this)
        processIntentScheme()
        handleParticipantIdDetection()
        setupStudyInfo()
        registerPluginStatusReciever()
    }

    fun Cursor.getIntValue(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

    fun Cursor.getStringValue(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

    fun Cursor.getLongValue(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

    private fun processIntentScheme() {

        //If we are getting here from an AWARE study link
        intent.scheme?.let { scheme ->
            logDebug("AWARE Link detected: ${intent.dataString} SCHEME: $scheme")
            studyUrl = when (scheme.lowercase(Locale.ROOT)) {
                "aware" -> intent.dataString?.replace("aware://", "http://")
                "aware-ssl" -> intent.dataString?.replace("aware-ssl://", "https://")
                else -> intent.dataString
            }
        }
    }

    private fun handleParticipantIdDetection() {

        val url = Uri.parse(studyUrl)
        participantId = participantId ?: url.getQueryParameter("pid")
        if (participantId != null) {
            logDebug("AWARE Study participant ID detected: $participantId")
            Aware.setSetting(applicationContext, Aware_Preferences.DEVICE_ID, participantId)
            studyUrl = studyUrl!!.substring(0, studyUrl!!.indexOf("pid") - 1)
            logDebug("AWARE Study URL: $studyUrl")
            participant_id?.apply {
                setText(participantId)
                isFocusable = false
                isEnabled = false
                isCursorVisible = false
                keyListener = null
                setBackgroundColor(Color.TRANSPARENT)
            }
        } else {
            logDebug("AWARE Study participant ID NOT detected")
            participant_id?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    btn_sign_up?.apply {
                        isEnabled = s.isNotEmpty()
                    }
                    if (s.isNotEmpty()) {
                        Aware.setSetting(applicationContext, Aware_Preferences.DEVICE_ID, s.toString())
                    }
                }
                override fun afterTextChanged(s: Editable) {}
            })
        }
        logDebug("Study URL: $studyUrl")
    }

    private fun setupStudyInfo() {

        Aware.getStudy(this, studyUrl)?.use { study ->
            if(!study.moveToFirst()) {
                populateStudy(studyUrl)
            }else {
                try{
                    studyConfigs = JSONArray(study.getStringValue(Key.STUDY_CONFIG))
                    txt_title.text = study.getStringValue(Key.STUDY_TITLE)
                    txt_description.text = Html.fromHtml(study.getStringValue(Key.STUDY_DESCRIPTION), 0)
                    txt_researcher.text = study.getStringValue(Key.STUDY_PI)
                } catch(e: JSONException) { e.printStackTrace() }
                studyConfigs?.let { populateStudyInfo(it) }
                setupSignUpButton()
                setupQuitButton()
            }
        } ?: run {
            populateStudy(studyUrl)
        }
    }

    private fun setupSignUpButton() {

        btn_sign_up.setOnClickListener {
            btn_sign_up!!.isEnabled = false
            btn_sign_up!!.alpha = 0.5f
            Aware.getStudy(applicationContext, studyUrl)?.use { _ ->
                ContentValues().apply {
                    put(Key.STUDY_DEVICE_ID, Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID))
                    put(Key.STUDY_JOINED, System.currentTimeMillis())
                    put(Key.STUDY_EXIT, 0)
                    contentResolver.update(
                        Key.CONTENT_URI,this,Key.STUDY_URL + " LIKE '" + studyUrl + "'",null)
                }
            }
            joinStudy()
        }
    }

    private fun extractContentValues(studyCursor: Cursor?, studyObject: JSONObject?, pi: String,exit: Long?, joined: Long?, compliance: String?): ContentValues {

        return ContentValues().apply {

            put(Key.STUDY_TIMESTAMP,System.currentTimeMillis())
            put(Key.STUDY_DEVICE_ID, Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID))
            put(Key.STUDY_KEY, studyCursor?.getIntValue(Key.STUDY_KEY) ?: studyObject?.getInt(Key.STUDY_KEY))
            put(Key.STUDY_API, studyCursor?.getStringValue(Key.STUDY_API) ?: studyObject?.getString(Key.STUDY_API))
            put(Key.STUDY_URL, studyCursor?.getStringValue(Key.STUDY_URL) ?: studyObject?.getString(Key.STUDY_URL))
            put(Key.STUDY_CONFIG, studyCursor?.getStringValue(Key.STUDY_CONFIG) ?: studyObject?.getString(Key.STUDY_CONFIG))
            put(Key.STUDY_DESCRIPTION, studyCursor?.getStringValue(Key.STUDY_DESCRIPTION) ?: studyObject?.getString(Key.STUDY_DESCRIPTION))
            put(Key.STUDY_TITLE, studyCursor?.getStringValue(Key.STUDY_TITLE) ?: studyObject?.getString("study_name"))
            put(Key.STUDY_PI, pi)
            exit?.let { put(Key.STUDY_EXIT, it) }
            joined?.let { put(Key.STUDY_JOINED, it) }
            compliance?.let { put(Key.STUDY_COMPLIANCE, it) }

        }
    }

    private fun setupQuitButton() {

        btn_quit_study.setOnClickListener {
            Aware.getStudy(applicationContext, studyUrl)?.use { study ->

                if(study.moveToFirst()){
                    val customPi =  study.getStringValue(Key.STUDY_PI)
                    extractContentValues(study, null, customPi, study.getLongValue(Key.STUDY_EXIT), study.getLongValue(Key.STUDY_JOINED),"attempt to quit study")
                        .apply {
                            contentResolver.insert(Key.CONTENT_URI, this)
                        }
                }
            }

            AlertDialog.Builder(this@AwareJoinStudy)
                .setMessage("Are you sure you want to quit the study?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    btn_quit_study!!.isEnabled = false
                    btn_quit_study!!.alpha = 1f
                    btn_sign_up!!.isEnabled = false
                    btn_sign_up!!.alpha = 1f
                    Aware.getStudy(applicationContext, Aware.getSetting(applicationContext, Aware_Preferences.WEBSERVICE_SERVER))?.use { study ->

                        if(study.moveToFirst()){
                            val customPi = study.getStringValue(Key.STUDY_PI)
                            extractContentValues(study, null, customPi, System.currentTimeMillis(), study.getLongValue(Key.STUDY_JOINED),"attempt to quit study")
                                .apply {
                                    contentResolver.insert(Key.CONTENT_URI, this)
                                }
                        }
                    }
                    dialogInterface.dismiss()
                    quitStudy()
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    Aware.getStudy(applicationContext, Aware.getSetting(applicationContext, Aware_Preferences.WEBSERVICE_SERVER))?.use { study ->

                        if(study.moveToFirst()){
                            val customPi = study.getStringValue(Key.STUDY_PI)
                            extractContentValues(study, null, customPi, study.getLongValue(Key.STUDY_EXIT), study.getLongValue(Key.STUDY_JOINED),"attempt to quit study")
                                .apply {
                                    contentResolver.insert(Key.CONTENT_URI, this)
                                }
                        }
                    }
                    dialogInterface.dismiss()
                }
                .setOnDismissListener { //Sync to server the studies statuses
                    val sync = Bundle()
                    sync.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    sync.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    ContentResolver.requestSync(Aware.getAWAREAccount(applicationContext), Aware_Provider.getAuthority(applicationContext), sync)
                }
                .show()
        }
    }

    private fun registerPluginStatusReciever() {

        val pluginStatuses = IntentFilter()
        pluginStatuses.addAction(Aware.ACTION_AWARE_PLUGIN_INSTALLED)
        pluginStatuses.addAction(Aware.ACTION_AWARE_PLUGIN_UNINSTALLED)
        registerReceiver(pluginCompliance, pluginStatuses)
    }

    private fun populateStudy(studyUrl: String?) {

        lifecycleScope.launch(Dispatchers.Main) {
            val progressDialog = ProgressDialog(this@AwareJoinStudy).apply {
                setMessage("Retrieving study information, please wait.")
                setCancelable(false)
                setInverseBackgroundForced(false)
                show()
            }
            try{
                val studyData = withContext(Dispatchers.Main) {
                    fetchStudyData(studyUrl)
                }
                handleStudyData(studyData, studyUrl)
            }
            catch(e: Exception) { e.printStackTrace() }
            finally { progressDialog.dismiss() }
        }
    }

    private suspend fun fetchStudyData(studyUrl: String?): JSONObject? {

        if(studyUrl.isNullOrEmpty()) return null
        logDebug("Aware_QRCode study url: $studyUrl")
        val studyUri = Uri.parse(studyUrl)
        val protocol = studyUri.scheme
        val pathSegments = studyUri.pathSegments
        if(pathSegments.isNotEmpty()) {
            val studyApiKey = pathSegments[pathSegments.size - 1]
            val studyId = pathSegments[pathSegments.size - 2]
            Log.d(Aware.TAG, "Study API: $studyApiKey Study ID: $studyId")
            val request = if(protocol == "https") {
                // Note: Joining a study always downloads the certificate
                SSLManager.handleUrl(applicationContext, studyUrl, true)
                while(!SSLManager.hasCertificate(applicationContext, studyUri.host)) { /* wait */ }
                try {
                    Https(SSLManager.getHTTPS(applicationContext, studyUrl))
                        .dataGET(
                            studyUrl.substring(0, studyUrl.indexOf("/index.php")) +
                                    "/index.php/webservice/client_get_study_info/" + studyApiKey,
                            true
                        )
                } catch (e: FileNotFoundException) {
                    Log.d(Aware.TAG, "Failed to load certificate: " + e.message)
                    null
                }
            } else {
                Http().dataGET(
                    studyUrl.substring(0, studyUrl.indexOf("/index.php")) +
                            "/index.php/webservice/client_get_study_info/" + studyApiKey,
                    true
                )
            }
            if(request != null) {
                try {
                    if(request == "[]") return null
                    val studyData = JSONObject(request)
                    /* Automatically register this device on the study and
                       create credentials for this device ID
                    */
                    val data = Hashtable<String, String>()
                    data[Aware_Preferences.DEVICE_ID] = Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID)
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
                    val answer = if(protocol == "https") {
                        try {
                            Https(SSLManager.getHTTPS(applicationContext, studyUrl))
                                .dataPOST(studyUrl, data, true)
                        } catch(e: FileNotFoundException) { return null }
                    } else {
                        Http().dataPOST(studyUrl, data, true)
                    }
                    if(answer != null) {
                        try {
                            val configsStudy = JSONArray(answer)
                            if(!configsStudy.getJSONObject(0).has("message")){
                                // this might get used inside of onPostExecute
                                val studyConfigs = configsStudy.toString()
                                studyData.put(Key.STUDY_CONFIG, studyConfigs)
                            }
                        } catch(e: JSONException) { e.printStackTrace() }
                    } else return null
                    return studyData
                } catch (e: JSONException) { e.printStackTrace() }
            }
        } else {
            Toast.makeText(
                this@AwareJoinStudy,
                "Missing API key or study ID. Scanned: $studyUrl",
                Toast.LENGTH_SHORT
            ).show()
        }
        return null
    }

    private fun handleStudyData(studyData: JSONObject?, studyUrl: String?) {

        if(studyData == null) {
            AlertDialog.Builder(this@AwareJoinStudy).apply {
                setTitle("Study Information")
                setMessage("Unable to retrieve this study information: $studyUrl")
                setPositiveButton("OK") { _, _ ->
                    setResult(RESULT_CANCELED)
                    // Reset the webservice server status because this one is not valid
                    Aware.setSetting(applicationContext, Aware_Preferences.WEBSERVICE_SERVER, false)
                    navigateToMainClient(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    finish()
                }
                show()
            }
        } else {
            try {
                Aware.getStudy(applicationContext, studyUrl).use { study ->
                    logDebug(DatabaseUtils.dumpCursorToString(study))
                    val customPi =
                        """${studyData.getString("researcher_first")} |${studyData.getString("researcher_last")} 
                            |Contact: ${studyData.getString("researcher_contact")}""".trimMargin()

                    if (study == null || !study.moveToFirst()) {
                        extractContentValues(null, studyData, customPi, null, null, null)
                            .apply {
                                contentResolver.insert(Key.CONTENT_URI, this)
                                logDebug("New study data: $this")
                            }
                    } else {
                        //Update the information to the latest
                        extractContentValues(null, studyData, customPi, 0, 0, null)
                            .apply {
                                contentResolver.insert(Key.CONTENT_URI, this)
                                logDebug("Re-scanned study data: $this")
                            }
                    }
                    val studyInfo = Intent(applicationContext, AwareJoinStudy::class.java)
                    studyInfo.putExtra(EXTRA_STUDY_URL, intent.getStringExtra(EXTRA_STUDY_URL))
                    studyInfo.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    finish()
                    startActivity(studyInfo)
                }
            } catch(e: JSONException) { e.printStackTrace() }
        }
    }

    private fun quitStudy() {

        lifecycleScope.launch(Dispatchers.Main) {
            val progressDialog = ProgressDialog(this@AwareJoinStudy).apply {
                setMessage("Quitting study, please wait.")
                setCancelable(false)
                setInverseBackgroundForced(false)
                setOnDismissListener {
                    navigateToMainClient()
                }
                show()
            }
            try {
                withContext(Dispatchers.IO) {
                    Aware.reset(applicationContext)
                }
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun joinStudy() {

        lifecycleScope.launch {
            val progressDialog = ProgressDialog(this@AwareJoinStudy).apply {
                setMessage("Joining study, please wait.")
                setCancelable(false)
                setInverseBackgroundForced(false)
                show()
                setOnDismissListener {
                    navigateToMainClient()
                    finish()
                }
            }
            try {
                withContext(Dispatchers.IO) {
                    StudyUtils.applySettings(applicationContext, studyConfigs)
                }
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun logDebug(message: String) {
        if(Aware.DEBUG) Log.d(Aware.TAG, message)
    }

    fun navigateToMainClient(flag: Int = Intent.FLAG_ACTIVITY_CLEAR_TASK) {
        val intent = Intent(applicationContext, Aware_Client::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or flag
        }
        startActivity(intent)
    }

    private fun populateStudyInfo(study_config: JSONArray) {

        var plugins = JSONArray()
        var sensors = JSONArray()
        for (i in 0 until study_config.length()) {
            try {
                val element = study_config.getJSONObject(i)
                if (element.has("plugins")) plugins = element.getJSONArray("plugins")
                if (element.has("sensors")) sensors = element.getJSONArray("sensors")
            } catch (e: JSONException) { e.printStackTrace() }
        }

        //Show the plugins' information
        activePlugins = plugins.run {
            (0 until length()).mapNotNull { index ->
                optJSONObject(index)?.let { pluginConfig ->
                    val packageName = pluginConfig.optString("plugin")
                    PluginsManager.isInstalled(this@AwareJoinStudy, packageName)?.let {
                        PluginInfo(PluginsManager.getPluginName(applicationContext, packageName), packageName, true)
                    } ?: PluginInfo(packageName, packageName, false)
                }
            }
        }.toCollection(ArrayList())

        permissions = populatePermissionsList(plugins, sensors)
        permissionsHandler.requestPermissions(permissions!!, this)
    }

    private fun populatePermissionsList(plugins: JSONArray, sensors: JSONArray): ArrayList<String> {

        var permissions = HashSet<String>()

        for (i in 0 until plugins.length()) {
            try {
                val plugin = plugins.getJSONObject(i)
                permissions.addAll(PermissionsHandler.getPermissions(plugin.getString("plugin")))  //sends in "com.aware.plugin...."
            } catch (e: JSONException) { e.printStackTrace()}
        }

        for (i in 0 until sensors.length()) {
            try {
                val sensor = sensors.getJSONObject(i)
                permissions.addAll(PermissionsHandler.Companion.getPermissions(sensor.getString("setting")))
            } catch (e: JSONException) { e.printStackTrace() }
        }

        permissions.addAll(PermissionsHandler.getRequiredPermissions())

        return ArrayList(permissions)
    }

    class PluginCompliance : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(Aware.ACTION_AWARE_PLUGIN_INSTALLED, ignoreCase = true)) {
                val joinStudy = Intent(context, AwareJoinStudy::class.java)
                joinStudy.putExtra(EXTRA_STUDY_URL, studyUrl)
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
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        permissions?.let {
            pluginsInstalled = it.all {
                    permission -> ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        aware_bottombar.visibility = View.GONE
        activePlugins?.let {
            Aware.getStudy(this@AwareJoinStudy, studyUrl)?.use { study ->
                if(study.moveToFirst()) {
                    btn_sign_up?.apply {
                        isEnabled = pluginsInstalled && participant_id?.text?.isNotEmpty() == true
                        visibility = if (pluginsInstalled) View.VISIBLE else View.GONE
                        alpha = if (pluginsInstalled) 1f else 0.3f
                    }
                    btn_quit_study?.visibility = if (Aware.isStudy(applicationContext)) View.VISIBLE else View.GONE
                    if (Aware.isStudy(applicationContext)) {
                        btn_sign_up?.apply {
                            setOnClickListener { finish() }
                            text = "OK"
                        }
                    }

                }
            }
        }

    }
}