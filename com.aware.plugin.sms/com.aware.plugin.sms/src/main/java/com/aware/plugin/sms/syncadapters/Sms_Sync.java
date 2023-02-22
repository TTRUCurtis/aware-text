package com.aware.plugin.sms.syncadapters;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.aware.plugin.sms.Provider;
import com.aware.syncadapters.AwareSyncAdapter;

/**
 * Created by D. Bellew on 2021-03-30.
 */

public class Sms_Sync extends Service {

    private AwareSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SMSAwareSyncAdapter(getApplicationContext(), true, true);
                sSyncAdapter.init(
                        Provider.DATABASE_TABLES,
                        Provider.TABLES_FIELDS,
                        new Uri[]{
                                Provider.Sms_Data.CONTENT_URI,
                                Provider.Sentiment_Analysis.CONTENT_URI
                        }
                );
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
