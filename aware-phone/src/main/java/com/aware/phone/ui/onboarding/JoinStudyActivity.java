package com.aware.phone.ui.onboarding;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.phone.R;
import com.aware.phone.ui.AwareParticipant;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.studyeligibility.StudyEligibility;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

//TODO splash comes here or aware_client depending if a study is already joined.

//TODO create device ID if not created in the async task get study metadata (needed for request)

//TODO wait to load pref data into the db, if it is necessary, then you can ask if
// it should be done before joining study, if not, can leave it in the main screen.
// if should be done before loading/joining study, do it in get study metadata
// or create new task, initialize db
public class JoinStudyActivity extends AppCompatActivity implements PermissionsHandler.PermissionCallback{

    private JoinStudyViewModel viewModel;

    private ProgressDialog loader;
    private AlertDialog alertDialog;
    private AlertDialog accessibilityDialog;
    private LinearLayout joinStudyFromTextLayout;

    private LinearLayout studyMetadataLayout;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView researcherTextView;
    private TextView messageTitleTextView;
    private TextView messageDescriptionTextView;

    private Button actionButton;

    private PermissionsHandler permissionsHandler;
    private ArrayList<String> permissions;
    private StudyEligibility studyEligibility;
    private Button requestPermissionBtn;
    private TextView permissionRationale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_study);

        permissionsHandler = new PermissionsHandler(this);
        studyEligibility = new StudyEligibility(this);

        requestPermissionBtn = findViewById(R.id.request_permission);
        permissionRationale = findViewById(R.id.permission_rationale);
        permissionRationale.setVisibility(View.GONE);
        requestPermissionBtn.setVisibility(View.GONE);


        if (Aware.isStudy(this)) {
            //Redirect the user to the main UI
            Intent mainUI = new Intent(getApplicationContext(), AwareParticipant.class);
            mainUI.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainUI);
            finish();
        }

        //uri = https://survey.wwbp.org/test/download-app?pid=123
        joinStudyFromTextLayout = findViewById(R.id.layout_join_study_thru_text);
        messageTitleTextView = findViewById(R.id.msg_title);
        messageDescriptionTextView = findViewById(R.id.message);
        actionButton = findViewById(R.id.btn_action);
        actionButton.setOnClickListener(v -> {
            Telephony.Sms.getDefaultSmsPackage(this);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:"));
            startActivity(intent);
        });

        observeViewModel(new ViewModelProvider(this).get(JoinStudyViewModel.class));
        checkForStudy(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkForStudy(intent);
    }

    private void observeViewModel(JoinStudyViewModel joinStudyViewModel) {
        viewModel = joinStudyViewModel;
        viewModel.getLoadingIndicator().observe(this, loadingIndicator -> {
            if (loadingIndicator != null) {
                if (loader == null) {
                    loader = new ProgressDialog(JoinStudyActivity.this);
                    loader.setCancelable(false);
                    loader.setIndeterminate(true);
                }
                loader.setTitle(loadingIndicator.getTitle());
                loader.setMessage(loadingIndicator.getMessage());
                loader.show();
            } else {
                if (loader != null) {
                    loader.dismiss();
                }
            }
        });

        viewModel.getErrorMsgLiveData().observe(this, errorMsg -> {
            if (errorMsg != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    viewModel.dismissErrorDialog();
                    dialog.dismiss();
                });
                builder.setTitle("Error retrieving study metadata");
                builder.setMessage(errorMsg);
                builder.show();
            }
        });

        viewModel.getStudyMetadataLiveData().observe(this, studyMetadata -> {
            joinStudyFromTextLayout.setVisibility(View.GONE);
            if (studyMetadataLayout == null) {
                studyMetadataLayout = findViewById(R.id.layout_study_info);
                titleTextView = findViewById(R.id.txt_title);
                descriptionTextView = findViewById(R.id.txt_description);
                researcherTextView = findViewById(R.id.txt_researcher);
            }
            studyMetadataLayout.setVisibility(View.VISIBLE);
            titleTextView.setText(studyMetadata.getName());
            descriptionTextView.setText(Html.fromHtml(studyMetadata.getDescription()));
            researcherTextView.setText(studyMetadata.getResearcher());

            actionButton.setText("Join Study");
            messageTitleTextView.setText("Join Study");
            messageDescriptionTextView.setText("Click on button below to complete registration.");
            actionButton.setOnClickListener(v -> {
                viewModel.joinStudy();
            });

            if(!studyEligibility.hasEligibilityBeenChecked() && studyMetadata.getConfiguration() != null) {
                try {
                    studyEligibility.checkForSmsPluginStatus(new JSONArray(studyMetadata.getConfiguration()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(studyEligibility.isSmsPluginEnabled()) {
                permissions = studyMetadata.getPermissions();
                studyEligibility.showSMSPermissionDialog(permissionsHandler, this);
            } else {
                permissionsHandler.requestPermissions(studyMetadata.getPermissions(), this);
            }

            if (studyMetadata.showPermissionsNoticeDialog()) {
                actionButton.setEnabled(false);
                if (!Aware.is_watch(this)) {
                    Applications.isAccessibilityEnabled(this);
                }
            } else {
                actionButton.setEnabled(true);
            }
        });

        viewModel.getJoinedStudySuccessMsg().observe(this, joinedStudyMessage -> {
            if (joinedStudyMessage.showSuccessDialog()) {
                final SpannableString spannableMsg =
                        new SpannableString(joinedStudyMessage.getDescription() + joinedStudyMessage
                                .getSurveyUrl());
                Linkify.addLinks(spannableMsg, Linkify.ALL);
                alertDialog = new AlertDialog.Builder(this)
                        .setTitle(joinedStudyMessage.getTitle())
                        .setMessage(spannableMsg)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            joinedStudyMessage
                                    .setShowSuccessDialog(false); //no need to show the dialog again after dismissal
                        }).show();
                ((TextView) alertDialog.findViewById(android.R.id.message))
                        .setMovementMethod(LinkMovementMethod.getInstance());
            }

            findViewById(R.id.txt_do_not_close_aware).setVisibility(View.VISIBLE);
            TextView joinedSuccessfully = findViewById(R.id.txt_study_successfully_joined);
            joinedSuccessfully.setVisibility(View.VISIBLE);
            joinedSuccessfully.setText(MessageFormat
                    .format("{0}.\n{1}{2}", joinedStudyMessage.getTitle(), joinedStudyMessage
                            .getDescription(), joinedStudyMessage.getSurveyUrl()));
            joinedSuccessfully.setMovementMethod(LinkMovementMethod.getInstance());

            actionButton.setText("Done");
            actionButton.setOnClickListener(v -> {
                //Redirect the user to the main UI
                Intent mainUI = new Intent(getApplicationContext(), AwareParticipant.class);
                mainUI.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainUI);
                finish();
            });
        });
    }

    private void checkForStudy(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            viewModel.loadStudy(uri);
        }
    }

    private void requestIgnoreBatteryOptimization() {
        Intent whitelisting = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        whitelisting.setData(Uri.parse("package:" + getPackageName()));
        whitelistingResult.launch(whitelisting);
    }

    private ActivityResultLauncher<Intent> whitelistingResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (!Aware.isBatteryOptimizationIgnored(this, getPackageName())) {
                    new AlertDialog.Builder(this)
                            .setMessage("To proceed, please allow AWARE to run in the background.")
                            .setPositiveButton("ok", (dialog, which) -> requestIgnoreBatteryOptimization())
                            .show();
                }else if(Aware.isBatteryOptimizationIgnored(this, getPackageName())) {
                    grantAccessibility();
                }
            }
    );

    private void grantAccessibility() {
        if (!Aware.is_watch(JoinStudyActivity.this)) {
            if (accessibilityDialog == null) {
                accessibilityDialog = new AlertDialog.Builder(JoinStudyActivity.this)
                        .setMessage("AWARE requires Accessibility access to participate in studies. " +
                                "Please click \"SETTINGS\" and turn on Accessibility access to continue.")
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                permissionsHandler.openAccessibilitySettings();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (!isFinishing() && !isDestroyed()) {
                                    permissionsHandler.openAccessibilitySettings();
                                }
                                accessibilityDialog = null;
                            }
                        })
                        .create();
            }

            if (!accessibilityDialog.isShowing() && !isFinishing() && !isDestroyed()) {
                accessibilityDialog.show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Aware.isBatteryOptimizationIgnored(this, getPackageName())) {
            if (!Applications.isAccessibilityEnabled(this)) {
                grantAccessibility();
            } else {
                if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
                    accessibilityDialog.dismiss();
                    accessibilityDialog = null;
                }
                actionButton.setEnabled(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loader != null && loader.isShowing()) {
            loader.cancel(); //was leaking a window, so need to dismiss here
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
        }
        if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
            accessibilityDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onPermissionGranted() {

        if (studyEligibility.isSmsPluginEnabled() && !studyEligibility.hasEligibilityBeenChecked()) {
            studyEligibility.performStudyEligibilityCheck(this::handleStudyEligibilityResult);
        } else {
            requestIgnoreBatteryOptimization();
        }
    }

    private void handleStudyEligibilityResult(boolean isEligible) {

        String message = isEligible ? "You passed!" : "You did not pass!";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .create();

        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();

                if (isEligible) {
                    permissionsHandler.requestPermissions(permissions, JoinStudyActivity.this);
                } else {
                    studyEligibility.markEligibilityAsUnchecked();
                    actionButton.setEnabled(true);
                    actionButton.setText("Retry");
                    messageTitleTextView.setText("Unable to register for this study");
                    messageDescriptionTextView.setText("You did not meet the minimum requirements for this study." +
                            "If you feel this is an error hit retry or contact the study administrator at someemail@nih.gov.");
                    actionButton.setOnClickListener(v -> {
                        startActivity(
                                new Intent(JoinStudyActivity.this, JoinStudyActivity.class)
                        );
                        finish();
                    });
                }
            }
        }, 2000);
    }

    @Override
    public void onPermissionDenied(List<String> deniedPermissions) {

        requestPermissionBtn.isEnabled();
        actionButton.setVisibility(View.GONE);
        permissionRationale.setVisibility(View.VISIBLE);
        requestPermissionBtn.setVisibility(View.VISIBLE);

        requestPermissionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null)
                ));
            }
        });

    }

    @Override
    public void onPermissionDeniedWithRationale(List<String> deniedPermissions) {

        new AlertDialog.Builder(this)
                .setTitle("Permissions Required to Join Study")
                .setMessage("Permissions are required to join this study. Press OK " +
                        "to review the required permissions. Please select \"Allow\" or \"While using the app\" for all permissions.")
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                permissionsHandler.requestPermissions(deniedPermissions, JoinStudyActivity.this);
                            }
                        })
                .show();

    }


}