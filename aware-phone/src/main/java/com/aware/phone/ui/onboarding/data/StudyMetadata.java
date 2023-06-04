package com.aware.phone.ui.onboarding.data;

import org.json.JSONArray;

import java.util.ArrayList;

public class StudyMetadata {
    private final String url;
    private final String name;
    private final String researcher;
    private final String description;
    private final String configuration;
    private final ArrayList<String> permissions;
    private final String surveyUrl;
    private final String socialMediaUrl;

    public boolean showPermissionsNoticeDialog() {
        return showPermissionsNoticeDialog;
    }

    public void setShowPermissionsNoticeDialog(boolean showPermissionsNoticeDialog) {
        this.showPermissionsNoticeDialog = showPermissionsNoticeDialog;
    }

    private boolean showPermissionsNoticeDialog = true; //TODO move this somewhere else, it's
    // view logic

    public String getSurveyUrl() {
        return surveyUrl;
    }

    public String getSocialMediaUrl() {
        return socialMediaUrl;
    }

    public String getConfiguration() {
        return configuration;
    }

    private StudyMetadata(String url, String name, String researcher, String description,
                          String configuration, ArrayList<String> permissions, String surveyUrl,
                          String socialMediaUrl) {
        this.url = url;
        this.name = name;
        this.researcher = researcher;
        this.description = description;
        this.configuration = configuration;
        this.permissions = permissions;
        this.surveyUrl = surveyUrl;
        this.socialMediaUrl = socialMediaUrl;
    }

    public String getResearcher() {
        return researcher;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public String getUrl() {
        return url;
    }

    public static class Builder {
        public String url;
        public String name;
        public String researcher;
        public String description;
        public String configuration;
        ArrayList<String> permissions;
        public String surveyUrl;
        public String socialMediaUrl;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setResearcher(String researcher) {
            this.researcher = researcher;
            return this;
        }

        public Builder setConfiguration(String configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setPermissions(ArrayList<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder setSurveyUrl(String surveyUrl) {
            this.surveyUrl = surveyUrl;
            return this;
        }

        public Builder setSocialMediaUrl(String url) {
            this.socialMediaUrl = url;
            return this;
        }

        public StudyMetadata build() {
            return new StudyMetadata(url, name, researcher, description, configuration, permissions,
                    surveyUrl, socialMediaUrl);
        }
    }
}
