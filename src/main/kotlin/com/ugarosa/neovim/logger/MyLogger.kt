package com.ugarosa.neovim.logger

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger

inline fun <reified T : Any> T.myLogger(): MyLogger = MyLogger.getInstance(T::class.java)

class MyLogger private constructor(
    private val delegate: Logger,
) {
    companion object {
        fun getInstance(category: String): MyLogger {
            return MyLogger(Logger.getInstance(category))
        }

        fun getInstance(clazz: Class<*>): MyLogger {
            return MyLogger(Logger.getInstance(clazz))
        }
    }

    private val channel = service<MyLoggerChannel>()

    private fun enqueue(event: LogEvent) {
        if (!channel.send(event)) {
            delegate.warn("Log channel is full or closed: ${event.msg}")
        }
    }

    fun trace(msg: String) {
        enqueue(LogEvent.Trace(delegate, msg))
    }

    fun debug(
        msg: String,
        throwable: Throwable? = null,
    ) {
        enqueue(LogEvent.Debug(delegate, msg, throwable))
    }

    fun info(
        msg: String,
        throwable: Throwable? = null,
    ) {
        enqueue(LogEvent.Info(delegate, msg, throwable))
    }

    fun warn(
        msg: String,
        throwable: Throwable? = null,
    ) {
        enqueue(LogEvent.Warn(delegate, msg, throwable))
    }

    fun error(
        msg: String,
        throwable: Throwable? = null,
    ) {
        enqueue(LogEvent.Error(delegate, msg, throwable))
    }
}
