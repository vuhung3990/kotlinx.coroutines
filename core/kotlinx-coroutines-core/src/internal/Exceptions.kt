/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.internal

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.coroutines.jvm.internal.*

internal actual fun <E : Throwable> recoverStackTrace(exception: E): E {
    if (!DEBUG || exception is CancellationException) {
        return exception
    }

    return tryWrapException(exception) ?: exception
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
internal actual fun <E : Throwable> recoverStackTrace(exception: E, continuation: Continuation<*>): E {
    if (!DEBUG || exception is CancellationException || continuation !is CoroutineStackFrame) {
        return exception
    }

    val newException = tryWrapException(exception) ?: return exception
    val stacktrace = fillInStackTrace(continuation).toTypedArray()
    if (stacktrace.isEmpty()) return exception
    newException.stackTrace = stacktrace
    return newException
}

@Suppress("UNCHECKED_CAST")
private fun <E : Throwable> tryWrapException(exception: E): E? {
    // TODO multi-release JAR to cache in ClassValueMap
    var newException: E? = null
    try {
        for (constructor in exception.javaClass.constructors.sortedBy { -it.parameterTypes.size }) {
            val parameters = constructor.parameterTypes
            if (parameters.size == 2 && parameters[0] == String::class.java && parameters[1] == Throwable::class.java) {
                newException = constructor.newInstance(exception.message, exception) as E
            } else if (parameters.size == 1 && parameters[0] == Throwable::class.java) {
                newException = constructor.newInstance(exception) as E
            } else if (parameters.isEmpty()) {
                newException = (constructor.newInstance() as E).also { it.initCause(exception) }
            }

            if (newException != null) {
                break
            }
        }
    } catch (e: Exception) {
        // Do nothing
    }
    return newException
}

private fun fillInStackTrace(frame: CoroutineStackFrame): ArrayList<StackTraceElement> {
    val stack = ArrayList<StackTraceElement>()

    stackTraceElement(frame)?.let {  stack.add(it) }
    var last = frame
    while (true) {
        last = last.callerFrame ?: break
        stackTraceElement(last)?.let {  stack.add(it) }
    }
    return stack
}

// TODO basic stub before 1.3
private fun stackTraceElement(continuation: CoroutineStackFrame): StackTraceElement? = continuation.getStackTraceElement()
