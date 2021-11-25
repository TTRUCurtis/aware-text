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

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.PermissionUtils;
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
import java.util.UUID;

/**
 * Fetch study information and ask user to join the study
 */

public class GetStudyMetadata extends AsyncTask<Uri, Void, JSONObject> {

    private final Application application;
    private final Listener listener;

    private String studyUrl = "";
    private String studyApiKey = "";
    private String studyId = "";
    private String studyConfig = "";
    private JSONObject studyAndSurveyUrls;

    public GetStudyMetadata(Application application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected JSONObject doInBackground(Uri... params) {
        studyAndSurveyUrls = fetchStudyAndSurveyUrls(params[0]);
        try {
            if (studyAndSurveyUrls != null) {
                studyUrl = studyAndSurveyUrls.getString("study_url");
            } else {
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Uri studyUri = Uri.parse(studyUrl);
        String protocol = studyUri.getScheme();

        List<String> pathSegments = studyUri.getPathSegments();

        studyApiKey = pathSegments.get(pathSegments.size() - 1);
        studyId = pathSegments.get(pathSegments.size() - 2);

        String request;
        //TODO will we have a protocol other than https?
        if (protocol.equals("https")) {
            //Note: Joining a study always downloads the certificate. TODO is it downloading the certificate here? If not, then when/where
            SSLManager.handleUrl(application, studyUrl, true);

            while (!SSLManager.hasCertificate(application, studyUri.getHost())) {
                //wait until we have the certificate downloaded
            }

            try {
                request = new Https(SSLManager.getHTTPS(application, studyUrl)).dataGET(studyUrl.substring(0, studyUrl.indexOf("/index.php")) + "/index.php/webservice/client_get_study_info/" + studyApiKey, true);
            } catch (FileNotFoundException e) {
                request = null;
            }
        } else {
            request = new Http().dataGET(studyUrl.substring(0, studyUrl.indexOf("/index.php")) + "/index.php/webservice/client_get_study_info/" + studyApiKey, true);
        }

        if (request != null) {
            try {
                if (request.equals("[]")) {
                    return null;
                }
                JSONObject studyInfo = new JSONObject(request);

                //Automatically register this device on the study and create credentials for this device ID!
                if (Aware.getSetting(application, Aware_Preferences.DEVICE_ID).length() == 0) {
                    UUID uuid = UUID.randomUUID();
                    Aware.setSetting(application, Aware_Preferences.DEVICE_ID, uuid.toString(), "com.aware.phone");
                }
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

                //TODO this is confusing having 2 calls here, maybe break them out into different methods?
                String answer;
                if (protocol.equals("https")) {
                    try {
                        answer = new Https(SSLManager.getHTTPS(application, studyUrl)).dataPOST(studyUrl, data, true);
                    } catch (FileNotFoundException e) {
                        answer = null;
                    }
                } else {
                    answer = new Http().dataPOST(studyUrl, data, true);
                }

                if (answer != null) {
                    try {
                        JSONArray studyConfig = new JSONArray(answer);
                        if (!studyConfig.getJSONObject(0).has("message")) {
                            this.studyConfig = studyConfig.toString();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else return null;

                return studyInfo;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        super.onPostExecute(result);
        if (result == null) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(listener);
//            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    listener.setResult(Activity.RESULT_CANCELED);
//                    listener.finish();
//                }
//            });
//            builder.setTitle("Study information");
//            builder.setMessage("Unable to retrieve this study information: " + study_url + "\nTry again later.");
//            builder.show();
            listener.onPostExecute(null); //TODO dismiss loader and implement error handling in listener
        } else {
            //TODO should this be done on a separate thread?
            try {

                Cursor dbStudy = Aware.getStudy(application, studyUrl);

                if (Aware.DEBUG)
                    Log.d(Aware.TAG, DatabaseUtils.dumpCursorToString(dbStudy));

                if (dbStudy == null || !dbStudy.moveToFirst()) {
                    ContentValues studyData = new ContentValues();
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(application, Aware_Preferences.DEVICE_ID));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_TIMESTAMP, System.currentTimeMillis());
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, studyId);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_API, studyApiKey);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, studyUrl);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_PI, result.getString("researcher_first") + " " + result.getString("researcher_last") + "\nContact: " + result.getString("researcher_contact"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, studyConfig);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_TITLE, result.getString("study_name"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION, result.getString("study_description"));

                    application.getContentResolver().insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData);

                    if (Aware.DEBUG) {
                        Log.d(Aware.TAG, "New study data: " + studyData.toString());
                    }
                } else {
                    //Update the information to the latest
                    ContentValues studyData = new ContentValues();
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(application, Aware_Preferences.DEVICE_ID));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_TIMESTAMP, System.currentTimeMillis());
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_JOINED, 0);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_EXIT, 0);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, studyId);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_API, studyApiKey);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, studyUrl);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_PI, result.getString("researcher_first") + " " + result.getString("researcher_last") + "\nContact: " + result.getString("researcher_contact"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, studyConfig);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_TITLE, result.getString("study_name"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION, result.getString("study_description"));

                    application.getContentResolver().insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData);

                    if (Aware.DEBUG) {
                        Log.d(Aware.TAG, "Re-scanned study data: " + studyData.toString());
                    }
                }

                if (dbStudy != null && !dbStudy.isClosed()) dbStudy.close();

                listener.onPostExecute(new StudyMetadata(
                        studyUrl,
                        result.getString("study_name"),
                        result.getString("study_description"),
                        result.getString("researcher_first") + " " + result.getString("researcher_last") + "\nContact: " + result.getString("researcher_contact"),
                        PermissionUtils.getRequiredPermissions(),
                        studyAndSurveyUrls.getString("survey_url")));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private JSONObject fetchStudyAndSurveyUrls(Uri registrationUri) {
        String protocol = registrationUri.getScheme();
        String url = registrationUri.toString() + "&config=1";
        List<String> pathSegments = registrationUri.getPathSegments();

        //TODO will path segments ever be 0?
        if (pathSegments.size() > 0) {
            String participantId = pathSegments.get(pathSegments.size() - 1);

            String request = null;
            if (protocol.equals("https")) {
                //TODO do we need this?
                //Note: Joining a study always downloads the certificate.
//                SSLManager.handleUrl(application, url, true);
//
//                while (!SSLManager.hasCertificate(application, registrationUri.getHost())) {
//                    //wait until we have the certificate downloaded
//                }
                request = new Https().dataGET(url, false);
            }

            try {
                return new JSONObject(request);
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO remove this stubbed response once the server is working
                try {
                    return new JSONObject("{\n" +
                            "  \"study_url\": \"https://aware-dev.wwbp.org/index.php/webservice/index/13/pnYFx6s6gIPp\",\n" +
                            "  \"survey_url\": \"https://upenn.co1.qualtrics.com/jfe/form/SV_2mHDdjVZgAOQzg9\"\n" +
                            "}");
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }

        } else {
            //Toast.makeText(listener, "Missing API key or study ID. Scanned: " + study_url, Toast.LENGTH_SHORT).show();
            //TODO can't do UI work from background thread. Instead return null or an error and handle in post execute
        }
        return null;
    }

    public interface Listener {
        void onPostExecute(StudyMetadata studyMetadata);
    }
}


