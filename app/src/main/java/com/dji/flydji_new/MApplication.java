package com.dji.flydji_new;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private FlyDJI flydji;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (flydji == null) {
            flydji = new FlyDJI();
            flydji.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        flydji.onCreate();
    }
}
