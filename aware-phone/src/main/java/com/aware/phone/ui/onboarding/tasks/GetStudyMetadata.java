package com.aware.phone.ui.onboarding.tasks;

import android.app.Application;
import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.PermissionUtils;
import com.aware.phone.ui.onboarding.data.Result;
import com.aware.phone.ui.onboarding.data.StudyMetadata;
import com.aware.providers.Aware_Provider;
import com.aware.utils.Http;
import com.aware.utils.Https;
import com.aware.utils.SSLManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.List;

/**
 * Fetch study information and ask user to join the study
 */

public class GetStudyMetadata extends AsyncTask<Uri, Void, Result<StudyMetadata>> {

    private final static String TAG = GetStudyMetadata.class.getSimpleName();

    private final Application application;
    private final Listener listener;

    public GetStudyMetadata(Application application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected @NonNull
    Result<StudyMetadata> doInBackground(Uri... params) {
        Uri registrationData = params[0];
        String participantId = registrationData.getQueryParameter("pid");

        if (participantId == null) {
            return Result.error("Unable to find participant ID from provided info: " + registrationData);
        } else {
            Aware.setSetting(application, Aware_Preferences.DEVICE_ID, participantId);
        }

        Result<StudyMetadata.Builder> result = getStudyAndSurveyUrls(registrationData);

        if (result.hasError()) {
            return Result.error(result.getErrorMsg()); //immediately return since we can't do
            // anything else
        } else return addRemainingStudyMetadata(result);
    }

    @NonNull
    private Result<StudyMetadata> addRemainingStudyMetadata(Result<StudyMetadata.Builder> result) {
        StudyMetadata.Builder studyBuilder = result.getData();
        assert studyBuilder != null;
        String request = getStudyInfo(studyBuilder);

        if (request != null) {
            try {
                if (request.equals("[]")) {
                    return Result.error("Study info is blank!");
                }
                JSONObject studyInfo = new JSONObject(request);

                Result<StudyMetadata.Builder> studyBuilderWithConfig = addStudyConfig(studyBuilder);
                if (studyBuilderWithConfig.hasError()) return Result.error(studyBuilderWithConfig.getErrorMsg());

                assert studyBuilderWithConfig.getData() != null;

                return Result.data(
                        studyBuilderWithConfig.getData()
                                .setName(studyInfo.getString("study_name"))
                                .setDescription(studyInfo.getString("study_description"))
                                .setResearcher(studyInfo
                                        .getString("researcher_first") + " " + studyInfo
                                        .getString("researcher_last") + "\nContact: " + studyInfo
                                        .getString("researcher_contact"))
                                .setPermissions(PermissionUtils.populatePermissionsList(new JSONArray(studyBuilderWithConfig.getData().configuration)))
                                .build());
            } catch (JSONException e) {
                e.printStackTrace();
                return Result.error("There was a problem retrieving the study information");

            }
        } else return Result.error("There was a problem retrieving the study information");
    }

    @Nullable
    private String getStudyInfo(StudyMetadata.Builder studyBuilder) {
        Uri studyUri = Uri.parse(studyBuilder.url);
        List<String> pathSegments = studyUri.getPathSegments();
        String studyApiKey = pathSegments.get(pathSegments.size() - 1);

        String protocol = studyUri.getScheme();

        String request;
        //TODO will we have a protocol other than https?
        if (protocol.equals("https")) {
            //Note: Joining a study always downloads the certificate.
            SSLManager.handleUrl(application, studyBuilder.url, true);

            while (!SSLManager.hasCertificate(application, studyUri.getHost())) {
                //wait until we have the certificate downloaded
            }

            try {
                request =
                        new Https(SSLManager.getHTTPS(application, studyBuilder.url)).dataGET(studyBuilder.url
                        .substring(0, studyBuilder.url
                                .indexOf("/index.php")) + "/index.php/webservice/client_get_study_info/" + studyApiKey, true);
            } catch (FileNotFoundException e) {
                request = null;
            }
        } else {
            request = new Http().dataGET(studyBuilder.url.substring(0, studyBuilder.url
                    .indexOf("/index.php")) + "/index.php/webservice/client_get_study_info/" + studyApiKey, true);
        }
        return request;
    }

    @NonNull
    private Result<StudyMetadata.Builder> addStudyConfig(StudyMetadata.Builder studyMetadata) {
        Uri studyUri = Uri.parse(studyMetadata.url);
        String protocol = studyUri.getScheme();

        //Automatically register this device on the study and create credentials for this device ID!
        Hashtable<String, String> data = new Hashtable<>();
        data.put(Aware_Preferences.DEVICE_ID, Aware.getSetting(application, Aware_Preferences.DEVICE_ID));
        data.put("platform", "android");
        try {
            PackageInfo package_info = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
            data.put("package_name", package_info.packageName);
            data.put("package_version_code", String.valueOf(package_info.versionCode));
            data.put("package_version_name", String.valueOf(package_info.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(Aware.TAG, "Failed to put package info: " + e);
            e.printStackTrace();
        }

        String studyUrl = studyMetadata.url;
        String response;
        if (protocol.equals("https")) {
            try {
                response = new Https(SSLManager.getHTTPS(application, studyUrl))
                        .dataPOST(studyUrl, data, true);
            } catch (FileNotFoundException e) {
                return Result.error("Certificate not found for " + studyUrl);
            }
        } else {
            response = new Http().dataPOST(studyUrl, data, true);
        }

        if (response != null) {
            try {
                JSONArray studyConfig = new JSONArray(response);
                if (!studyConfig.getJSONObject(0).has("message")) {
                    studyMetadata.setConfiguration(studyConfig.toString());
                    return Result.data(studyMetadata);
                } else {
                    String message = studyConfig.getJSONObject(0).getString("message");
                    return Result.error("Message from server: " + message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return Result.error("There was a problem with the response data from get study " +
                        "config");
            }
        } else return Result.error("There was a problem retrieving the study's " +
                "configuration settings");
    }

    @Override
    protected void onPostExecute(Result<StudyMetadata> result) {
        super.onPostExecute(result);
        if (result.hasError()) {
            listener.onError(result.getErrorMsg());
        } else {
            //TODO should this be done on a separate thread?
            StudyMetadata studyMetadata = result.getData();

            String studyUrl = studyMetadata.getUrl();
            Cursor dbStudy = Aware.getStudy(application, studyUrl);

            if (Aware.DEBUG)
                Log.d(Aware.TAG, DatabaseUtils.dumpCursorToString(dbStudy));

            if (dbStudy == null || !dbStudy.moveToFirst()) {
                ContentValues studyData = new ContentValues();
                studyData.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware
                        .getSetting(application, Aware_Preferences.DEVICE_ID));
                studyData.put(Aware_Provider.Aware_Studies.STUDY_TIMESTAMP, System
                        .currentTimeMillis());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, getStudyId(studyMetadata));
                studyData.put(Aware_Provider.Aware_Studies.STUDY_API, getStudyApiKey(studyMetadata));
                studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, studyUrl);
                studyData.put(Aware_Provider.Aware_Studies.STUDY_PI, studyMetadata.getResearcher());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG,
                        studyMetadata.getConfiguration());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_TITLE, studyMetadata.getName());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                        studyMetadata.getDescription());

                application.getContentResolver()
                        .insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData);

                if (Aware.DEBUG) {
                    Log.d(Aware.TAG, "New study data: " + studyData.toString());
                }
            } else {
                //Update the information to the latest
                ContentValues studyData = new ContentValues();
                studyData.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware
                        .getSetting(application, Aware_Preferences.DEVICE_ID));
                studyData.put(Aware_Provider.Aware_Studies.STUDY_TIMESTAMP, System
                        .currentTimeMillis());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_JOINED, 0);
                studyData.put(Aware_Provider.Aware_Studies.STUDY_EXIT, 0);
                studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, getStudyId(studyMetadata));
                studyData.put(Aware_Provider.Aware_Studies.STUDY_API, getStudyApiKey(studyMetadata));
                studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, studyUrl);
                studyData.put(Aware_Provider.Aware_Studies.STUDY_PI, studyMetadata.getResearcher());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, studyMetadata.getConfiguration());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_TITLE, studyMetadata.getName());
                studyData.put(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION,
                        studyMetadata.getDescription());

                application.getContentResolver()
                        .insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData);

                if (Aware.DEBUG) {
                    Log.d(Aware.TAG, "Re-scanned study data: " + studyData.toString());
                }
            }

            if (dbStudy != null && !dbStudy.isClosed()) dbStudy.close();

            listener.onSuccess(result.getData());
        }
    }

    private String getStudyApiKey(StudyMetadata studyMetadata) {
        Uri studyUri = Uri.parse(studyMetadata.getUrl());
        List<String> pathSegments = studyUri.getPathSegments();
        return pathSegments.get(pathSegments.size() - 1);
    }

    private String getStudyId(StudyMetadata studyMetadata) {
        Uri studyUri = Uri.parse(studyMetadata.getUrl());
        List<String> pathSegments = studyUri.getPathSegments();
        return pathSegments.get(pathSegments.size() - 2);
    }

    private Result<StudyMetadata.Builder> getStudyAndSurveyUrls(Uri registrationUri) {

        Uri.Builder builder = registrationUri.buildUpon();
        builder.appendPath("config");
        String url = builder.build().toString();

        String response = new Https().dataGET(url, true);

        if (response == null) {
            return Result.error("There was a problem getting the study's web address.");
        }

        try {
            JSONObject responseJO = new JSONObject(response);
            String studyUrl = responseJO.getString("study_url");
            String surveyUrl = responseJO.getString("survey_url");

            return Result.data(
                    new StudyMetadata.Builder()
                            .setUrl(studyUrl)
                            .setSurveyUrl(surveyUrl));

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, registrationUri + " --> " + response);
            return Result.error("There was a problem with the response data from get study web " +
                    "address: " + e.getMessage());
        }
    }

    public interface Listener {
        void onSuccess(StudyMetadata studyMetadata);
        void onError(String errorMsg);
    }
}


