package com.hack;

import android.util.Log;

public final class Slog {
    private Slog() {}

    public static void d(String tag, String format, Object... args) {
        String message = (args != null && args.length > 0) ? String.format(format, args) : format;
        Log.d(tag, message);
    }

    public static void e(String tag, String format, Object... args) {
        String message = (args != null && args.length > 0) ? String.format(format, args) : format;
        Log.e(tag, message);
    }
}
