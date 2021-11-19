package com.aware.phone.ui.registration;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.aware.phone.R;

public class RegistrationActivity extends AppCompatActivity {

    private RegistrationViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_app_link);

        //uri = https://survey.wwbp.org/test/download-app?pid=123

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        new AlertDialog.Builder(this)
                .setTitle("URL")
                .setMessage("The following data was retrieved: " + data + " pid: " + data.getQueryParameter("pid"))

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();

        viewModel = new ViewModelProvider(this).get(RegistrationViewModel.class);
        viewModel.registerForStudy(data);


    }
}