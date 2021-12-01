package com.aware.phone.ui.onboarding.data;

public class JoinedStudyMessage {
    private final String surveyUrl;
    private final String title;
    private final String description;

    public boolean showSuccessDialog() {
        return showSuccessDialog;
    }

    public void setShowSuccessDialog(boolean showSuccessDialog) {
        this.showSuccessDialog = showSuccessDialog;
    }

    private boolean showSuccessDialog = true;

    public JoinedStudyMessage(String surveyUrl, String title, String description) {
        this.surveyUrl = surveyUrl;
        this.title = title;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getSurveyUrl() {
        return surveyUrl;
    }
}
