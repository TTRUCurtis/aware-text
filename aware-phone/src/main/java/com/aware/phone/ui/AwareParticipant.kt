package com.aware.phone.ui

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.phone.Aware_Client
import com.aware.phone.R
import com.aware.phone.ui.AwareParticipant.AwareParticipantItems.revokedPermissions
import com.aware.providers.Aware_Provider
import com.aware.ui.PermissionsHandler
import com.aware.utils.Scheduler
import kotlinx.android.synthetic.main.aware_item_layout.view.*
import kotlinx.android.synthetic.main.aware_ui_participant.*

class AwareParticipant : AppCompatActivity(), PermissionsHandler.PermissionCallback {

    private lateinit var permissionsHandler: PermissionsHandler
    private val esmButtons = mutableMapOf<String?, View?>()

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
        registerEsmReceiver()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun registerEsmReceiver() {

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    when (it.action) {
                        Scheduler.ACTION_AWARE_PARTICIPANT_ESM_ENABLE -> {
                            val esmId = it.getStringExtra(Scheduler.SCHEDULE_ID)
                            ap_esm_container_title.text = "FOR YOU TO COMPLETE"
                            if(!esmButtons.containsKey(esmId)) {
                                val esmTitle = it.getStringExtra(Scheduler.ESM_BUTTON_TITLE);
                                val esmView = layoutInflater.inflate(R.layout.aware_item_layout, ap_esm_container, false).apply {
                                    aware_item_title.text = esmTitle
                                    aware_item_description.text = "Respond to questionnaire"
                                    aware_item_card.setCardBackgroundColor(ContextCompat.getColor(this@AwareParticipant, R.color.primary))
                                    aware_item_image.setImageResource(R.drawable.ic_esm)
                                    aware_item.setOnClickListener {
                                        sendBroadcast(
                                            Intent("ACTION_AWARE_${esmId}")
                                        )
                                    }
                                    aware_item.setOnTouchListener { v, event ->
                                        when(event.action) {
                                            MotionEvent.ACTION_DOWN -> {
                                                v.alpha = 0.5f
                                            }
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                v.alpha = 1.0f
                                            }
                                        }
                                        false
                                    }
                                }
                                esmButtons[esmId] = esmView
                                if (ap_esm_container.childCount > 0) {
                                    val divider = View(this@AwareParticipant).apply {
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            3
                                        ).apply {
                                            setMargins(0, 0, 0, 0)
                                        }
                                        setBackgroundColor(
                                            ContextCompat.getColor(this@AwareParticipant, R.color.grey)
                                        )
                                    }
                                    ap_esm_container.addView(divider)
                                }
                                ap_esm_container.addView(esmView)
                            }
                        }
                        Scheduler.ACTION_AWARE_PARTICIPANT_ESM_DISABLE -> {
                            val esmId = it.getStringExtra(Scheduler.SCHEDULE_ID)
                            if(esmButtons.containsKey(esmId)) {
                                val view = esmButtons[esmId]
                                ap_esm_container.removeView(view)
                                esmButtons.remove(esmId)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Scheduler.ACTION_AWARE_PARTICIPANT_ESM_ENABLE)
            addAction(Scheduler.ACTION_AWARE_PARTICIPANT_ESM_DISABLE)
        }

        registerReceiver(receiver, filter)

        startService(
            Intent(this@AwareParticipant, Scheduler::class.java)
        )
    }

    private fun checkForRevokedPermissions() {
        if (intent != null && intent.extras != null) {
            if(intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) != null) {
                val permissions =
                    intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) as java.util.ArrayList<String>?

                for(p in permissions!!) {
                    if(!permissionsHandler.isPermissionGranted(p)) {
                        revokedPermissions.add(p)
                    }
                }
                permissionsHandler.requestPermissions(permissions, this)
            }
            if(intent.getStringExtra("Method") == "redirectToAccessibility") grantAccessibility()

        }
    }

    private fun grantAccessibility() {
        if (!Aware.is_watch(this)) {
            AlertDialog.Builder(this@AwareParticipant).apply {
                setMessage("AWARE requires Accessibility access to participate in studies. " +
                        "Please click \"SETTINGS\" and turn on Accessibility access to continue.")
                setPositiveButton("settings"){ dialog, _ ->
                    dialog.dismiss()
                    permissionsHandler.openAccessibilitySettings()
                }
                setOnDismissListener {
                    permissionsHandler.openAccessibilitySettings()
                }
                show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun populateLayout() {

        ap_icon_image.setImageResource(R.drawable.ic_launcher_aware)

        ap_study.text = AwareParticipantItems.awareParticipantItems[0].title
        ap_device_id.text =
            "Device Id: ${Aware.getSetting(this@AwareParticipant, Aware_Preferences.DEVICE_ID)}"
        applicationContext.contentResolver.query(Aware_Provider.Aware_Studies.CONTENT_URI, null, null, null, null)?.use {
            if(it.moveToFirst()){
                ap_study_title.text =
                    "Study Name: ${it.getString(it.getColumnIndexOrThrow(Aware_Provider.Aware_Studies.STUDY_TITLE))}"
            }
        }

        sync_item.aware_item_title.text = AwareParticipantItems.awareParticipantItems[1].title
        sync_item.aware_item_description.text = AwareParticipantItems.awareParticipantItems[1].description
        sync_item.aware_item_image.setImageResource(AwareParticipantItems.awareParticipantItems[1].image)
        sync_item.aware_item_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green))
        sync_item.aware_item.setOnClickListener {
            Toast.makeText(applicationContext, "Syncing data...", Toast.LENGTH_SHORT).show()
            val sync = Intent(Aware.ACTION_AWARE_SYNC_DATA)
            sendBroadcast(sync)
        }
        sync_item.aware_item.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
        }
        quit_study_item.aware_item_title.text = AwareParticipantItems.awareParticipantItems[2].title
        quit_study_item.aware_item_description.text = AwareParticipantItems.awareParticipantItems[2].description
        quit_study_item.aware_item_image.setImageResource(AwareParticipantItems.awareParticipantItems[2].image)
        quit_study_item.aware_item_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red))
        quit_study_item.aware_item.setOnClickListener {
            val quitStudy = Intent(this@AwareParticipant, AwareJoinStudy::class.java)
            quitStudy.putExtra(AwareJoinStudy.EXTRA_STUDY_URL, Aware.getSetting(this, Aware_Preferences.WEBSERVICE_SERVER))
            startActivity(quitStudy)
        }
        quit_study_item.aware_item.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
        }
        ap_study_options_title.text = "AWARE STUDY OPTIONS"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun populateRevokedPermissionLayout() {
        revoked_permission_item.aware_item_title.text = AwareParticipantItems.awareParticipantItems[3].title
        revoked_permission_item.aware_item_description.text = AwareParticipantItems.awareParticipantItems[3].description
        revoked_permission_item.aware_item_image.setImageResource(AwareParticipantItems.awareParticipantItems[3].image)

        revoked_permission_item.aware_item_card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.red))
        revoked_permission_item.aware_item.visibility = View.VISIBLE


        val mobileVersionPermission = permissionsHandler.getDistinctPermissionsList(revokedPermissions)
        val permissionString = mobileVersionPermission!!.joinToString(separator = ", ")
        val message = "Permissions are required to continue in a study. Tap on " +
                "\"Go to settings\", click on \"Permissions\" and Please select " +
                "\"Allow\" or \"Allow only while using the app\" or \"Ask every time\" for " +
                "the following permissions: $permissionString"
        revoked_permission_item.aware_item.setOnClickListener {
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

        revoked_permission_item.aware_item.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.5f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 0.1f
                }
            }
            false
        }

        study_options_separator.visibility = View.VISIBLE
    }

    private fun removeRevokedPermissionLayout() {
        revoked_permission_item.aware_item.visibility = View.GONE
        study_options_separator.visibility = View.GONE
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
            populateRevokedPermissionLayout()
        }else {
            removeRevokedPermissionLayout()
        }

        if(!Applications.isAccessibilityEnabled(this@AwareParticipant)) {
            grantAccessibility()
        }

    }

    private fun updatePermissionList() {
        for(permission in revokedPermissions) {
            if(permissionsHandler.isPermissionGranted(permission)) {
                revokedPermissions.remove(permission)
            }
        }
    }

    override fun onPermissionGranted() {

        for(permission in revokedPermissions) {
           if(permissionsHandler.isPermissionGranted(permission)) {
               revokedPermissions.remove(permission)
           }
        }

        if(revokedPermissions.isEmpty()) {
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
                "",
                R.drawable.ic_launcher_aware,
                R.id.aware_item_card,
                R.drawable.item_background
            ),
            AwareParticipantItem(
                "Sync Data",
                "Send data to the AWARE server",
                R.drawable.ic_sync,
                R.id.aware_item_card,
                R.drawable.item_background_3
            ),
            AwareParticipantItem(
                "Quit Study",
                "Quit a study you're currently enrolled in",
                R.drawable.ic_quit,
                R.id.aware_item_card,
                R.drawable.item_background_2
            ),
            AwareParticipantItem(
                "Revoked Permission",
                "Granting permissions from app settings",
                R.drawable.ic_error,
                R.id.aware_item_card,
                R.drawable.item_background_2
            )
        )
    }

}