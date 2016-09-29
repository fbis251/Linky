package com.fernandobarillas.linkshare.utils;

import android.content.Context;
import android.content.Intent;

import com.fernandobarillas.linkshare.R;
import com.kennyc.bottomsheet.BottomSheet;

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
        BottomSheet share = BottomSheet.createShareBottomSheet(context, sendIntent, shareTitle);
        if (share != null) {
            share.show();
            return true;
        }
        return false;
    }
}
