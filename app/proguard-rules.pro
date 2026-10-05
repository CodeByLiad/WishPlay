# Proguard rules for WishPlay

# 1. SQLCipher and SQLite
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
-keep class androidx.sqlite.db.** { *; }
-keep class androidx.sqlite.db.framework.** { *; }

# 2. Room Database & Paging
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 3. Dagger / Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponentManager
-keep class * extends dagger.hilt.internal.ComponentManager
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# 4. Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <fields>;
    <init>(...);
}

# 5. Jetpack Glance & Widgets
-keep class com.nuvetrix.wishplay.widget.** { *; }
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }

# 6. Firebase & Google Credential Manager
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# 7. OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# 8. Domain Models & DTOs
-keep class com.nuvetrix.wishplay.domain.model.** { *; }
-keep class com.nuvetrix.wishplay.data.remote.dto.** { *; }
-keep class com.nuvetrix.wishplay.data.remote.update.** { *; }
-keep class com.nuvetrix.wishplay.data.local.entity.** { *; }
-keep class com.nuvetrix.wishplay.data.local.prefs.** { *; }

# 9. Release Log Stripping (PRD line 262: release logging stripped)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
