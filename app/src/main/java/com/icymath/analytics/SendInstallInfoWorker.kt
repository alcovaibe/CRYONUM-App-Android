package com.icymath.analytics

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SendInstallInfoWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun doWork(): Result {
        val context = applicationContext

        return try {
            // Загружаем ожидающую отправки информацию (статический метод менеджера)
            val info = InstallationInformationManager.loadPendingInfo(context)
            if (info == null) {
                Log.w(TAG, "No pending InstallInfo found")
                return Result.success()
            }

            // Формируем JSON (элементарное экранирование кавычек)
            val json = """
                {
                "source":"${escape(info.source)}",
                "version":"${escape(info.version)}",
                "androidId":"${escape(info.androidId)}"
                }
            """.trimIndent()

            val body = json.toRequestBody(JSON)

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "InstallInfo sent successfully")

                    // Статические методы менеджера для сохранения/очистки
                    InstallationInformationManager.saveSentAndroidId(context, info.androidId)
                    InstallationInformationManager.clearPendingInfo(context)

                    Result.success()
                } else {
                    Log.w(TAG, "Failed to send InstallInfo: ${response.code}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending InstallInfo", e)
            Result.retry()
        }
    }

    private fun escape(s: String?): String {
        return s?.replace("\"", "\\\"") ?: ""
    }

    companion object {
        private const val TAG = "SendInstallInfoWorker"
        private const val SERVER_URL = "http://192.168.1.10:5000/install_info"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
