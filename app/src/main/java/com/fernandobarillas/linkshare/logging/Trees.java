package com.fernandobarillas.linkshare.logging;

import android.util.Log;

import timber.log.Timber;

/**
 * Created by fb on 3/16/17.
 */

public class Trees {
    public static class DebugTree extends Timber.DebugTree {
        @Override
        protected String createStackElementTag(StackTraceElement element) {
            // Use filename and line numbers in logging output
            return "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
        }
    }

    public static class ReleaseTree extends Timber.DebugTree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) return;
            super.log(priority, tag, message, t);
        }
    }
}
