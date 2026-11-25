package com.hack.opensdk;

import com.hack.Slog;

import java.lang.reflect.Method;

public final class Cmd {
    private static final String TAG = "Cmd";
    private static final Cmd sInstance = new Cmd();

    private Cmd() {}

    public static Cmd INSTANCE() {
        return sInstance;
    }

    public Object exec(int cmd, Object... args) {
        if (!HackRuntime.isEngineReady()) {
            Slog.d(TAG, "Cmd.exec(%d) skipped: native opensdk engine not populated.", cmd);
            return null;
        }

        ClassLoader loader = HackRuntime.getEngineClassLoader();
        if (loader == null) {
            return null;
        }

        String[] candidates = {
                "com.hack.opensdk.CmdImpl",
                "com.hack.server.core.HackEngine"
        };
        for (String clsName : candidates) {
            try {
                Class<?> cls = Class.forName(clsName, true, loader);
                Method instanceMethod = cls.getDeclaredMethod("INSTANCE");
                Object engineObj = instanceMethod.invoke(null);
                Method execMethod = cls.getDeclaredMethod("exec", int.class, Object[].class);
                return execMethod.invoke(engineObj, cmd, args);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
