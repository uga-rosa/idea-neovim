package com.ugarosa.neovim.domain.buffer

data class RepeatableChange(
    val beforeDelete: Int,
    val afterDelete: Int,
    val text: String,
    val caretAdvance: Int,
) {
    val ignoreTickIncrement = beforeDelete + afterDelete + if (text.isEmpty()) 0 else 1

    companion object {
        fun merge(changes: List<RepeatableChange>): RepeatableChange {
            require(changes.isNotEmpty()) { "Cannot merge an empty list of changes" }

            var beforeDelete = changes.first().beforeDelete
            var afterDelete = changes.first().afterDelete
            val body = StringBuilder(changes.first().text)
            var caret = changes.first().caretAdvance

            for (c in changes.drop(1)) {
                if (c.beforeDelete > 0) {
                    val deleteStart = caret - c.beforeDelete
                    if (deleteStart < 0) {
                        beforeDelete += -deleteStart
                        body.delete(0, caret)
                        caret = 0
                    } else {
                        body.delete(deleteStart, caret)
                        caret -= c.beforeDelete
                    }
                }

                if (c.afterDelete > 0) {
                    val deleteEnd = caret + c.afterDelete
                    val overflow = deleteEnd - body.length
                    if (overflow > 0) {
                        afterDelete += overflow
                        body.delete(caret, body.length)
                    } else {
                        body.delete(caret, deleteEnd)
                    }
                }

                if (c.text.isNotEmpty()) {
                    body.insert(caret, c.text)
                }

                if (c.caretAdvance != 0) {
                    caret += c.caretAdvance
                }
                caret = caret.coerceIn(0, body.length)
            }

            return RepeatableChange(
                beforeDelete = beforeDelete,
                afterDelete = afterDelete,
                text = body.toString(),
                caretAdvance = caret,
            )
        }
    }
}
