package com.ugarosa.neovim.logger

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

sealed class LogEvent(val logger: Logger, val msg: String, val throwable: Throwable?) {
    class Trace(logger: Logger, msg: String) : LogEvent(logger, msg, null)

    class Debug(logger: Logger, msg: String, throwable: Throwable?) : LogEvent(logger, msg, throwable)

    class Info(logger: Logger, msg: String, throwable: Throwable?) : LogEvent(logger, msg, throwable)

    class Warn(logger: Logger, msg: String, throwable: Throwable?) : LogEvent(logger, msg, throwable)

    class Error(logger: Logger, msg: String, throwable: Throwable?) : LogEvent(logger, msg, throwable)
}

@Service(Service.Level.APP)
class MyLoggerChannel(
    scope: CoroutineScope,
) {
    private val channel = Channel<LogEvent>(Channel.UNLIMITED)

    init {
        scope.launch(Dispatchers.Default) {
            for (event in channel) {
                when (event) {
                    is LogEvent.Trace -> event.logger.trace(event.msg)
                    is LogEvent.Debug -> event.logger.debug(event.msg, event.throwable)
                    is LogEvent.Info -> event.logger.info(event.msg, event.throwable)
                    is LogEvent.Warn -> event.logger.warn(event.msg, event.throwable)
                    is LogEvent.Error -> event.logger.error(event.msg, event.throwable)
                }
            }
        }
    }

    fun send(event: LogEvent): Boolean {
        return channel.trySend(event).isSuccess
    }
}
