# Readable stack traces in crash reports
-keepattributes SourceFile,LineNumberTable

# slf4j (via Readability4J) looks for a binding that is not shipped
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Menu items resolve android:onClick by name at runtime
-keepclassmembers class de.bernd.shandschuh.sparserss.** {
    public void *(android.view.MenuItem);
}
