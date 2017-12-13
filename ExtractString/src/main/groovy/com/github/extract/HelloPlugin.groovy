package com.github.extract

import org.gradle.api.Plugin
import org.gradle.api.Project

class HelloPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // 配置dsl 插件接受参数
        project.extensions.create('config', ExtractConfiguration)

        // create a task use group
        project.getTasks().create(["name": "hello", "group": "extract Strings"]) {
            println("Hello plugin")
        }
    }
}