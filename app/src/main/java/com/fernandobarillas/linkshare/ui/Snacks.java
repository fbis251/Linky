package com.fernandobarillas.linkshare.ui;

import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * SnackBar wrapper/helper class
 */
public class Snacks {
    public static Snackbar showError(View view, String message, Action snackAction) {
        return showSnackbar(view, message, snackAction != null, snackAction);
    }

    public static Snackbar showMessage(View view, String message) {
        return showSnackbar(view, message, false);
    }

    public static Snackbar showSnackbar(View view, String message, boolean indefinite) {
        return showSnackbar(view, message, false, null);
    }

    public static Snackbar showSnackbar(
            View view, String message, boolean indefinite, Action snackAction) {
        if (view == null || message == null) return null;
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
        return snackbar;
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
