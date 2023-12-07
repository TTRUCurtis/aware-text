package com.aware.phone.ui.onboarding;



import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.text.MessageFormat;
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
    private LinearLayout joinStudyFromTextLayout;

    private LinearLayout studyMetadataLayout;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView researcherTextView;

    private Button actionButton;

    private PermissionsHandler permissionsHandler;
    private Button requestPermissionBtn;
    private TextView permissionRationale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_study);

        permissionsHandler = new PermissionsHandler(this);

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
            actionButton.setOnClickListener(v -> {
                viewModel.joinStudy();
            });

            if (studyMetadata.showPermissionsNoticeDialog()) {
                actionButton.setEnabled(false);
                if (!Aware.is_watch(this)) {
                    Applications.isAccessibilityServiceActive(this);
                }

                Intent whitelisting = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                whitelisting.setData(Uri.parse("package:" + getPackageName()));
                startActivity(whitelisting);
                permissionsHandler.requestPermissions(studyMetadata.getPermissions(), this);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loader != null && loader.isShowing()) {
            loader.cancel(); //was leaking a window, so need to dismiss here
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onPermissionGranted() {

        Intent mainUI = new Intent(getApplicationContext(), AwareParticipant.class);
        mainUI.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainUI);
        finish();
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