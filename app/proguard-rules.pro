# --- Максимальная обфускация и оптимизация ---

# 1. Агрессивная оптимизация (5 проходов вместо одного)
-optimizationpasses 5

# 2. Разрешить изменение модификаторов доступа (дает больше свободы для инлайнинга и оптимизации)
-allowaccessmodification

# 3. Агрессивное слияние интерфейсов
-mergeinterfacesaggressively

# 4. Перемещение всех классов в пустой корневой пакет (скрывает структуру папок вашего проекта)
-repackageclasses ''

# 5. Использовать одно и то же имя для методов с разными сигнатурами (максимально путает декомпиляторы)
-overloadaggressively

# 6. Удаление всех атрибутов, которые не критичны для работы, но помогают при декомпиляции
# ВНИМАНИЕ: Удаление SourceFile и LineNumberTable сделает невозможным чтение StackTrace без файла mapping.txt
-keepattributes !SourceFile,!LineNumberTable,*Annotation*,Signature,EnclosingMethod

# 7. Отключаем предупреждения (чтобы сборка не падала из-за мелких нестыковок в библиотеках)
-dontwarn **

# --- Остальные правила ---

# WorkManager
-keep public class * extends androidx.work.CoroutineWorker

# OkHttp + Retrofit
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.icymath.analytics.InstallInfo { *; }

# Room (современные версии Room сами поставляют нужные правила)
# -keep class androidx.room.** { *; }
# -dontwarn androidx.room.**
