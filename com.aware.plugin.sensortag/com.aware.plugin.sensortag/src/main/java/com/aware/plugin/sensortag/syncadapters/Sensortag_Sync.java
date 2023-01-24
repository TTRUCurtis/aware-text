package com.aware.plugin.sensortag.syncadapters;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.aware.plugin.sensortag.Provider;
import com.aware.syncadapters.AwareSyncAdapter;

/**
 * Created by denzilferreira on 17/08/2017.
 */

public class Sensortag_Sync extends Service {
    private AwareSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new AwareSyncAdapter(getApplicationContext(), true, true);
                sSyncAdapter.init(
                        Provider.DATABASE_TABLES, Provider.TABLES_FIELDS,
                        new Uri[]{
                                Provider.SensorTag_Data.CONTENT_URI,
                                Provider.SensorTag_Devices.CONTENT_URI
                        }
                );
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
