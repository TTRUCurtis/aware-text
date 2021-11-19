package com.aware.phone.ui.registration;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;

import com.aware.Aware;
import com.aware.phone.Aware_Client;
import com.aware.phone.ui.Aware_Join_Study;
import com.aware.providers.Aware_Provider;
import com.aware.utils.StudyUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Join study asynchronously
 */
public class JoinStudy extends AsyncTask<String, Void, Void> {

    private final Application application;
    private final Listener listener;

    public JoinStudy(Application application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
//
//        mLoading = new ProgressDialog(Aware_Join_Study.this);
//        mLoading.setMessage("Joining study, please wait.");
//        mLoading.setCancelable(false);
//        mLoading.setInverseBackgroundForced(false);
//        mLoading.show();
//        mLoading.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialogInterface) {
//                finish();
//                //Redirect the user to the main UI
//                Intent mainUI = new Intent(getApplicationContext(), Aware_Client.class);
//                mainUI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(mainUI);
//            }
//        });
        listener.onPreExecute();
    }

    @Override
    protected Void doInBackground(String... params) {
        Cursor qry = Aware.getStudy(application, params[0]);
        qry.moveToFirst();
        JSONArray study_configs = null;
        try {
            int columnIndex = qry.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_CONFIG);
            String studyConfigJson = qry.getString(columnIndex);
            study_configs = new JSONArray(studyConfigJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StudyUtils.applySettings(application, study_configs);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        listener.onPostExecute(null); //TODO fix
    }

    interface Listener {
        void onPreExecute();
        void onPostExecute(JSONObject result);
    }
}
