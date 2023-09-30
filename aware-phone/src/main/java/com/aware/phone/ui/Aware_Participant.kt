package com.aware.phone.ui

import android.Manifest
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.phone.Aware_Client
import com.aware.phone.R
import com.aware.ui.PermissionsHandler
import kotlinx.android.synthetic.main.aware_participant_item_layout.view.*
import kotlinx.android.synthetic.main.aware_ui_participant.*


class Aware_Participant : AppCompatActivity(), PermissionsHandler.PermissionCallback {

    private lateinit var permissionsHandler: PermissionsHandler
    private lateinit var awareParticipantItems: MutableList<AwareParticipantItem>
    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var image: ImageView
    private lateinit var card: CardView
    private lateinit var container: ConstraintLayout
    private lateinit var layoutRevokedPermissions: View
    private lateinit var itemRevokedPermission: AwareParticipantItem

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        (supportActionBar as ActionBar).setDisplayHomeAsUpEnabled(false)
        (supportActionBar as ActionBar).setDisplayShowHomeEnabled(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aware_ui_participant)
        permissionsHandler = PermissionsHandler(this)
        awareParticipantItems = mutableListOf(
            AwareParticipantItem(
                "AWARE Study",
                "Device Id: ${Aware.getSetting(this, Aware_Preferences.DEVICE_ID)}\n" +
                        "Study URL: ${Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER)}",
                R.drawable.ic_launcher_aware,
                R.id.aware_participant_card,
                R.drawable.item_background
            ),
            AwareParticipantItem(
                "Revoked Permission",
                "Instructions on granting permissions in phone settings",
                R.drawable.ic_warning,
                R.id.aware_participant_card,
                R.drawable.item_background
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

        val layoutAwareStudy = aware_participant_study_info
        layoutRevokedPermissions = aware_participant_revoked_permission
        val layoutSyncData = aware_participant_sync
        val layoutQuitStudy = aware_participant_quit_study

        layoutRevokedPermissions.visibility = View.GONE

        val itemAwareStudy = awareParticipantItems[0]
        itemRevokedPermission = awareParticipantItems[1]
        val itemSyncData = awareParticipantItems[2]
        val itemQuitStudy = awareParticipantItems[3]

        populateLayout(layoutAwareStudy, itemAwareStudy)
//        populateLayout(layoutRevokedPermissions, itemRevokedPermission)
        populateLayout(layoutSyncData, itemSyncData)
        populateLayout(layoutQuitStudy, itemQuitStudy)

        if (intent != null && intent.extras != null && intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) != null) {

            val permissions =
                intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) as java.util.ArrayList<String>?
            permissionsHandler.requestPermissions(permissions!!, this)
        }
    }

    private fun populateLayout(layout: View, item: AwareParticipantItem) {



        title = layout.aware_participant_title
        description = layout.aware_participant_description
        image = layout.aware_participant_image
        card = layout.aware_participant_card
        container = layout.aware_participant_item

        title.text = item.title
        description.text = item.description
        image.setImageResource(item.image)
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary))

//        if(title.text == "Revoked Permission") {
//            val backgroundDrawable = ContextCompat.getDrawable(container.context, R.drawable.item_background) as GradientDrawable
//            backgroundDrawable.setStroke(2, ContextCompat.getColor(container.context, R.color.red))
//            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red))
//            container.background = backgroundDrawable
//            container.visibility = View.VISIBLE
//        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
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

        AlertDialog.Builder(this)
            .setTitle("Aware: Permanently Denied Permissions")
            .setMessage("You have permanently denied necessary permissions. Please follow the " +
                    "instructions in the \"Revoked Permissions\" section to grant permissions")
            .setNegativeButton("ok", DialogInterface.OnClickListener { _, _ ->
                Log.d("Permissions123", "Inside neg butt")
                //populateLayout(layoutRevokedPermissions, itemRevokedPermission)
                title = layoutRevokedPermissions.aware_participant_title
                description = layoutRevokedPermissions.aware_participant_description
                image = layoutRevokedPermissions.aware_participant_image
                card = layoutRevokedPermissions.aware_participant_card
                container = layoutRevokedPermissions.aware_participant_item

                title.text = itemRevokedPermission.title
                description.text = itemRevokedPermission.description
                image.setImageResource(itemRevokedPermission.image)

                val backgroundDrawable = ContextCompat.getDrawable(container.context, R.drawable.item_background) as GradientDrawable
                backgroundDrawable.setStroke(2, ContextCompat.getColor(container.context, R.color.red))
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red))
                container.background = backgroundDrawable

                layoutRevokedPermissions.visibility = View.VISIBLE

            })
            .show()
//        startActivity(
//            Intent(
//                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                Uri.fromParts("package", packageName, null)
//            )
//        )
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

}