package com.hack.opensdk;

public final class CmdConstants {
    private CmdConstants() {}

    public static final int CMD_APPLICATION_ATTACHBASE = 1;
    public static final int CMD_APPLICATION_ONCREATE = 2;
    public static final int CMD_QUICK_START_ACTIVITY = 3;

    public static final String PKG_SET_REQUEST_ASSISTANT = "request_assistant";
    public static final String PKG_SET_VIRTUAL_ROOT = "virtual_root";
    public static final String PKG_SET_GMS_ENABLED = "gms_enabled";

    public static final int INSTALL_SUCCEEDED = 1;
    public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;
    public static final int INSTALL_FAILED_INVALID_APK = -2;
    public static final int INSTALL_FAILED_INVALID_URI = -3;

    public static final int START_SUCCESS = 0;
    public static final int START_ERR_ENGINE_UNAVAILABLE = -100;
    public static final int START_ERR_INTENT_NULL = -101;
}
