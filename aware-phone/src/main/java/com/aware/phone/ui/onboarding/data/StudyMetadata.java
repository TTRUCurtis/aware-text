package com.aware.phone.ui.onboarding.data;

import java.util.ArrayList;

public class StudyMetadata {
    private final String studyUrl;
    private final String title;
    private final String description;
    private final String researcher;
    private final ArrayList<String> permissions;

    public boolean showPermissionsNoticeDialog() {
        return showPermissionsNoticeDialog;
    }

    public void setShowPermissionsNoticeDialog(boolean showPermissionsNoticeDialog) {
        this.showPermissionsNoticeDialog = showPermissionsNoticeDialog;
    }

    private boolean showPermissionsNoticeDialog = true;

    public String getSurveyUrl() {
        return surveyUrl;
    }

    private final String surveyUrl;

    public StudyMetadata(String studyUrl, String title, String description, String researcher, ArrayList<String> permissions, String surveyUrl) {
        this.studyUrl = studyUrl;
        this.title = title;
        this.description = description;
        this.researcher = researcher;
        this.permissions = permissions;
        this.surveyUrl = surveyUrl;
    }

    public String getResearcher() {
        return researcher;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public String getStudyUrl() {
        return studyUrl;
    }
}
