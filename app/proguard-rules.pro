# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes SourceFile,LineNumberTable,*Annotation*
-dontwarn com.caverock.androidsvg.**
-keep class net.gnehzr.tnoodle.** { *; }
-keep interface net.gnehzr.tnoodle.** { *; }
-keep class puzzle.** { *; }
-keep class org.timepedia.exporter.client.** { *; }

##---------------Begin: proguard configuration for Gson  ----------
-keep public class * {
    public protected *;
}

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

-keep class com.pluscubed.plustimer.model.Session { *; }
-keep class com.pluscubed.plustimer.model.Solve { *; }
-keep class com.pluscubed.plustimer.model.ScrambleAndSvg { *; }

##---------------End: proguard configuration for Gson  ----------

#Crashlytics
-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**

#retrolambda
-dontwarn java.lang.invoke.*

#Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

#okio
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

#RxAndroid/RxJava
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

#MPAndroidChart
-dontwarn io.realm.**

#Jackson
-dontwarn org.w3c.**
-dontwarn java.beans.**