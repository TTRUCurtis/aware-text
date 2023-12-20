package com.aware.phone.ui

import android.Manifest
import android.annotation.SuppressLint
import com.aware.providers.Aware_Provider.Aware_Studies as Key
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.aware.utils.studyeligibility.StudyEligibility
import kotlinx.android.synthetic.main.aware_join_study.*
import kotlinx.android.synthetic.main.aware_item_layout.view.*
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
    private var deviceId: String? = null
    private var permissions: ArrayList<String>? = null
    private val missingPermissions = mutableListOf<String>()
    private lateinit var permissionsHandler: PermissionsHandler
    private lateinit var studyEligibility: StudyEligibility

    companion object {
        const val EXTRA_STUDY_URL = "study_url"
        private var studyUrl: String? = null
        private val pluginCompliance: PluginCompliance = PluginCompliance()
    }

    data class PluginInfo(val pluginName: String, val packageName: String, val installed: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.aware_join_study)
        studyUrl = intent.getStringExtra(EXTRA_STUDY_URL)
        permissionsHandler = PermissionsHandler(this@AwareJoinStudy)
        studyEligibility = StudyEligibility(this@AwareJoinStudy)
        processIntentScheme()
        handleParticipantIdDetection()
        setupStudyInfo()
        registerPluginStatusReceiver()
    }

    private fun Cursor.getIntValue(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

    private fun Cursor.getStringValue(columnName: String): String = getString(getColumnIndexOrThrow(columnName))

    private fun Cursor.getLongValue(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

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
            aware_join_id.apply {
                setText(participantId)
                isFocusable = false
                isEnabled = false
                isCursorVisible = false
                keyListener = null
                setBackgroundColor(Color.TRANSPARENT)
            }
        } else {
            logDebug("AWARE Study participant ID NOT detected")
            aware_join_id?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    btn_sign_up?.apply {
                        isEnabled = s.isNotEmpty()
                    }
                    if (s.isNotEmpty()) {
                        Aware.setSetting(applicationContext, Aware_Preferences.DEVICE_ID, s.trim().toString())
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
                    aware_join_study_info.aware_item_image.setImageResource(R.drawable.ic_launcher_aware)
                    aware_join_study_info.aware_item_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                    aware_join_study_info.aware_item_title.text = study.getStringValue(Key.STUDY_TITLE)
                    aware_join_study_info.aware_item_description.text = Html.fromHtml(study.getStringValue(Key.STUDY_DESCRIPTION), 0)
                    aware_join_study_info.aware_item_extra.visibility = View.VISIBLE
                    aware_join_study_info.aware_item_extra.text =
                        "Researcher: ${study.getStringValue(Key.STUDY_PI)}"
                } catch(e: JSONException) { e.printStackTrace() }
                studyConfigs?.let {
                    populateStudyInfo(it)
                    studyEligibility.checkForSmsPluginStatus(it)
                }
                if(studyEligibility.isSmsPluginEnabled()){
                    studyEligibility.showSMSPermissionDialog(permissionsHandler, this@AwareJoinStudy)
                }else{
                    setupSignUpButton()
                    setupQuitButton()
                    permissionsHandler.requestPermissions(permissions!!, this)
                }
            }
        } ?: run {
            populateStudy(studyUrl)
        }
    }

    private fun setupSignUpButton() {

        btn_sign_up.setOnClickListener {
            btn_sign_up!!.isEnabled = false
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

    private fun registerPluginStatusReceiver() {

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

    private fun fetchStudyData(studyUrl: String?): JSONObject? {

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

    private fun navigateToMainClient(flag: Int = Intent.FLAG_ACTIVITY_CLEAR_TASK) {
        val intent = Intent(applicationContext, Aware_Client::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or flag
            putExtra("studyUrl", studyUrl)
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

        deviceId = sensors.run {
            var foundDeviceId: String? = null
            for (i in 0 until this.length()) {
                val item = this.getJSONObject(i)
                if (item.getString("setting") == "mqtt_username") {
                    foundDeviceId = item.getString("value")
                    break
                }
            }
            foundDeviceId
        }

        permissions = populatePermissionsList(plugins, sensors)
    }

    private fun populatePermissionsList(plugins: JSONArray, sensors: JSONArray): ArrayList<String> {

        val permissions = HashSet<String>()

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

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimization() {

        pluginsInstalled = true

        if (!Aware.is_watch(this)) {
            Applications.isAccessibilityServiceActive(this)
        }
        val whitelisting = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        whitelisting.data = Uri.parse("package:$packageName")
        startActivity(whitelisting)
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

    private fun handleStudyEligibilityResult(isEligible: Boolean) {

        val message = if (isEligible) "You passed!" else "You did not pass!"
        val dialog = AlertDialog.Builder(this@AwareJoinStudy)
            .setMessage(message)
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()

            if (isEligible) {
                setupSignUpButton()
                setupQuitButton()
                permissionsHandler.requestPermissions(permissions!!, this@AwareJoinStudy)
            } else {
                navigateToMainClient()
            }

            performServerPing(
                JSONObject().apply {
                    put("Participant Id", participantId ?: deviceId)
                    put("Study Eligibility Test Result", isEligible)
                }
            )
        }, 2000)
    }

    private fun performServerPing(data: JSONObject) {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = Https().dataPOSTJson(
                "https://survey.wwbp.org/test/registerAware/update/",
                data,
                true)
            Log.d("Miguel", "Response: $response")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pluginCompliance)
        } catch (e: IllegalArgumentException) {
            //no-op we can get here if we still need to retrieve the study.
        }
    }

    override fun onPermissionGranted() {

        if(studyEligibility.isSmsPluginEnabled() && studyEligibility.shouldPerformStudyEligibility()){
            studyEligibility.performStudyEligibilityCheck(object: StudyEligibility.EligibilityCheckCallback{
                override fun onEligibilityChecked(isEligible: Boolean) {
                    handleStudyEligibilityResult(isEligible)
                }
            })
        }else {
            requestBatteryOptimization()
        }
    }

    override fun onPermissionDenied(deniedPermissions: List<String>?) {

        deniedPermissions?.forEach {
            missingPermissions.add(it)
        }
        enableDeniedPermissionButton()
    }

    private fun getPermissionRationale(missingPermissions: MutableList<String>): String {

        val mobileVersionPermission = permissionsHandler.getDistinctPermissionsList(missingPermissions)
        val permissionString = mobileVersionPermission!!.joinToString(separator = ", ")
        return "Permissions are required to join a study. Tap on " +
                "\"OPEN SETTINGS\", click on \"Permissions\" and Please select " +
                "\"Allow\" or \"Allow only while using the app\" or \"Ask every time\" for " +
                "the following permissions: $permissionString"
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
                    permission -> permissionsHandler.isPermissionGranted(permission)
            }
        }
        activePlugins?.let {
            Aware.getStudy(this@AwareJoinStudy, studyUrl)?.use { study ->
                if(study.moveToFirst()) {
                    btn_sign_up?.apply {
                        isEnabled = pluginsInstalled && aware_join_id?.text?.isNotEmpty() == true
                        visibility = if (pluginsInstalled) View.VISIBLE else View.GONE
                        alpha = if (pluginsInstalled) 1f else 0.3f
                    }
                    btn_quit_study?.visibility = if (Aware.isStudy(applicationContext)) View.VISIBLE else View.GONE
                    if (Aware.isStudy(applicationContext)) {
                        btn_sign_up?.apply {
                            isEnabled = true                          
                            setOnClickListener { finish() }
                            text = R.string.ok.toString()
                        }
                    }

                }
            }
        }

        if(missingPermissions.isNotEmpty()){
            missingPermissions.iterator().let {
                while(it.hasNext()) {
                    val permission = it.next()
                    if(permissionsHandler.isPermissionGranted(permission))
                        it.remove()
                }
            }

            if(missingPermissions.isEmpty()) {
                if(studyEligibility.shouldPerformStudyEligibility()) {
                    studyEligibility.performStudyEligibilityCheck(object: StudyEligibility.EligibilityCheckCallback {
                        override fun onEligibilityChecked(isEligible: Boolean) {
                            handleStudyEligibilityResult(isEligible)
                        }

                    })
                }else {
                    requestBatteryOptimization()
                }
            }
        }

        if(missingPermissions.isEmpty()) {
            aware_join_revoked_permission.aware_item.visibility = View.GONE
            listOf(
                aware_join_id,
                aware_join_study_info,
                aware_join_onboarding
            ).forEach { view ->
                view.apply {
                    isEnabled = true
                    alpha = 1f
                }
            }
        } else {
            enableDeniedPermissionButton()
        }
    }

    private fun enableDeniedPermissionButton() {

        listOf(
            aware_join_id,
            aware_join_onboarding,
            aware_join_study_info
        ).forEach { view ->
            view.apply {
                isEnabled = false
                alpha = 0.25f
            }
        }

        aware_join_revoked_permission.aware_item.visibility = View.VISIBLE
        aware_join_revoked_permission.aware_item_title.text = "Denied Permissions"
        aware_join_revoked_permission.aware_item_description.text = "Grant permissions from app settings"
        aware_join_revoked_permission.aware_item_image.setImageResource(R.drawable.ic_error)
        val backgroundDrawable = ContextCompat.getDrawable(aware_join_revoked_permission.aware_item_card.context, R.drawable.item_background_2) as GradientDrawable
        aware_join_revoked_permission.aware_item_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red))
        aware_join_revoked_permission.aware_item.background = backgroundDrawable

        aware_join_revoked_permission.aware_item.setOnClickListener {
            AlertDialog.Builder(this@AwareJoinStudy).apply {
                setMessage(getPermissionRationale(missingPermissions))
                setPositiveButton("go to settings") { _, _ ->
                    permissionsHandler.openAppSettings()
                }
                setNegativeButton("cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                show()
            }
        }
    }
}