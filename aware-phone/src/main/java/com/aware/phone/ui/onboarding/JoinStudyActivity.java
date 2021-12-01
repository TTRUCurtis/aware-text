package com.aware.phone.ui.onboarding;

import static com.aware.ui.PermissionsHandler.RC_PERMISSIONS;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.phone.Aware_Client;
import com.aware.phone.R;
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.Collections;

//TODO splash comes here or aware_client depending if a study is already joined.

//TODO create device ID if not created in the async task get study metadata (needed for request)

//TODO wait to load pref data into the db, if it is necessary, then you can ask if
// it should be done before joining study, if not, can leave it in the main screen.
// if should be done before loading/joining study, do it in get study metadata
// or create new task, initialize db
public class JoinStudyActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_PERMISSIONS = 1;
    private JoinStudyViewModel viewModel;

    private ProgressDialog loader;
    private LinearLayout joinStudyFromTextLayout;

    private LinearLayout studyMetadataLayout;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView researcherTextView;

    private LinearLayout reviewPermissionsLayout;
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_study);

        if (Aware.isStudy(this)) {
            //Redirect the user to the main UI
            Intent mainUI = new Intent(getApplicationContext(), Aware_Client.class);
            mainUI.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainUI);
            finish();
        }

        //uri = https://survey.wwbp.org/test/download-app?pid=123
        joinStudyFromTextLayout = findViewById(R.id.layout_join_study_thru_text);
        actionButton = findViewById(R.id.btn_action);
        actionButton.setOnClickListener(v -> {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(this);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:"));
            intent.putExtra("address", "***REMOVED***"); //TODO put number here if it's constant to open up that convo. What happens if the convo had been deleted?
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

        viewModel.getStudyMetadata().observe(this, studyMetadata -> {
            joinStudyFromTextLayout.setVisibility(View.GONE);
            if (studyMetadataLayout == null) {
                studyMetadataLayout = findViewById(R.id.layout_study_info);
                titleTextView = findViewById(R.id.txt_title);
                descriptionTextView = findViewById(R.id.txt_description);
                researcherTextView = findViewById(R.id.txt_researcher);
            }
            studyMetadataLayout.setVisibility(View.VISIBLE);
            titleTextView.setText(studyMetadata.getTitle());
            descriptionTextView.setText(Html.fromHtml(studyMetadata.getDescription()));
            researcherTextView.setText(studyMetadata.getResearcher());

            actionButton.setText("Join Study");
            actionButton.setOnClickListener(v -> {
                viewModel.joinStudy();
            });

            if (studyMetadata.getPermissions() != null) {
                actionButton.setEnabled(false);

                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required to Join Study")
                        .setMessage("A few permissions are required to join this study. Press OK to review the required permissions.")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            requestPermissions(studyMetadata.getPermissions());
                        }).show();
            } else {
                actionButton.setEnabled(true);
            }

        });
        viewModel.getJoinedStudySuccessMsg().observe(this, surveyUrl -> {
            if (surveyUrl != null) {
                final SpannableString spannableMsg = new SpannableString("Please complete this brief follow-up survey: " + surveyUrl); // msg should have url to enable clicking
                Linkify.addLinks(spannableMsg, Linkify.ALL);
                final AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setTitle("Congratulations! You've successfully registered for this study")
                        .setMessage(spannableMsg)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                        }).show();

                ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                TextView joinedSuccessfully = findViewById(R.id.txt_study_successfully_joined);
                joinedSuccessfully.setText("Congratulations! You've successfully registered for this study Please complete this brief follow-up survey: " + surveyUrl);
                joinedSuccessfully.setMovementMethod(LinkMovementMethod.getInstance());
                joinedSuccessfully.setVisibility(View.VISIBLE);
            }

            actionButton.setText("Done");
            actionButton.setOnClickListener(v -> {
                //Redirect the user to the main UI
                Intent mainUI = new Intent(getApplicationContext(), Aware_Client.class);
                mainUI.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainUI);
                finish();
            });
        });
    }

//    private void requestPermissions(ArrayList<String> requiredPermissions) {
//        Intent permissionsHandler = new Intent(this, PermissionsHandler.class);
//        permissionsHandler.putStringArrayListExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, requiredPermissions);
//        permissionsHandler.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivityForResult(permissionsHandler, REQUEST_CODE_PERMISSIONS);
//    }

    private void checkForStudy(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            viewModel.loadStudy(uri);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PERMISSIONS && resultCode == Activity.RESULT_OK) {
            //TODO add checkbox for enabling accessibility and then enable join study after they check it
            if (reviewPermissionsLayout != null && reviewPermissionsLayout.getVisibility() == View. VISIBLE) {
                reviewPermissionsLayout.setVisibility(View.GONE);
                actionButton.setEnabled(true);
            }
        } else {
            //show must accept permissions UI
            if (reviewPermissionsLayout == null) {
                reviewPermissionsLayout = findViewById(R.id.layout_review_permissions);
                Button reviewPermissionsButton = findViewById(R.id.btn_review_permissions);
                reviewPermissionsButton.setOnClickListener(v -> {
                    ArrayList<String> permissions = new ArrayList<>();
                    Collections.addAll(permissions, data.getStringArrayExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS));
                    requestPermissions(permissions);
                });
            }
            reviewPermissionsLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RC_PERMISSIONS) {
            int not_granted = 0;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    not_granted++;
                    Log.d(Aware.TAG, permissions[i] + " was not granted");
                } else {
                    Log.d(Aware.TAG, permissions[i] + " was granted");
                }
            }

            if (not_granted > 0) {
                //show must accept permissions UI
                if (reviewPermissionsLayout == null) {
                    reviewPermissionsLayout = findViewById(R.id.layout_review_permissions);
                    Button reviewPermissionsButton = findViewById(R.id.btn_review_permissions);
                    reviewPermissionsButton.setOnClickListener(v -> {
                        ArrayList<String> permissionsArrayList = new ArrayList<>();
                        Collections.addAll(permissionsArrayList, permissions);
                        requestPermissions(permissionsArrayList);
                    });
                }
                reviewPermissionsLayout.setVisibility(View.VISIBLE);
            } else {
                if (reviewPermissionsLayout != null && reviewPermissionsLayout.getVisibility() == View. VISIBLE) {
                    reviewPermissionsLayout.setVisibility(View.GONE);
                }
                actionButton.setEnabled(true);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private void requestPermissions(ArrayList<String> permissions) {
        //Check if AWARE is active on the accessibility services. Android Wear doesn't support accessibility services (no API yet...)
        if (!Aware.is_watch(this)) {
            Applications.isAccessibilityServiceActive(this);
        }

        Intent whitelisting = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        whitelisting.setData(Uri.parse("package:" + getPackageName()));
        startActivity(whitelisting);

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSIONS);
    }
}