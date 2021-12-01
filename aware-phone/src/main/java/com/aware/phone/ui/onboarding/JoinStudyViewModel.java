package com.aware.phone.ui.onboarding;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.PermissionUtils;
import com.aware.phone.ui.onboarding.data.LoadingIndicator;
import com.aware.phone.ui.onboarding.data.StudyMetadata;
import com.aware.phone.ui.onboarding.tasks.GetStudyMetadata;
import com.aware.phone.ui.onboarding.tasks.JoinStudy;

import org.json.JSONException;

import java.util.ArrayList;

public class JoinStudyViewModel extends AndroidViewModel {

    private String surveyUrl;
    private String studyUrl;

    private final MutableLiveData<LoadingIndicator> loadingIndicator = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<String>> requiredPermissions = new MutableLiveData<>();
    private final MutableLiveData<StudyMetadata> studyMetadata = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> joinStudySuccessMsg = new MutableLiveData<>();

    public JoinStudyViewModel(@NonNull Application application) {
        super(application);
    }

    public void loadStudy(Uri registrationData) {
        //TODO check if already fetched study url
        if (studyUrl != null) {
            Cursor cursor = Aware.getStudy(getApplication(), studyUrl);
            if (cursor == null || !cursor.moveToFirst()) {
                //TODO join study
            } else {
                //TODO display message to user, you've already joined this study
                // return to main UI? but could still need survey URL
            }
        } else {
            loadingIndicator.postValue(new LoadingIndicator("Loading study", "Please wait..."));
            String participantId = registrationData.getQueryParameter("pid");

            //TODO move this to join study task
            Aware.setSetting(getApplication(), Aware_Preferences.DEVICE_LABEL, participantId);

            new GetStudyMetadata(getApplication(), studyMetadata -> {
                loadingIndicator.postValue(null);
                this.studyMetadata.postValue(studyMetadata);
            }).execute(registrationData);
        }
    }

    public LiveData<String> getJoinedStudySuccessMsg() {
        return joinStudySuccessMsg;
    }

    public LiveData<LoadingIndicator> getLoadingIndicator() {
        return loadingIndicator;
    }

    public MutableLiveData<ArrayList<String>> getRequiredPermissions() {
        return requiredPermissions;
    }

    private void joinStudy(String studyUrl) {
        loadingIndicator.postValue(new LoadingIndicator("Joining study", "Please wait..."));
        new JoinStudy(getApplication(), result -> {
            loadingIndicator.postValue(null);
            joinStudySuccessMsg.postValue(studyMetadata.getValue().getSurveyUrl());
        }).execute(studyMetadata.getValue().getStudyUrl());
    }

    public void joinStudy() {
        joinStudy(studyUrl);
    }

    public MutableLiveData<StudyMetadata> getStudyMetadata() {
        return studyMetadata;
    }
}