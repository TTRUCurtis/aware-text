package com.aware.phone.ui.onboarding.data;

import java.util.ArrayList;

public class LoadingIndicator {
    private final String title;
    private final String message;

    public LoadingIndicator(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }
}
