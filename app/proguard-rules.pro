# Keep rules for Shappky Async release (R8 / shrink resources)

# Shizuku — reflective newProcess + binder APIs
-keep class rikka.shizuku.** { *; }
-keepclassmembers class rikka.shizuku.Shizuku {
    public static *** newProcess(...);
}

# libsu
-keep class com.topjohnwu.superuser.** { *; }

# AIDL / Parcelable models used across process boundaries
-keep class com.yassernull.shappky.data.models.** { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# RemoteViews / widgets
-keepclassmembers class * {
    public void set*(...);
}

-dontwarn rikka.shizuku.**
-dontwarn com.topjohnwu.superuser.**
