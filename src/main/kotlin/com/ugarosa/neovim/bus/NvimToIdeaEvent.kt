package com.ugarosa.neovim.bus

import com.ugarosa.neovim.domain.id.BufferId
import com.ugarosa.neovim.domain.mode.NvimMode
import com.ugarosa.neovim.domain.position.NvimPosition
import com.ugarosa.neovim.domain.position.NvimRegion

sealed interface NvimToIdeaEvent

data class NvimBufLines(
    val bufferId: BufferId,
    val changedTick: Long,
    val firstLine: Int,
    val lastLine: Int,
    val replacementLines: List<String>,
) : NvimToIdeaEvent

data class NvimCursorMoved(
    val bufferId: BufferId,
    val pos: NvimPosition,
) : NvimToIdeaEvent

data class ModeChanged(
    val bufferId: BufferId,
    val mode: NvimMode,
) : NvimToIdeaEvent

data class VisualSelectionChanged(
    val bufferId: BufferId,
    val regions: List<NvimRegion>,
) : NvimToIdeaEvent
