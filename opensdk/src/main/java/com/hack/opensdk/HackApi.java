package com.hack.opensdk;

import android.app.Application;
import android.content.ComponentName;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        return HackRuntime.isEngineReady();
    }

    public static String getRuntimeDiagnosticSummary() {
        return HackRuntime.buildDiagnosticSummary(sHostContext);
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
        if (HackRuntime.isEngineReady()) {
            Object engineRet = HackRuntime.invokeEngineStatic(
                    "com.hack.opensdk.HackApiImpl",
                    "installPackageFromHost",
                    new Class<?>[]{String.class, int.class, boolean.class},
                    pkgName, userId, forceInstall
            );
            if (engineRet instanceof Integer) {
                return (Integer) engineRet;
            }
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
            if (!FileUtils.copyFile(srcBase, baseDest) || !baseDest.exists() || baseDest.length() == 0L) {
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
            prepareContainerDirectories(appDir, apkDir);
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
        if (HackRuntime.isEngineReady()) {
            Object engineRet = HackRuntime.invokeEngineStatic(
                    "com.hack.opensdk.HackApiImpl",
                    "installApkFiles",
                    new Class<?>[]{String.class, int.class, boolean.class},
                    apkPathOrDir, userId, forceInstall
            );
            if (engineRet instanceof Integer) {
                return (Integer) engineRet;
            }
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
        if (!FileUtils.copyFile(baseFile, destBase) || !destBase.exists() || destBase.length() == 0L) {
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
        prepareContainerDirectories(appDir, apkDir);
        return CmdConstants.INSTALL_SUCCEEDED;
    }

    private static void prepareContainerDirectories(File appDir, File apkDir) {
        new File(appDir, "data").mkdirs();
        new File(appDir, "cache").mkdirs();
        File libDir = new File(appDir, "lib");
        libDir.mkdirs();

        // Extract native .so libraries from staged base + split APKs into container lib/<abi>/
        File[] apks = apkDir.listFiles((dir, name) -> name.endsWith(".apk"));
        if (apks == null) return;
        byte[] buf = new byte[16 * 1024];
        for (File apk : apks) {
            try (ZipFile zip = new ZipFile(apk)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.startsWith("lib/") && name.endsWith(".so")) {
                        String rel = name.substring("lib/".length());
                        if (rel.contains("..")) continue;
                        File outSo = new File(libDir, rel);
                        File parent = outSo.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (InputStream in = zip.getInputStream(entry);
                             FileOutputStream out = new FileOutputStream(outSo)) {
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean uninstallPackage(String pkgName, int userId) {
        if (HackRuntime.isEngineReady()) {
            HackRuntime.invokeEngineStatic(
                    "com.hack.opensdk.HackApiImpl",
                    "uninstallPackage",
                    new Class<?>[]{String.class, int.class},
                    pkgName, userId
            );
        }
        File spaceDir = getSpaceRoot(userId);
        if (spaceDir == null || pkgName == null || pkgName.isEmpty()) return false;
        File appDir = new File(spaceDir, pkgName);
        if (!appDir.exists()) return false;
        FileUtils.deleteQuietly(appDir);
        return !appDir.exists();
    }

    public static Bundle getPackageSetting(String pkgName, int userId, int flags) {
        Bundle bundle = new Bundle();
        boolean needsAssist = false;
        boolean host64 = Build.SUPPORTED_64_BIT_ABIS.length > 0;

        if (host64 && pkgName != null) {
            File spaceDir = getSpaceRoot(userId);
            if (spaceDir != null) {
                File libDir = new File(new File(spaceDir, pkgName), "lib");
                boolean hasArm32 = new File(libDir, "armeabi-v7a").exists()
                        || new File(libDir, "armeabi").exists();
                boolean hasArm64 = new File(libDir, "arm64-v8a").exists();
                if (hasArm32 && !hasArm64) {
                    needsAssist = true;
                }
            }
            if (!needsAssist && sHostContext != null) {
                try {
                    ApplicationInfo info = sHostContext.getPackageManager().getApplicationInfo(pkgName, 0);
                    String nativeDir = info.nativeLibraryDir;
                    if (nativeDir != null && nativeDir.contains("arm") && !nativeDir.contains("arm64")) {
                        needsAssist = true;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        bundle.putBoolean(CmdConstants.PKG_SET_REQUEST_ASSISTANT, needsAssist);
        return bundle;
    }

    public static int startActivity(Intent intent, int userId) {
        if (intent == null) {
            return CmdConstants.START_ERR_INTENT_NULL;
        }
        String pkg = intent.getPackage();
        if (pkg == null || pkg.isEmpty()) {
            ComponentName cn = intent.getComponent();
            pkg = cn != null ? cn.getPackageName() : null;
        }
        if (pkg != null && !pkg.isEmpty()) {
            File spaceDir = getSpaceRoot(userId);
            if (spaceDir != null) {
                File stagedBase = new File(new File(new File(spaceDir, pkg), "apk"), "base.apk");
                if (!stagedBase.exists() || stagedBase.length() == 0L) {
                    Slog.e(TAG, "Cannot start %s in Space %d: staged base.apk missing at %s",
                            pkg, userId, stagedBase.getAbsolutePath());
                    return CmdConstants.START_ERR_NOT_INSTALLED;
                }
            }
        }

        if (!HackRuntime.isEngineReady()) {
            Slog.e(TAG, "Cannot launch virtualized activity for userSpace %d: %s",
                    userId, getRuntimeDiagnosticSummary());
            return CmdConstants.START_ERR_ENGINE_UNAVAILABLE;
        }

        Object engineRet = HackRuntime.invokeEngineStatic(
                "com.hack.opensdk.HackApiImpl",
                "startActivity",
                new Class<?>[]{Intent.class, int.class},
                intent, userId
        );
        if (engineRet instanceof Integer) {
            return (Integer) engineRet;
        }
        Object cmdRet = Cmd.INSTANCE().exec(CmdConstants.CMD_QUICK_START_ACTIVITY, intent, userId);
        if (cmdRet instanceof Integer) {
            return (Integer) cmdRet;
        }
        return CmdConstants.START_ERR_ENGINE_UNAVAILABLE;
    }

    public static void killApplication(String pkgName, int userId) {
        if (HackRuntime.isEngineReady()) {
            HackRuntime.invokeEngineStatic(
                    "com.hack.opensdk.HackApiImpl",
                    "killApplication",
                    new Class<?>[]{String.class, int.class},
                    pkgName, userId
            );
        }
        Slog.d(TAG, "killApplication(%s, user=%d)", pkgName, userId);
    }
}
