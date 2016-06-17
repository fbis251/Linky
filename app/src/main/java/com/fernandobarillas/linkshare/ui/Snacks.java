package com.fernandobarillas.linkshare.ui;

import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * SnackBar wrapper/helper class
 */
public class Snacks {
    public static void showError(View view, String message) {
        showSnackBar(view, message, true);
    }

    public static void showError(View view, String message, Action snackAction) {
        showSnackBar(view, message, true, snackAction);
    }

    public static void showMessage(View view, String message) {
        showSnackBar(view, message, false);
    }

    public static void showSnackBar(View view, String message, boolean indefinite) {
        showSnackBar(view, message, false, null);
    }

    public static void showSnackBar(View view, String message, boolean indefinite,
            Action snackAction) {
        if (view == null || message == null) return;
        int length = indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;
        Snackbar snackbar = Snackbar.make(view, message, length);
        if (snackAction != null) {
            if (snackAction.mResId != Action.INVALID_ID) {
                snackbar.setAction(snackAction.mResId, snackAction.mOnClickListener);
            } else {
                snackbar.setAction(snackAction.mActionMessage, snackAction.mOnClickListener);
            }
        }
        snackbar.show();
    }

    public static class Action {
        static final int INVALID_ID = -1;
        @StringRes
        int mResId = INVALID_ID;
        String               mActionMessage;
        View.OnClickListener mOnClickListener;

        public Action(int resId, View.OnClickListener onClickListener) {
            mResId = resId;
            mOnClickListener = onClickListener;
        }

        public Action(String actionMessage, View.OnClickListener onClickListener) {
            mActionMessage = actionMessage;
            mOnClickListener = onClickListener;
        }
    }
}
