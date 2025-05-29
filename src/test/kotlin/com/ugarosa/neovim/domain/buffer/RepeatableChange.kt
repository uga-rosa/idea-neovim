package com.ugarosa.neovim.domain.buffer

import com.ugarosa.neovim.bus.IdeaDocumentChange
import com.ugarosa.neovim.domain.id.BufferId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class RepeatableChangeTest : FunSpec({
    context("splitDocumentChanges") {
        val id = BufferId(1)

        context("Simple Typing") {
            val changes =
                listOf(
                    IdeaDocumentChange(id, 0, 0, "a", 0),
                    IdeaDocumentChange(id, 1, 0, "b", 1),
                    IdeaDocumentChange(id, 2, 0, "c", 2),
                )
            val (fixed, block) = splitDocumentChanges(changes)
            fixed.shouldBeEmpty()
            block.shouldNotBeNull()
            block.body.toString() shouldBe "abc"
        }

        context("Typing with Deletion") {
            val changes =
                listOf(
                    IdeaDocumentChange(id, 0, 0, "a", 0),
                    // delete 'a' and insert 'b'
                    IdeaDocumentChange(id, 0, 1, "b", 1),
                    IdeaDocumentChange(id, 1, 0, "c", 1),
                )
            val (fixed, block) = splitDocumentChanges(changes)
            fixed.shouldBeEmpty()
            block.shouldNotBeNull()
            block.body.toString() shouldBe "bc"
        }

        context("Input char after caret") {
            val changes =
                listOf(
                    IdeaDocumentChange(id, 0, 0, "f", 0),
                    IdeaDocumentChange(id, 1, 0, "(", 1),
                    IdeaDocumentChange(id, 2, 0, ")", 2),
                    IdeaDocumentChange(id, 2, 0, "x", 2),
                )
            val (fixed, block) = splitDocumentChanges(changes)
            fixed.shouldBeEmpty()
            block.shouldNotBeNull()
            block.body.toString() shouldBe "f(x)"
        }

        context("Far cursor input before block") {
            val changes =
                listOf(
                    IdeaDocumentChange(id, 10, 0, "f", 10),
                    IdeaDocumentChange(id, 0, 0, "x".repeat(10), 11),
                    // offset and caret are shifted, but should be merged into block
                    IdeaDocumentChange(id, 21, 0, "(", 21),
                    IdeaDocumentChange(id, 22, 0, ")", 22),
                    IdeaDocumentChange(id, 22, 0, "x", 22),
                )
            val (fixed, block) = splitDocumentChanges(changes)
            fixed.size shouldBe 1
            fixed[0] shouldBe FixedChange(0, 0, "x".repeat(10))
            block.shouldNotBeNull()
            block.body.toString() shouldBe "f(x)"
        }

        context("Far cursor input after block") {
            val changes =
                listOf(
                    IdeaDocumentChange(id, 0, 0, "f", 0),
                    IdeaDocumentChange(id, 1, 0, "(", 1),
                    IdeaDocumentChange(id, 2, 0, ")", 2),
                    // Should be shifted 3 (`f()`)
                    IdeaDocumentChange(id, 13, 0, "after", 2),
                    IdeaDocumentChange(id, 2, 0, "x", 2),
                    // Should be shifted 4 (`f(x)`)
                    IdeaDocumentChange(id, 24, 0, "after2", 3),
                )
            val (fixed, block) = splitDocumentChanges(changes)
            fixed.size shouldBe 2
            fixed[0] shouldBe FixedChange(10, 10, "after")
            fixed[1] shouldBe FixedChange(20, 20, "after2")
            block.shouldNotBeNull()
            block.body.toString() shouldBe "f(x)"
        }
    }
})
