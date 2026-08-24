package com.cryonum.content

import android.content.Context
import com.google.gson.Gson
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ContentDependencies private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()

    val storage = ContentStorage(appContext, gson)
    val metadataStore = ContentMetadataStore(appContext, gson)
    val policyUpdateStore = PolicyUpdateStore(appContext)

    val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .followRedirects(false)
        .followSslRedirects(false)
        .cookieJar(CookieJar.NO_COOKIES)
        .retryOnConnectionFailure(false)
        .build()

    private val verifier = SignedContentManifestVerifier(ProductionContentKeys.trustedKeys())
    private val service = ApprovedContentService(downloadClient, verifier, metadataStore, storage)
    val policyConfigService = PolicyConfigService(downloadClient)
    val repository = ContentDownloadRepository(downloadClient, service, storage, metadataStore, ContentIntegrityVerifier())
    val coordinator = ContentDownloadCoordinator(appContext, repository)
    val policyUpdateCoordinator = PolicyUpdateCoordinator(appContext)

    companion object {
        @Volatile private var instance: ContentDependencies? = null

        fun get(context: Context): ContentDependencies = instance ?: synchronized(this) {
            instance ?: ContentDependencies(context).also { instance = it }
        }
    }
}
