package com.aware.phone.ui.onboarding;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.aware.phone.ui.onboarding.data.LoadingIndicator;
import com.aware.phone.ui.onboarding.data.StudyMetadata;
import com.aware.phone.ui.onboarding.tasks.GetStudyMetadata;
import com.aware.phone.ui.onboarding.tasks.JoinStudy;

import java.util.ArrayList;

public class JoinStudyViewModel extends AndroidViewModel {

    private final MutableLiveData<LoadingIndicator> loadingIndicator = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<String>> requiredPermissions = new MutableLiveData<>();
    private final MutableLiveData<StudyMetadata> studyMetadataLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMsgLiveData = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<String>> deniedPermissions = new MutableLiveData<>();

    public JoinStudyViewModel(@NonNull Application application) {
        super(application);
    }

    public void loadStudy(Uri registrationData) {
        if (studyMetadataLiveData.getValue() == null) {
            loadingIndicator.setValue(new LoadingIndicator("Loading study", "Please wait..."));

            new GetStudyMetadata(getApplication(), new GetStudyMetadata.Listener() {
                @Override
                public void onSuccess(StudyMetadata studyMetadata) {
                    loadingIndicator.setValue(null);
                    studyMetadataLiveData.setValue(studyMetadata);
                }

                @Override
                public void onError(String errorMsg) {
                    loadingIndicator.setValue(null);
                    errorMsgLiveData.setValue(errorMsg);
                }
            }).execute(registrationData);
        }
    }

    public LiveData<LoadingIndicator> getLoadingIndicator() {
        return loadingIndicator;
    }

    public MutableLiveData<ArrayList<String>> getRequiredPermissions() {
        return requiredPermissions;
    }

    public void joinStudy() {
        loadingIndicator.setValue(new LoadingIndicator("Joining study", "Please wait..."));
        new JoinStudy(getApplication(), result -> loadingIndicator.setValue(null)).execute(studyMetadataLiveData.getValue());
    }

    public MutableLiveData<StudyMetadata> getStudyMetadataLiveData() {
        return studyMetadataLiveData;
    }

    public MutableLiveData<String> getErrorMsgLiveData() {
        return errorMsgLiveData;
    }

    public void dismissErrorDialog() {
        errorMsgLiveData.setValue(null);
    }

    public MutableLiveData<ArrayList<String>> getDeniedPermissions() {
        return deniedPermissions;
    }

    public void updateDeniedPermissions(ArrayList<String> deniedPermissions) {
        this.deniedPermissions.setValue(deniedPermissions);
    }

}