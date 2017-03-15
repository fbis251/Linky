# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/fb/android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes InnerClasses,EnclosingMethod

# fix warnings build errors for LinkShare
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# SearchView used in LinksListActivity
-keep class android.support.v7.widget.SearchView { *; }

-keep class java.nio.file.** { *; }
-dontwarn java.nio.file.**
-keep class java.lang.invoke.** { *; }
-dontwarn java.lang.invoke.**
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
