package com.github.extract

import com.github.extract.api.ExtractStringResAPI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class ExtractStringsPlugin implements Plugin<Project> {

    static final String APP = "com.android.application"
    static final String LIBRARY = "com.android.library"

    private ExtractStringResAPI api

    @Override
    void apply(Project project) {
        if (!(project.plugins.hasPlugin(APP) || project.plugins.hasPlugin(LIBRARY))) {
            throw new IllegalArgumentException(
                    'ExtractStrings gradle plugin can only be applied to android projects.')
        }

        project.extensions.create('extractConfig', ExtractConfiguration.class)
        api = new StringResRecord()

        // create a task use group
        final def currentTask = project.tasks.create(["name": "doExtractStringsToExcel", "group": "extract Strings"]) << {
            if (!project.android) {
                throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
            }

            // 获取配置参数
            long startTime = System.currentTimeMillis()
            println ">>>>>> Start extract strings resource!"
            println(">>>>>> Postfix:${project.extractConfig?.postfix}")
            println(">>>>>> TargetFilePath:${project.extractConfig?.targetFileFullPath}")
            api.create(project.extractConfig, project.buildDir)
            println(">>>>> finish extract, Total time: ${(System.currentTimeMillis() - startTime) / 1000} ")
        }

        // 设置依赖于build Task
        currentTask.dependsOn(project.tasks.findByName("build"))
    }
}