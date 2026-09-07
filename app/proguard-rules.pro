# Preserve useful stack traces; R8 mapping must accompany the exact release artifact.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,EnclosingMethod,InnerClasses

# WorkManager constructs workers by class name.
-keep public class * extends androidx.work.CoroutineWorker

# Gson reflection: persisted field names and constructors are a storage contract.
-keep class com.cryonum.items.HistoryItem { *; }
-keep class com.cryonum.items.HistoryItem$HistoryType { *; }
-keep class com.cryonum.content.PartialContentMetadata { *; }
-keep class com.cryonum.content.CompletedContentRecord { *; }
-keep class com.cryonum.managers.AnalyticsManager$DeviceInfo { *; }
-keep class com.cryonum.managers.AnalyticsManager$UserEvent { *; }
-keep class com.cryonum.managers.AnalyticsManager$CrashReport { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
