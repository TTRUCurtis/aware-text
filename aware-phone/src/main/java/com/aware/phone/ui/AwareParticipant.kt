package com.aware.phone.ui

import android.Manifest
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.phone.Aware_Client
import com.aware.phone.R
import com.aware.phone.ui.AwareParticipant.AwareParticipantItems.revokedPermissions
import com.aware.ui.PermissionsHandler
import kotlinx.android.synthetic.main.aware_participant_item_layout.view.*
import kotlinx.android.synthetic.main.aware_ui_participant.*

class AwareParticipant : AppCompatActivity(), PermissionsHandler.PermissionCallback {

    private lateinit var permissionsHandler: PermissionsHandler

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        (supportActionBar as ActionBar).setDisplayHomeAsUpEnabled(false)
        (supportActionBar as ActionBar).setDisplayShowHomeEnabled(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aware_ui_participant)
        permissionsHandler = PermissionsHandler(this)
        checkForRevokedPermissions()
    }

    private fun checkForRevokedPermissions() {
        if (intent != null && intent.extras != null && intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) != null) {

            val permissions =
                intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) as java.util.ArrayList<String>?

            for(p in permissions!!) {
                if(!permissionsHandler.isPermissionGranted(p)) {
                    revokedPermissions.add(p)
                }
            }
            permissionsHandler.requestPermissions(permissions, this)
        }
    }

    private fun populateLayout() {
        aware_participant_study_info.aware_participant_title.text = AwareParticipantItems.awareParticipantItems[0].title
        aware_participant_study_info.aware_participant_description.text = AwareParticipantItems.awareParticipantItems[0].description
        aware_participant_study_info.aware_participant_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        aware_participant_study_info.aware_participant_image.setImageResource(AwareParticipantItems.awareParticipantItems[0].image)

        aware_participant_sync.aware_participant_title.text = AwareParticipantItems.awareParticipantItems[2].title
        aware_participant_sync.aware_participant_description.text = AwareParticipantItems.awareParticipantItems[2].description
        aware_participant_sync.aware_participant_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        aware_participant_sync.aware_participant_image.setImageResource(AwareParticipantItems.awareParticipantItems[2].image)
        aware_participant_sync.aware_participant_item.setOnClickListener {
            Toast.makeText(applicationContext, "Syncing data...", Toast.LENGTH_SHORT).show()
            val sync = Intent(Aware.ACTION_AWARE_SYNC_DATA)
            sendBroadcast(sync)
        }

        aware_participant_quit_study.aware_participant_title.text = AwareParticipantItems.awareParticipantItems[3].title
        aware_participant_quit_study.aware_participant_description.text = AwareParticipantItems.awareParticipantItems[3].description
        aware_participant_quit_study.aware_participant_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        aware_participant_quit_study.aware_participant_image.setImageResource(AwareParticipantItems.awareParticipantItems[3].image)
        aware_participant_quit_study.aware_participant_item.setOnClickListener {
            val quitStudy = Intent(this@AwareParticipant, AwareJoinStudy::class.java)
            quitStudy.putExtra(AwareJoinStudy.EXTRA_STUDY_URL, Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER))
            startActivity(quitStudy)
        }
    }

    private fun populateRevokedPermissionLayout() {
        aware_participant_revoked_permission.aware_participant_title.text = AwareParticipantItems.awareParticipantItems[1].title
        aware_participant_revoked_permission.aware_participant_description.text = AwareParticipantItems.awareParticipantItems[1].description
        aware_participant_revoked_permission.aware_participant_image.setImageResource(AwareParticipantItems.awareParticipantItems[1].image)

        val backgroundDrawable = ContextCompat.getDrawable(aware_participant_revoked_permission.aware_participant_card.context, R.drawable.item_background_2) as GradientDrawable
        aware_participant_revoked_permission.aware_participant_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red))
        aware_participant_revoked_permission.aware_participant_item.background = backgroundDrawable

        aware_participant_revoked_permission.aware_participant_item.visibility = View.VISIBLE

        val mobileVersionPermission = permissionsHandler.getDistinctPermissionsList(revokedPermissions)
        val permissionString = mobileVersionPermission!!.joinToString(separator = ", ")
        val message = "Permissions are required to continue in a study. Tap on " +
                "\"Go to settings\", click on \"Permissions\" and Please select " +
                "\"Allow\" or \"Allow only while using the app\" or \"Ask every time\" for " +
                "the following permissions: $permissionString"
        aware_participant_revoked_permission.aware_participant_item.setOnClickListener {
            AlertDialog.Builder(this@AwareParticipant)
                .setTitle("Grant Permissions Manually")
                .setMessage(message)
                .setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                })
                .setPositiveButton("go to settings", DialogInterface.OnClickListener { _, _ ->
                    permissionsHandler.openAppSettings()
                })
                .show()
        }
    }

    private fun removeRevokedPermissionLayout() {
        aware_participant_revoked_permission.aware_participant_item.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()

        populateLayout()

        updatePermissionList()

        if(revokedPermissions.isNotEmpty()) {
            Log.d("Permissions123", "revokedPermissions is not null or not empty")
            populateRevokedPermissionLayout()
        }else {
            Log.d("Permissions123", "revokedPermissions is either null or empty")
            removeRevokedPermissionLayout()
        }
    }

    private fun updatePermissionList() {
        for(permission in revokedPermissions) {
            if(permissionsHandler.isPermissionGranted(permission)) {
                revokedPermissions.remove(permission)
            }
        }
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
                val qrcode = Intent(this@AwareParticipant, Aware_QRCode::class.java)
                qrcode.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(qrcode)
            }
        }
        if (item.title.toString().equals(resources.getString(R.string.aware_study), ignoreCase = true)) {
            val studyInfo = Intent(this@AwareParticipant, AwareJoinStudy::class.java)
            studyInfo.putExtra(AwareJoinStudy.EXTRA_STUDY_URL, Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER))
            startActivity(studyInfo)
        }
        if (item.title.toString().equals(resources.getString(R.string.aware_team), ignoreCase = true)) {
            val about_us = Intent(this@AwareParticipant, About::class.java)
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
        // This does not get called when you grant permission!
        Log.d("Permissions123", "onGranted!")

        for(permission in revokedPermissions) {
           if(permissionsHandler.isPermissionGranted(permission)) {
               Log.d("Permissions123", "permissions revoked!")
               revokedPermissions.remove(permission)
           }
        }

        if(revokedPermissions.isEmpty()) {
            Log.d("Permissions123", "remove the layout")
            removeRevokedPermissionLayout()
        }
        val redirectService = Intent()
        redirectService.action = Aware_Client.ACTION_AWARE_PERMISSIONS_CHECK
        val component = intent.getStringExtra(Aware_Client.EXTRA_REDIRECT_SERVICE)!!
            .split("/").toTypedArray()
        redirectService.component = ComponentName(component[0], component[1])
        startService(redirectService)
    }

    override fun onPermissionDenied(deniedPermissions: List<String>?) {

        AlertDialog.Builder(this)
            .setTitle("Aware: Permanently Denied Permissions")
            .setMessage("You have permanently denied necessary permissions. Please follow the " +
                    "instructions in the \"Revoked Permissions\" section to grant permissions")
            .setNegativeButton("ok", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
            .show()
    }

    override fun onPermissionDeniedWithRationale(deniedPermissions: List<String>?) {
        /*
            This activity is responsible for handling permissions that have been revoked
            and does not receive any rationale as revoked permissions do not trigger this
            part of the permission flow.
         */
    }

    data class AwareParticipantItem(
        val title: String,
        val description: String,
        val image: Int,
        val card: Int,
        val container: Int
    )

    object AwareParticipantItems {

        var revokedPermissions = mutableListOf<String>()

        val awareParticipantItems = mutableListOf(
            AwareParticipantItem(
                "AWARE Study",
                "Device Id: {Aware.getSetting(Aware_Participant.context, Aware_Preferences.DEVICE_ID)}\n" +
                        "Study URL: {Aware.getSetting(context, Aware_Preferences.WEBSERVICE_SERVER)}",
                R.drawable.ic_launcher_aware,
                R.id.aware_participant_card,
                R.drawable.item_background
            ),
            AwareParticipantItem(
                "Revoked Permission",
                "Instructions on granting permissions in phone settings",
                R.drawable.ic_warning,
                R.id.aware_participant_card,
                R.drawable.item_background_2
            ),
            AwareParticipantItem(
                "Sync Data",
                "Send any pending data to the AWARE server",
                R.drawable.ic_sync,
                R.id.aware_participant_card,
                R.drawable.item_background
            ),
            AwareParticipantItem(
                "Quit Study",
                "Quit a study you're currently enrolled in",
                R.drawable.ic_quit,
                R.id.aware_participant_card,
                R.drawable.item_background
            )
        )
    }
}