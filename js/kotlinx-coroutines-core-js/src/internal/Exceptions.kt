package kotlinx.coroutines.internal

import kotlin.coroutines.*

internal actual fun <E : Throwable> recoverStackTrace(exception: E, continuation: Continuation<*>): E = exception
internal actual fun <E : Throwable> recoverStackTrace(exception: E): E = exception
