package com.aware.phone.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.phone.Aware_Client
import com.aware.phone.R
import com.aware.ui.PermissionsHandler

import kotlinx.android.synthetic.main.aware_ui_participant.*

class Aware_Participant : AppCompatActivity(), PermissionsHandler.PermissionCallback {

    private lateinit var permissionsHandler: PermissionsHandler
    private lateinit var requestPermissionBtn:Button
    private lateinit var permissionRationale: TextView

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        (supportActionBar as ActionBar).setDisplayHomeAsUpEnabled(false)
        (supportActionBar as ActionBar).setDisplayShowHomeEnabled(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aware_ui_participant)
        requestPermissionBtn = findViewById<View>(R.id.request_permission) as Button
        permissionRationale = findViewById<View>(R.id.permission_rationale) as TextView
        permissionsHandler = PermissionsHandler(this)
        requestPermissionBtn.isVisible = false
        permissionRationale.isVisible = false

        if (intent != null && intent.extras != null && intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) != null) {

            val permissions =
                intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) as java.util.ArrayList<String>?
            permissionsHandler.requestPermissions(permissions!!, this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        device_id.text = Aware.getSetting(this, Aware_Preferences.DEVICE_ID)
        device_name.text = Aware.getSetting(this, Aware_Preferences.DEVICE_LABEL)
        study_url.text = Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.aware_menu, menu)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.title.toString().equals(resources.getString(R.string.aware_qrcode), ignoreCase = true)) item.isVisible = false
            if (item.title.toString().equals(resources.getString(R.string.aware_team), ignoreCase = true)) item.isVisible = false
            if (item.title.toString().equals(resources.getString(R.string.aware_study), ignoreCase = true)) item.isVisible = true
            if (item.title.toString().equals(resources.getString(R.string.aware_sync), ignoreCase = true)) item.isVisible = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.title.toString().equals(resources.getString(R.string.aware_qrcode), ignoreCase = true)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
                val permission = ArrayList<String>()
                permission.add(Manifest.permission.CAMERA)

                val permissions = Intent(this, PermissionsHandler::class.java)
                permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, permission)
                permissions.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY, "$packageName/$packageName.ui.Aware_QRCode")
                permissions.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(permissions)
            } else {
                val qrcode = Intent(this@Aware_Participant, Aware_QRCode::class.java)
                qrcode.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(qrcode)
            }
        }
        if (item.title.toString().equals(resources.getString(R.string.aware_study), ignoreCase = true)) {
            val studyInfo = Intent(this@Aware_Participant, Aware_Join_Study::class.java)
            studyInfo.putExtra(Aware_Join_Study.EXTRA_STUDY_URL, Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER))
            startActivity(studyInfo)
        }
        if (item.title.toString().equals(resources.getString(R.string.aware_team), ignoreCase = true)) {
            val about_us = Intent(this@Aware_Participant, About::class.java)
            startActivity(about_us)
        }
        if (item.title.toString().equals(resources.getString(R.string.aware_sync), ignoreCase = true)) {
            Toast.makeText(applicationContext, "Syncing data...", Toast.LENGTH_SHORT).show()
            val sync = Intent(Aware.ACTION_AWARE_SYNC_DATA)
            sendBroadcast(sync)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPermissionGranted() {
        val redirectService = Intent()
        redirectService.action = Aware_Client.ACTION_AWARE_PERMISSIONS_CHECK
        val component = intent.getStringExtra(Aware_Client.EXTRA_REDIRECT_SERVICE)!!
            .split("/").toTypedArray()
        redirectService.component = ComponentName(component[0], component[1])
        startService(redirectService)
    }

    override fun onPermissionDenied(deniedPermissions: List<String>?) {
        permissionRationale.visibility = View.VISIBLE
        requestPermissionBtn.visibility = View.VISIBLE
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    override fun onPermissionDeniedWithRationale(deniedPermissions: List<String>?) {
        /*
            This activity is responsible for handling permissions that have been revoked
            and does not receive any rationale as revoked permissions do not trigger this
            part of the permission flow.
         */
    }


}