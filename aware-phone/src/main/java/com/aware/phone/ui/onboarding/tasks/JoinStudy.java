package com.aware.phone.ui.onboarding.tasks;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.onboarding.data.StudyMetadata;
import com.aware.providers.Aware_Provider;
import com.aware.utils.Https;
import com.aware.utils.StudyUtils;
import com.aware.utils.serverping.AwareServerPing;
import com.aware.utils.studyeligibility.StudyEligibility;

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
    private String socialMediaUrl;

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
            application.getContentResolver().update(Aware_Provider.Aware_Studies.CONTENT_URI,
                    studyData, Aware_Provider.Aware_Studies.STUDY_URL + " LIKE '" + params[0].getUrl() + "'", null);
        }
        if (!study.isClosed()) study.close();

        AwareServerPing.INSTANCE.setDeviceInfo(application);
        AwareServerPing.INSTANCE.setPermissionsStatus(application, params[0].getPermissions());
        AwareServerPing.INSTANCE.setStudyInfo(true, application.getSharedPreferences(StudyEligibility.Values.PREFS_NAME,0).getBoolean(StudyEligibility.Values.PREF_ELIGIBILITY_CHECKED_KEY,false));

        StudyUtils.applySettings(application, study_configs);

        //Last step in joining study
        socialMediaUrl = params[0].getSocialMediaUrl();
        if (socialMediaUrl != null) {
            new Https().dataPOSTJson(socialMediaUrl, AwareServerPing.INSTANCE.getRegistrationData(), true);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        listener.onPostExecute(null);
        AwareServerPing.INSTANCE.setServerURL(socialMediaUrl+"update/");
    }

    public interface Listener {
        void onPostExecute(JSONObject result);
    }
}
