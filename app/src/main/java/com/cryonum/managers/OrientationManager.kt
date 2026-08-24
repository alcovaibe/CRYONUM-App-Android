package com.cryonum.managers

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.MainThread
import java.lang.ref.WeakReference

class OrientationManager(activity: Activity) {

    private val activityRef: WeakReference<Activity> = WeakReference(activity)
    private val orientationListener: OrientationEventListener
    @Volatile
    private var handling = false
    private val fadeDurationMs = 180L

    init {
        orientationListener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(degrees: Int) {
                val act = activityRef.get() ?: return
                
                // игнорируем неизвестную ориентацию (плоско)
                if (degrees == ORIENTATION_UNKNOWN) return

                // определяем в каком "приблизительном" положении устройство:
                val deviceIsLandscape = isDeviceLandscapeDegrees(degrees)
                // Текущее состояние UI
                val uiOrientation = act.resources.configuration.orientation

                // Если устройство горизонтально, а UI не зафиксирован в портрете — применяем коррекцию
                if (deviceIsLandscape) {
                    // если уже зафиксировано портретом — ничего не делаем
                    val requested = act.requestedOrientation
                    if (requested == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                        requested == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT ||
                        uiOrientation == Configuration.ORIENTATION_PORTRAIT
                    ) {
                        // UI уже портрет — всё ок
                        return
                    }
                    // иначе плавно переводим в портрет и фиксируем
                    enforcePortraitSmooth()
                } else {
                    // устройство вертикально — можно при желании снять флаг handling
                    handling = false
                    // не снимаем фиксированный портрет — оставляем портретную ориентацию по требованию задачи
                }
            }
        }
    }

    fun start() {
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        } else {
            orientationListener.disable()
        }
    }

    fun stop() {
        orientationListener.disable()
    }

    fun isAutoRotateEnabled(): Boolean {
        val act = activityRef.get() ?: return false
        return try {
            val value = Settings.System.getInt(act.contentResolver, Settings.System.ACCELEROMETER_ROTATION)
            value == 1
        } catch (e: Settings.SettingNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    @MainThread
    fun enforcePortraitSmooth() {
        val act = activityRef.get() ?: return

        // защита от повторного выполнения
        if (handling) return
        handling = true

        // Если уже установлен запрос на портрет — ничего не делаем
        val requested = act.requestedOrientation
        if (requested == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
            requested == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        ) {
            handling = false
            return
        }

        // Root view (вкладка с контентом)
        val root = act.findViewById<View>(android.R.id.content)
        if (root == null) {
            // если корневой вид не найден — просто зафиксируем без анимации
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            handling = false
            return
        }

        // Плавная анимация: затемнение -> изменение ориентации -> появление.
        root.animate()
            .alpha(0f)
            .setDuration(fadeDurationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Установим портретную ориентацию
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                root.alpha = 0f
                root.animate()
                    .alpha(1f)
                    .setDuration(fadeDurationMs)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        handling = false
                    }.start()
            }.start()
    }

    private fun isDeviceLandscapeDegrees(degrees: Int): Boolean {
        val tol = 30
        val normalized = normalizeDegrees(degrees)
        val d90 = Math.abs(normalized - 90)
        val d270 = Math.abs(normalized - 270)
        return d90 <= tol || d270 <= tol
    }

    private fun normalizeDegrees(degrees: Int): Int {
        var d = degrees % 360
        if (d < 0) d += 360
        return d
    }
}
