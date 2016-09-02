package com.fernandobarillas.linkshare.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.fernandobarillas.linkshare.R;

/**
 * Created by fb on 9/2/16.
 */

public class ShareHandler {
    public static boolean share(String title, String url, Context context) {
        if (context == null) return false;

        Intent sendIntent = new Intent();
        sendIntent.setType("text/plain");
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);

        String shareTitle = context.getString(R.string.share);
        PackageManager packageManager = context.getPackageManager();
        if (sendIntent.resolveActivity(packageManager) != null) {
            context.startActivity(Intent.createChooser(sendIntent, shareTitle));
            return true;
        }
        return false;
    }
}
