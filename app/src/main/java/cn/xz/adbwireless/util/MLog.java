package cn.xz.adbwireless.util;

import android.util.Log;

public class MLog {
    private MLog() {
    }

    public static void log(Object obj) {
        Log.e(IConst.LOG_TAG, "" + obj);
    }

    public static void logInfo(Object obj) {
        Log.i(IConst.LOG_TAG, "" + obj);
    }
}
