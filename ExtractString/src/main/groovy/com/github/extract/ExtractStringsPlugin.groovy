package com.github.extract

import com.github.extract.api.ExtractStringResAPI
import com.github.extract.excel_strings.Excel2StringsXml
import org.gradle.api.Plugin
import org.gradle.api.Project

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

        // res folder
        String resFolder = project.android.sourceSets.main.res.srcDirs[0].getAbsolutePath()

        // === 1. Task 1: xml to excel, create a task use group
        final
        def currentTask = project.tasks.create(["name": "strings2Excel", "group": "extractStrings"]) << {
            if (!project.android) {
                throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
            }

            // 创建接口实例
            api = project.extractConfig?.useRes ? new com.github.extract.res_extract.ResStringRecord(resFolder) : new BuildMergeStringRecord()

            // 获取配置参数
            long startTime = System.currentTimeMillis()
            println ">>>>>> Start extract strings resource!"
            println(">>>>>> Postfix:${project.extractConfig?.postfix}")
            println(">>>>>> TargetFilePath:${project.extractConfig?.targetFileFullPath}")
            println(">>>>>> useRes:${project.extractConfig?.useRes}")
            api.create(project.extractConfig, project.buildDir)
            println(">>>>> finish extract, Total time: ${(System.currentTimeMillis() - startTime) / 1000} ")
        }

        // 设置依赖于build Task (奇怪，加了判断无效，必须先手动build，再抽资源)，先分期
        if (!project.extractConfig?.useRes) {
            currentTask.dependsOn(project.tasks.findByName("build"))
        }

        // === 2. Task 2: excel to xml
        final excelToXml = project.tasks.create(["name": "excel2Strings", "group": "extractStrings"]) << {
            if (!project.android) {
                throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
            }

            // 构建目录
            def buidPath = project.buildDir
            def excelFilePath = project.extractConfig?.translatedExcelFilePath
            if (excelFilePath == null || !new File(excelFilePath).exists()) {
                throw new IllegalStateException('Excel2StringXml must be config the translated Excel File full path, and keep it right.')
            } else {
                println ">>>>>> Start excel2StringXml Task !"
                println(">>>>>> Translated excel file path: ${excelFilePath}")
                Excel2StringsXml work = new Excel2StringsXml(excelFilePath, buidPath)
                work.doWork()
                println(">>>>>> Finish it, You can find xml file at : $buidPath")
            }
        }
    }
}