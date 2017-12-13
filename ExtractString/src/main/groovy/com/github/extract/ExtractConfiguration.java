package com.github.extract;

import java.util.List;

/**
 * Created by zhaoyu1 on 2017/12/13.
 */
public class ExtractConfiguration {
    // 配置过来values的后缀名，对于语言的后缀（如：en-rUS，默认不配置，则生成default）
    List<String> postfix;
    // 生成的目标文件全路径，不配置，则使用默认
    String targetFileFullPath;
    // 配置检测目录,如果用户动态设置了build目录
    String buildStringFile;
}
