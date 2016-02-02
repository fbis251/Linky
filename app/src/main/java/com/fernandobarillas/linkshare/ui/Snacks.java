package com.fernandobarillas.linkshare.ui;

import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * SnackBar wrapper/helper class
 */
public class Snacks {
    public static void showError(View view, String message) {
        showSnackBar(view, message, true);
    }

    public static void showMessage(View view, String message) {
        showSnackBar(view, message, false);
    }

    public static void showSnackBar(View view, String message, boolean indefinite) {
        if (view == null || message == null) return;
        int length = indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;
        Snackbar.make(view, message, length).show();
    }
}
