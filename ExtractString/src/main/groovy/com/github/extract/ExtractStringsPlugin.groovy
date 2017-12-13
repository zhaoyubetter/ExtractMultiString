package com.github.extract

import org.gradle.api.Plugin
import org.gradle.api.Project

class ExtractStringsPlugin implements Plugin<Project> {

    static final String APP = "com.android.application"
    static final String LIBRARY = "com.android.library"

    @Override
    void apply(Project project) {
        if (!(project.plugins.hasPlugin(APP) || project.plugins.hasPlugin(LIBRARY))) {
            throw new IllegalArgumentException(
                    'ExtractStrings gradle plugin can only be applied to android projects.')
        }

        project.extensions.create('extractConfig', ExtractConfiguration.class)

        // create a task use group
        project.getTasks().create(["name": "doExtractStringsToExcel", "group": "extract Strings"]) << {
            if (!project.android) {
                throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
            }

            // 获取配置参数
            println ">>> better >>> Start extract strings resource!"
            println("BuildStringFile:${project.extractConfig?.buildStringFile}")
            println("Postfix:${project.extractConfig?.postfix}")
            println("TargetFilePath:${project.extractConfig?.targetFileFullPath}")
        }
    }
}