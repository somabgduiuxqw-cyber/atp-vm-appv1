package com.hack.opensdk;

import android.app.Application;
import android.content.Context;

public class HackApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        HackApi.attachHostContext(base);
        HackApi.dispatchAttachBaseContext(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HackApi.dispatchOnCreate(this);
    }
}
