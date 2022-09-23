package com.aware.phone.ui.onboarding.data;

import androidx.annotation.Nullable;

public class Result<T> {
    private final T data;
    private final String error;

    private Result(T data, String error) {
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> data(T data){
        return new Result<T>(data, null);
    }

    public static <T> Result<T> error(String error){
        return new Result<>(null, error);
    }

    public @Nullable T getData() {
        return data;
    }

    public @Nullable String getErrorMsg() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }
}
