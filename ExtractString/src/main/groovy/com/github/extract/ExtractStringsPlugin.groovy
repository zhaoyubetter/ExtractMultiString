package com.github.extract

import org.gradle.api.Plugin
import org.gradle.api.Project

class ExtractStringsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // 配置dsl 插件接受参数
        project.extensions.create('extractConfig', ExtractConfiguration)

        // create a task use group
        project.getTasks().create(["name": "doExtractStringsToExcel", "group": "extract Strings"]) {
            if (!project.android) {
                throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
            }

            //配置不能为空
            if (project.extractConfig.postfix == null || project.apkdistconf.destDir == null) {
                project.logger.info('Apkdist conf should be set!')
                return
            }
        }
    }
}