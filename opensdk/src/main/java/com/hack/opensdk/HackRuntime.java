package com.hack.opensdk;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.hack.Slog;
import com.hack.utils.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public final class HackRuntime {
    private static final String TAG = "HackRuntime";
    private static final String[] NATIVE_LIB_CANDIDATES = {"hack", "moon"};
    private static final String[] ENGINE_ENTRY_CLASSES = {
            "com.hack.server.core.HackEngine",
            "com.hack.opensdk.HackApiImpl",
            "com.hack.opensdk.CmdImpl"
    };

    private static volatile boolean sInstalled = false;
    private static volatile boolean sNativeLoaded = false;
    private static volatile boolean sMoonJarLoaded = false;
    private static volatile String sLoadedNativeName = null;
    private static volatile String sLastError = "Not initialized yet.";
    private static volatile ClassLoader sEngineClassLoader = null;

    private HackRuntime() {}

    public static synchronized void install(Application app, String engineJarName, boolean enableHook) {
        if (sInstalled) {
            return;
        }
        sInstalled = true;
        if (app == null) {
            sLastError = "Application context was null during HackRuntime.install.";
            return;
        }

        Context base = app.getBaseContext() != null ? app.getBaseContext() : app;
        StringBuilder errors = new StringBuilder();

        // 1. Attempt to load native WaxMoon virtualization library (libhack.so / libmoon.so)
        String nativeDir = base.getApplicationInfo().nativeLibraryDir;
        for (String libName : NATIVE_LIB_CANDIDATES) {
            File candidateSo = nativeDir != null ? new File(nativeDir, "lib" + libName + ".so") : null;
            try {
                System.loadLibrary(libName);
                sNativeLoaded = true;
                sLoadedNativeName = "lib" + libName + ".so";
                Slog.d(TAG, "Loaded native runtime library: %s (dir=%s)", sLoadedNativeName, nativeDir);
                break;
            } catch (UnsatisfiedLinkError e) {
                String pathState = (candidateSo != null && candidateSo.exists()) ? "present-failed" : "missing";
                errors.append("lib").append(libName).append(".so [").append(pathState).append("]: ")
                        .append(e.getMessage()).append("; ");
            } catch (Throwable t) {
                errors.append("lib").append(libName).append(".so: ").append(t.getMessage()).append("; ");
            }
        }

        // 2. Attempt to load WaxMoon engine DEX/JAR (moon.jar in assets or classpath)
        String jarFileName = (engineJarName == null || engineJarName.isEmpty()) ? "moon.jar" : engineJarName;
        sEngineClassLoader = loadEngineClasses(base, jarFileName, errors);
        sMoonJarLoaded = (sEngineClassLoader != null);

        if (sNativeLoaded && sMoonJarLoaded) {
            sLastError = null;
            Slog.d(TAG, "WaxMoon opensdk runtime initialized successfully (hook=%s).", enableHook);
        } else {
            sLastError = errors.toString().trim();
            Slog.e(TAG, "WaxMoon opensdk runtime incomplete: %s", sLastError);
        }
    }

    private static ClassLoader loadEngineClasses(Context context, String jarFileName, StringBuilder errors) {
        ClassLoader hostLoader = HackRuntime.class.getClassLoader();
        if (hasEngineClass(hostLoader)) {
            return hostLoader;
        }

        try (InputStream ignored = context.getAssets().open(jarFileName)) {
            File engineDir = new File(context.getCodeCacheDir(), "atp_engine");
            if (!engineDir.exists()) {
                engineDir.mkdirs();
            }
            File extractedJar = new File(engineDir, jarFileName);
            if (!FileUtils.extractAsset(context, jarFileName, extractedJar) || !extractedJar.exists()) {
                errors.append(jarFileName).append(" asset extraction failed; ");
                return null;
            }
            File dexOutDir = new File(engineDir, "oat");
            if (!dexOutDir.exists()) {
                dexOutDir.mkdirs();
            }
            DexClassLoader dexLoader = new DexClassLoader(
                    extractedJar.getAbsolutePath(),
                    dexOutDir.getAbsolutePath(),
                    context.getApplicationInfo().nativeLibraryDir,
                    hostLoader
            );
            if (hasEngineClass(dexLoader)) {
                return dexLoader;
            }
            errors.append(jarFileName).append(" loaded but engine entry class not found; ");
            return null;
        } catch (Exception e) {
            errors.append("assets/").append(jarFileName).append(" missing (")
                    .append(e.getClass().getSimpleName()).append("); ");
            return null;
        }
    }

    private static boolean hasEngineClass(ClassLoader loader) {
        if (loader == null) return false;
        for (String clsName : ENGINE_ENTRY_CLASSES) {
            try {
                Class.forName(clsName, false, loader);
                return true;
            } catch (ClassNotFoundException ignored) {
            }
        }
        return false;
    }

    public static boolean isNativeLoaded() {
        return sNativeLoaded;
    }

    public static boolean isMoonJarLoaded() {
        return sMoonJarLoaded;
    }

    public static boolean isEngineReady() {
        return sNativeLoaded && sMoonJarLoaded;
    }

    public static ClassLoader getEngineClassLoader() {
        return sEngineClassLoader;
    }

    public static String getLoadedNativeName() {
        return sLoadedNativeName;
    }

    public static String getLastError() {
        return sLastError;
    }

    public static String buildDiagnosticSummary(Context context) {
        String abis = String.join(", ", Build.SUPPORTED_ABIS);
        String nativeDir = context != null ? context.getApplicationInfo().nativeLibraryDir : "unknown";
        return "Native runtime ready: " + isEngineReady()
                + " | libhack.so/libmoon.so loaded: " + sNativeLoaded
                + " | moon.jar loaded: " + sMoonJarLoaded
                + " | Host ABIs: [" + abis + "]"
                + " | nativeLibraryDir: " + nativeDir
                + (sLastError != null && !sLastError.isEmpty() ? " | Detail: " + sLastError : "");
    }

    static Object invokeEngineStatic(String className, String methodName, Class<?>[] paramTypes, Object... args) {
        ClassLoader loader = sEngineClassLoader;
        if (loader == null) return null;
        try {
            Class<?> cls = Class.forName(className, true, loader);
            Method method = cls.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (Throwable t) {
            Slog.e(TAG, "invokeEngineStatic %s.%s failed: %s", className, methodName, t.getMessage());
            return null;
        }
    }
}
