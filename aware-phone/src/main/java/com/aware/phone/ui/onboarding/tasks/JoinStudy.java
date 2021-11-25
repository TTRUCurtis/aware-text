package com.aware.phone.ui.onboarding.tasks;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

import com.aware.Aware;
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
    protected Void doInBackground(String... params) {
        //TODO do we really need to ask the DB for the study config if we just received it? Why not keep it in memory and pass it in here
        Cursor study = Aware.getStudy(application, params[0]);
        study.moveToFirst();
        JSONArray study_configs = null;
        try {
            int columnIndex = study.getColumnIndex(Aware_Provider.Aware_Studies.STUDY_CONFIG);
            String studyConfigJson = study.getString(columnIndex);
            study_configs = new JSONArray(studyConfigJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (study.moveToFirst()) {
            ContentValues studyData = new ContentValues();
            studyData.put(Aware_Provider.Aware_Studies.STUDY_JOINED, System.currentTimeMillis());
            studyData.put(Aware_Provider.Aware_Studies.STUDY_EXIT, 0);
            application.getContentResolver().update(Aware_Provider.Aware_Studies.CONTENT_URI, studyData, Aware_Provider.Aware_Studies.STUDY_URL + " LIKE '" + params[0] + "'", null);
        }
        if (!study.isClosed()) study.close();

        //Last step in joining study
        //TODO uncomment and figure out how this really works and how it creates/inserts all those tables on the server
        StudyUtils.applySettings(application, study_configs);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(application, "join study complete", Toast.LENGTH_LONG).show();
        listener.onPostExecute(null);
    }

    public interface Listener {
        void onPostExecute(JSONObject result);
    }
}
