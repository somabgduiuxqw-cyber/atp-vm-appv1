package com.hack.opensdk;

import android.app.Application;
import android.content.Context;

public class HackApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        HackApi.attachHostContext(base);
        HackRuntime.install(this, BuildConfig.ENGINE_JAR_NAME, true);
        Cmd.INSTANCE().exec(CmdConstants.CMD_APPLICATION_ATTACHBASE, this, base);
        HackApi.dispatchAttachBaseContext(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Cmd.INSTANCE().exec(CmdConstants.CMD_APPLICATION_ONCREATE);
        HackApi.dispatchOnCreate(this);
    }
}
