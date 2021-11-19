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

public class FetchStudyUrl extends AsyncTask<Uri, Void, JSONObject> {

    private final Application application;
    private final Listener listener;

    private Uri registrationUri;
    private String participantId = "";
    private String study_id = "";
    private String study_config = "";

    public FetchStudyUrl(Application application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
//        loader = new ProgressDialog(listener);
//        loader.setTitle("Loading study");
//        loader.setMessage("Please wait...");
//        loader.setCancelable(false);
//        loader.setIndeterminate(true);
//        loader.show();
        listener.onPreExecute();
    }

    @Override
    protected JSONObject doInBackground(Uri... params) {
        registrationUri = params[0];

        String protocol = registrationUri.getScheme();
        String url = registrationUri.toString() + "&config=1";
        List<String> pathSegments = registrationUri.getPathSegments();

        if (pathSegments.size() > 0) {
            participantId = pathSegments.get(pathSegments.size() - 1);

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
            listener.onPostExecute(null); //TODO implement error handling in listener
        } else {


                //Load join study wizard. We already have the study info on the database.
                Intent studyInfo = new Intent(application, Aware_Join_Study.class);
//                studyInfo.putExtra(Aware_Join_Study.EXTRA_STUDY_URL, registrationUri);
//                studyInfo.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                listener.startActivity(studyInfo);
//
//                listener.finish();
                //TODO implement success scenario to start call for getting metadata
            listener.onPostExecute(result); //TODO implement error handling in listener

        }
    }

    interface Listener {
        void onPreExecute();
        void onPostExecute(JSONObject result);
    }
}


