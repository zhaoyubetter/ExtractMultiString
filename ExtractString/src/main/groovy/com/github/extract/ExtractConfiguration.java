package com.github.extract;

import java.util.List;

/**
 * Created by zhaoyu1 on 2017/12/13.
 */
public class ExtractConfiguration {
    // 配置过来values的后缀名，对于语言的后缀（如：en-rUS，默认不配置，则生成default）
    public List<String> postfix;
    // 生成的目标文件全路径，不配置，则使用默认
    public String targetFileFullPath;

    // 已经翻译好的excel 文件路径
    public String translatedExcelFilePath;
    /**
     * 使用res目录下的资源,false 使用build下的merge资源
     */
    public boolean useRes = true;
    // 配置检测目录,如果用户动态设置了build目录
    // public String buildPath;
}
