package com.fernandobarillas.linkshare.logging;

import android.util.Log;

import timber.log.Timber;

/**
 * Created by fb on 3/16/17.
 */

public class Trees {
    private static boolean isLowerThanWarning(int priority) {
        return (priority < Log.WARN);
    }

    public static class DebugTree extends Timber.DebugTree {
        private boolean mIsLogErrorsOnly;
        private boolean mIsUseLogcatLineNumbers;

        public DebugTree(boolean isLogErrorsOnly, boolean isUseLogcatLineNumbers) {
            mIsLogErrorsOnly = isLogErrorsOnly;
            mIsUseLogcatLineNumbers = isUseLogcatLineNumbers;
        }

        @Override
        protected String createStackElementTag(StackTraceElement element) {
            if (mIsUseLogcatLineNumbers) {
                // Use filename and line numbers in logging output
                return "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
            } else {
                return super.createStackElementTag(element);
            }
        }

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (mIsLogErrorsOnly && isLowerThanWarning(priority)) return;
            super.log(priority, tag, message, t);
        }
    }

    public static class ReleaseTree extends Timber.DebugTree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (isLowerThanWarning(priority)) return;
            super.log(priority, tag, message, t);
        }
    }
}
