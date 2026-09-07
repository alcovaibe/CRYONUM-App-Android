package com.cryonum.content

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Call

/** Cancels the socket during connect, headers AND blocking body reads. */
internal suspend fun <T> withCancellableCall(call: Call, block: suspend () -> T): T = coroutineScope {
    val cancellation = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { call.cancel() }
    }
    try { block() } finally { cancellation.cancel() }
}
