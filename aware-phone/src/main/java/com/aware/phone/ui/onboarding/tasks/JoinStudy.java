package com.aware.phone.ui.onboarding.tasks;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.phone.ui.onboarding.data.StudyMetadata;
import com.aware.providers.Aware_Provider;
import com.aware.utils.Https;
import com.aware.utils.StudyUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

/**
 * Join study asynchronously
 */
public class JoinStudy extends AsyncTask<StudyMetadata, Void, Void> {

    private final Application application;
    private final Listener listener;

    public JoinStudy(Application application, Listener listener) {
        this.application = application;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(StudyMetadata... params) {
        //TODO do we really need to ask the DB for the study config if we just received it? Why not keep it in memory and pass it in here
        Cursor study = Aware.getStudy(application, params[0].getUrl());
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

        StudyUtils.applySettings(application, study_configs);

        //Last step in joining study
        String socialMediaUrl = params[0].getSocialMediaUrl();
        if (socialMediaUrl != null) {
            String response = new Https().dataGET(socialMediaUrl, true);
            Log.i("JoinStudy", response);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        listener.onPostExecute(null);
    }

    public interface Listener {
        void onPostExecute(JSONObject result);
    }
}
