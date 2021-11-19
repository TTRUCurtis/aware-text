package com.aware.phone.ui.registration;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.Aware_Join_Study;
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

public class FetchStudyData extends AsyncTask<JSONObject, Void, JSONObject> {

    private final Application application;
    private final Listener listener;

    private String study_url = "";
    private String study_api_key = "";
    private String study_id = "";
    private String study_config = "";

    public FetchStudyData(Application application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.onPreExecute();
    }

    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        try {
            study_url = params[0].getString("study_url");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (study_url.length() == 0) {
            Log.e(Aware.TAG, "Aware_QRCode study_url? " + study_url);
            return null;
        }

        if (Aware.DEBUG) Log.d(Aware.TAG, "Aware_QRCode study_url: " + study_url);

        Uri study_uri = Uri.parse(study_url);
        String protocol = study_uri.getScheme();

        List<String> path_segments = study_uri.getPathSegments();

        if (path_segments.size() > 0) {
            study_api_key = path_segments.get(path_segments.size() - 1);
            study_id = path_segments.get(path_segments.size() - 2);

            String request;
            if (protocol.equals("https")) {
                //Note: Joining a study always downloads the certificate.
                SSLManager.handleUrl(application, study_url, true);

                while (!SSLManager.hasCertificate(application, study_uri.getHost())) {
                    //wait until we have the certificate downloaded
                }

                try {
                    request = new Https(SSLManager.getHTTPS(application, study_url)).dataGET(study_url.substring(0, study_url.indexOf("/index.php")) + "/index.php/webservice/client_get_study_info/" + study_api_key, true);
                } catch (FileNotFoundException e) {
                    request = null;
                }
            } else {
                request = new Http().dataGET(study_url.substring(0, study_url.indexOf("/index.php")) + "/index.php/webservice/client_get_study_info/" + study_api_key, true);
            }

            if (request != null) {
                try {
                    if (request.equals("[]")) {
                        return null;
                    }
                    JSONObject study_data = new JSONObject(request);

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

                    String answer;
                    if (protocol.equals("https")) {
                        try {
                            answer = new Https(SSLManager.getHTTPS(application, study_url)).dataPOST(study_url, data, true);
                        } catch (FileNotFoundException e) {
                            answer = null;
                        }
                    } else {
                        answer = new Http().dataPOST(study_url, data, true);
                    }

                    if (answer != null) {
                        try {
                            JSONArray configs_study = new JSONArray(answer);
                            if (!configs_study.getJSONObject(0).has("message")) {
                                study_config = configs_study.toString();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else return null;

                    return study_data;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
//            Toast.makeText(listener, "Missing API key or study ID. Scanned: " + study_url, Toast.LENGTH_SHORT).show();
            //TODO can't do UI work from background thread. Instead return null or an error and handle in post execute
        }
        return null;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        super.onPostExecute(result);

        try {
//            loader.dismiss();
        } catch (IllegalArgumentException e) {
            //It's ok, we might get here if we couldn't get study info.
            return;
        }

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
            listener.onPostExecute(); //TODO implement error handling in listener
        } else {

            try {
                Cursor dbStudy = Aware.getStudy(application, study_url);

                if (Aware.DEBUG)
                    Log.d(Aware.TAG, DatabaseUtils.dumpCursorToString(dbStudy));

                if (dbStudy == null || !dbStudy.moveToFirst()) {
                    ContentValues studyData = new ContentValues();
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_DEVICE_ID, Aware.getSetting(application, Aware_Preferences.DEVICE_ID));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_TIMESTAMP, System.currentTimeMillis());
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, study_id);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_API, study_api_key);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, study_url);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_PI, result.getString("researcher_first") + " " + result.getString("researcher_last") + "\nContact: " + result.getString("researcher_contact"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, study_config);
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
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_KEY, study_id);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_API, study_api_key);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_URL, study_url);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_PI, result.getString("researcher_first") + " " + result.getString("researcher_last") + "\nContact: " + result.getString("researcher_contact"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_CONFIG, study_config);
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_TITLE, result.getString("study_name"));
                    studyData.put(Aware_Provider.Aware_Studies.STUDY_DESCRIPTION, result.getString("study_description"));

                    application.getContentResolver().insert(Aware_Provider.Aware_Studies.CONTENT_URI, studyData);

                    if (Aware.DEBUG) {
                        Log.d(Aware.TAG, "Re-scanned study data: " + studyData.toString());
                    }
                }

                if (dbStudy != null && !dbStudy.isClosed()) dbStudy.close();

                //Load join study wizard. We already have the study info on the database.
//                Intent studyInfo = new Intent(application, Aware_Join_Study.class);
//                studyInfo.putExtra(Aware_Join_Study.EXTRA_STUDY_URL, study_url);
//                studyInfo.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                listener.startActivity(studyInfo);
//
//                listener.finish();
                //TODO implement success scenario to start call for getting metadata
                listener.onPostExecute();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    interface Listener {
        void onPreExecute();
        void onPostExecute();
    }
}


