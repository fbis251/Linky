package com.fernandobarillas.linkshare.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class UrlParser {

    final static Pattern pattern = Pattern.compile(
            ".*?(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");

    @Nullable
    public static String extractUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        String matched = matchUrl(url);
        if (matched != null) {
            return matched;
        }

        String[] splitText = url.split("\n");
        if (splitText.length > 1) {
            // Try to find a URL in the last line of text
            String matchedText = matchUrl(splitText[splitText.length - 1]);
            if (matchedText != null) {
                return matchedText;
            }
        }

        return url;
    }

    private static String matchUrl(String url) {
        final Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
