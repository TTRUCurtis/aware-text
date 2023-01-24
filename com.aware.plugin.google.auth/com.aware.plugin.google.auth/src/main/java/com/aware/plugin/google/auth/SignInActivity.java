package com.aware.plugin.google.auth;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by denzil on 03/08/16.
 */
public class SignInActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final int REQUEST_CODE_PERMISSON_READ_PHONENUMBER = 1;

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView, mTitleText;
    private ProgressDialog mProgressDialog;
    private Button mCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusTextView = findViewById(R.id.status);

        mTitleText = findViewById(R.id.title_text);
        mTitleText.setText(String.format(getResources().getString(R.string.title_text), getApplicationContext().getApplicationInfo().nonLocalizedLabel));

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        mCancel = findViewById(R.id.cancel_login);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues row = new ContentValues();
                row.put(Provider.Google_Account.TIMESTAMP, System.currentTimeMillis());
                row.put(Provider.Google_Account.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                row.put(Provider.Google_Account.NAME, "NOT-PROVIDED");
                row.put(Provider.Google_Account.EMAIL, "NOT-PROVIDED");
                row.put(Provider.Google_Account.PHONENUMBER, "NOT-PROVIDED");
                row.put(Provider.Google_Account.PICTURE, new ByteArrayOutputStream().toByteArray());
                getContentResolver().insert(Provider.Google_Account.CONTENT_URI, row);
                finish();
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PERMISSON_READ_PHONENUMBER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSON_READ_PHONENUMBER: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestProfile()
                            .build();

                    mGoogleApiClient = new GoogleApiClient.Builder(this)
                            .enableAutoManage(this, this)
                            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                            .addScope(new Scope(Scopes.PROFILE))
                            .addScope(new Scope(Scopes.EMAIL))
                            .build();

                    SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
                    signInButton.setSize(SignInButton.SIZE_STANDARD);
                    signInButton.setScopes(gso.getScopeArray());

                    OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
                    if (opr.isDone()) {
                        Log.d(TAG, "Got cached sign-in");
                        GoogleSignInResult result = opr.get();
                        handleSignInResult(result);
                    } else {
                        showProgressDialog();
                        opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                            @Override
                            public void onResult(GoogleSignInResult googleSignInResult) {
                                dismissProgress();
                                handleSignInResult(googleSignInResult);
                            }
                        });
                    }
                } else {
                    Toast.makeText(this, "Permission Grant Error", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            new ProfileDownloader().execute(account);
            mStatusTextView.setText("Signed in as: " + account.getDisplayName());
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading");
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void dismissProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag().equals("signIn")) signIn();
    }

    private class ProfileDownloader extends AsyncTask<GoogleSignInAccount, Void, Void> {
        private Bitmap photo;
        private GoogleSignInAccount account;

        @Override
        protected Void doInBackground(GoogleSignInAccount... params) {
            account = params[0];
            try {
                if (account.getPhotoUrl() != null) {
                    URL photo_url = new URL(account.getPhotoUrl().toString());
                    HttpURLConnection conn = (HttpURLConnection) photo_url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream input = conn.getInputStream();
                    photo = BitmapFactory.decodeStream(input);
                }
            } catch (IOException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = "";

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
                mPhoneNumber = telephonyManager.getLine1Number();

            ContentValues row = new ContentValues();
            row.put(Provider.Google_Account.TIMESTAMP, System.currentTimeMillis());
            row.put(Provider.Google_Account.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            row.put(Provider.Google_Account.NAME, account.getDisplayName());
            row.put(Provider.Google_Account.EMAIL, account.getEmail());
            row.put(Provider.Google_Account.PHONENUMBER, mPhoneNumber);
            if (photo != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                row.put(Provider.Google_Account.PICTURE, stream.toByteArray());
            }
            getContentResolver().insert(Provider.Google_Account.CONTENT_URI, row);

            if (Aware.DEBUG)
                Log.d(Aware.TAG, "Google Account: " + row.toString());

            Plugin.accountDetails = row;
            if (Plugin.contextProducer != null) Plugin.contextProducer.onContext();

            finish();
        }
    }
}