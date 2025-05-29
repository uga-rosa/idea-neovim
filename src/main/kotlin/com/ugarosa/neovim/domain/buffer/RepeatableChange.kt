package com.ugarosa.neovim.domain.buffer

import com.ugarosa.neovim.bus.IdeaDocumentChange

data class RepeatableChange(
    var anchor: Int,
    var leftDel: Int,
    var rightDel: Int,
    val body: StringBuilder,
) {
    val start: Int get() = anchor - leftDel
    val end: Int get() = anchor + body.length + rightDel
    val delta: Int get() = body.length - (leftDel + rightDel)
    val ignoreTickIncrement = leftDel + rightDel + if (body.isEmpty()) 0 else 1

    fun overlap(c: IdeaDocumentChange): Boolean {
        return !(c.end < start || c.offset > end)
    }

    fun merge(c: IdeaDocumentChange) {
        var caretInBody = c.caret - start

        val needL = c.caret - c.offset
        val overL = needL - caretInBody
        if (overL > 0) {
            body.delete(0, caretInBody)
            leftDel += overL
            caretInBody = 0
        } else {
            body.delete(caretInBody - needL, caretInBody)
            caretInBody -= needL
        }

        val needR = c.end - c.caret
        val overR = needR - (body.length - caretInBody)
        if (overR > 0) {
            body.delete(caretInBody, body.length)
            rightDel += overR
        } else {
            body.delete(caretInBody, caretInBody + needR)
        }

        body.insert(caretInBody, c.newText)
    }
}

data class FixedChange(
    val start: Int,
    val end: Int,
    val replacement: List<String>,
)

fun splitDocumentChanges(changes: List<IdeaDocumentChange>): Pair<List<FixedChange>, RepeatableChange?> {
    var block: RepeatableChange? = null
    val queue = mutableListOf<FixedChange>()

    for (c in changes) {
        when {
            block == null && c.caretInside -> {
                block =
                    RepeatableChange(
                        anchor = c.caret,
                        leftDel = c.caret - c.offset,
                        rightDel = c.end - c.caret,
                        body = StringBuilder(c.newText),
                    )
            }

            block != null && block.overlap(c) -> {
                block.merge(c)
            }

            block == null || c.end <= block.start -> {
                queue.add(
                    FixedChange(
                        start = c.offset,
                        end = c.end,
                        replacement = c.lines,
                    ),
                )
                block?.apply {
                    anchor += c.delta
                }
            }

            else -> {
                queue.add(
                    FixedChange(
                        start = c.offset - block.delta,
                        end = c.end - block.delta,
                        replacement = c.lines,
                    ),
                )
            }
        }
    }

    return queue to block
}
