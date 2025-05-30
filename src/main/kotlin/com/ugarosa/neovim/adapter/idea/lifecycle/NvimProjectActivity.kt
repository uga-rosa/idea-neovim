package com.ugarosa.neovim.adapter.idea.lifecycle

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class NvimProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<ProjectLifecycleRegistry>()
    }
}
