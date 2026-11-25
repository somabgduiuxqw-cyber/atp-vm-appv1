package com.hack.opensdk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.hack.Slog;
import com.hack.utils.FileUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class HackApi {
    private static final String TAG = "HackApi";
    private static Context sHostContext;
    private static final List<ApplicationCallback> sCallbacks = new CopyOnWriteArrayList<>();

    private HackApi() {}

    public interface ApplicationCallback {
        void onInitAppContext(Object loadedApk, Context appContext);
        void onAttachBaseContext(Application app);
        void onInstallProviders(Application app);
        void onCreate(Application app);
    }

    static void attachHostContext(Context context) {
        sHostContext = context != null ? context.getApplicationContext() : null;
        if (sHostContext == null) {
            sHostContext = context;
        }
    }

    static void dispatchAttachBaseContext(Application app) {
        for (ApplicationCallback callback : sCallbacks) {
            callback.onAttachBaseContext(app);
        }
    }

    static void dispatchOnCreate(Application app) {
        for (ApplicationCallback callback : sCallbacks) {
            callback.onCreate(app);
        }
    }

    public static void registerApplicationCallback(ApplicationCallback callback) {
        if (callback != null && !sCallbacks.contains(callback)) {
            sCallbacks.add(callback);
        }
    }

    public static boolean isNativeRuntimeAvailable() {
        if (sHostContext == null) return false;
        String nativeDir = sHostContext.getApplicationInfo().nativeLibraryDir;
        if (nativeDir != null) {
            File libHack = new File(nativeDir, "libhack.so");
            if (libHack.exists()) return true;
        }
        return false;
    }

    public static File getSpaceRoot(int userId) {
        Context ctx = sHostContext;
        if (ctx == null) return null;
        File dir = new File(new File(ctx.getFilesDir(), "atp_spaces"), "user_" + Math.max(0, userId));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static int installPackageFromHost(String pkgName, int userId, boolean forceInstall) {
        Context ctx = sHostContext;
        if (ctx == null || pkgName == null || pkgName.isEmpty()) {
            return CmdConstants.INSTALL_FAILED_INVALID_URI;
        }
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
            File spaceDir = getSpaceRoot(userId);
            if (spaceDir == null) return CmdConstants.INSTALL_FAILED_INVALID_URI;

            File appDir = new File(spaceDir, pkgName);
            File apkDir = new File(appDir, "apk");
            File baseDest = new File(apkDir, "base.apk");
            if (!forceInstall && baseDest.exists()) {
                return CmdConstants.INSTALL_FAILED_ALREADY_EXISTS;
            }
            if (appInfo.sourceDir == null) {
                return CmdConstants.INSTALL_FAILED_INVALID_APK;
            }
            File srcBase = new File(appInfo.sourceDir);
            if (!FileUtils.copyFile(srcBase, baseDest)) {
                return CmdConstants.INSTALL_FAILED_INVALID_APK;
            }
            if (appInfo.splitSourceDirs != null) {
                for (int i = 0; i < appInfo.splitSourceDirs.length; i++) {
                    File splitSrc = new File(appInfo.splitSourceDirs[i]);
                    if (splitSrc.exists() && splitSrc.canRead()) {
                        String splitName = splitSrc.getName().endsWith(".apk")
                                ? splitSrc.getName()
                                : "split_" + i + ".apk";
                        FileUtils.copyFile(splitSrc, new File(apkDir, splitName));
                    }
                }
            }
            new File(appDir, "data").mkdirs();
            new File(appDir, "cache").mkdirs();
            Slog.d(TAG, "Staged host package %s into user space %d", pkgName, userId);
            return CmdConstants.INSTALL_SUCCEEDED;
        } catch (Throwable t) {
            Slog.e(TAG, "installPackageFromHost failed for %s: %s", pkgName, t.getMessage());
            return CmdConstants.INSTALL_FAILED_INVALID_APK;
        }
    }

    public static int installApkFiles(String apkPathOrDir, int userId, boolean forceInstall) {
        Context ctx = sHostContext;
        if (ctx == null || apkPathOrDir == null || apkPathOrDir.isEmpty()) {
            return CmdConstants.INSTALL_FAILED_INVALID_URI;
        }
        File src = new File(apkPathOrDir);
        if (!src.exists()) {
            return CmdConstants.INSTALL_FAILED_INVALID_URI;
        }
        File baseFile = src.isDirectory() ? new File(src, "base.apk") : src;
        if (!baseFile.exists()) {
            return CmdConstants.INSTALL_FAILED_INVALID_APK;
        }
        PackageInfo info = ctx.getPackageManager().getPackageArchiveInfo(baseFile.getAbsolutePath(), 0);
        if (info == null || info.packageName == null) {
            return CmdConstants.INSTALL_FAILED_INVALID_APK;
        }
        File spaceDir = getSpaceRoot(userId);
        if (spaceDir == null) return CmdConstants.INSTALL_FAILED_INVALID_URI;

        File appDir = new File(spaceDir, info.packageName);
        File apkDir = new File(appDir, "apk");
        File destBase = new File(apkDir, "base.apk");
        if (!forceInstall && destBase.exists()) {
            return CmdConstants.INSTALL_FAILED_ALREADY_EXISTS;
        }
        if (!FileUtils.copyFile(baseFile, destBase)) {
            return CmdConstants.INSTALL_FAILED_INVALID_APK;
        }
        if (src.isDirectory()) {
            File[] files = src.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".apk") && !f.getName().equals("base.apk")) {
                        FileUtils.copyFile(f, new File(apkDir, f.getName()));
                    }
                }
            }
        }
        new File(appDir, "data").mkdirs();
        new File(appDir, "cache").mkdirs();
        return CmdConstants.INSTALL_SUCCEEDED;
    }

    public static boolean uninstallPackage(String pkgName, int userId) {
        File spaceDir = getSpaceRoot(userId);
        if (spaceDir == null || pkgName == null || pkgName.isEmpty()) return false;
        File appDir = new File(spaceDir, pkgName);
        if (!appDir.exists()) return false;
        FileUtils.deleteQuietly(appDir);
        return !appDir.exists();
    }

    public static Bundle getPackageSetting(String pkgName, int userId, int flags) {
        Bundle bundle = new Bundle();
        Context ctx = sHostContext;
        boolean needsAssist = false;
        if (ctx != null && pkgName != null) {
            try {
                ApplicationInfo info = ctx.getPackageManager().getApplicationInfo(pkgName, 0);
                String nativeDir = info.nativeLibraryDir;
                if (nativeDir != null && nativeDir.contains("arm") && !nativeDir.contains("arm64")) {
                    boolean host64 = Build.SUPPORTED_64_BIT_ABIS.length > 0;
                    needsAssist = host64;
                }
            } catch (Exception ignored) {
            }
        }
        bundle.putBoolean(CmdConstants.PKG_SET_REQUEST_ASSISTANT, needsAssist);
        return bundle;
    }

    public static int startActivity(Intent intent, int userId) {
        if (intent == null) {
            return CmdConstants.START_ERR_INTENT_NULL;
        }
        if (!isNativeRuntimeAvailable()) {
            Slog.e(TAG, "Cannot launch virtualized activity for userSpace %d: native libhack.so runtime not present.", userId);
            return CmdConstants.START_ERR_ENGINE_UNAVAILABLE;
        }
        return CmdConstants.START_SUCCESS;
    }

    public static void killApplication(String pkgName, int userId) {
        Slog.d(TAG, "killApplication(%s, user=%d)", pkgName, userId);
    }
}
