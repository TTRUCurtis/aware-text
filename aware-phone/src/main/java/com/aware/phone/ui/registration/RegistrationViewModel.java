package com.aware.phone.ui.registration;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> loadingIndicator;

    public RegistrationViewModel(@NonNull Application application) {
        super(application);
        loadingIndicator = new MutableLiveData<>(true);
    }

    public LiveData<Boolean> getLoadingIndicator() {
        if (loadingIndicator == null) {
            loadStudyUrl();
        }
        return loadingIndicator;
    }

    private void loadStudyUrl() {
    }

    public void registerForStudy(Uri registrationData) {
        Aware.setSetting(getApplication(), Aware_Preferences.DEVICE_LABEL, registrationData.getQueryParameter("pid"));

        new FetchStudyUrl(getApplication(), new FetchStudyUrl.Listener() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onPostExecute(JSONObject studyUrl) {
                //Success
                fetchStudyData(studyUrl);
            }
        }).execute(registrationData);
    }

    private void fetchStudyData(JSONObject result) {
        new FetchStudyData(getApplication(), new FetchStudyData.Listener() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onPostExecute() {
                //TODO continue with permissions if haven't already
                //TODO Start up join study async task
                String studyUrl = "";
                try {
                    studyUrl = result.getString("study_url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jointStudy(studyUrl);
            }
        }).execute(result);
    }

    private void jointStudy(String studyUrl) {
        new JoinStudy(getApplication(), new JoinStudy.Listener() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onPostExecute(JSONObject result) {

            }
        }).execute(studyUrl);
    }
}