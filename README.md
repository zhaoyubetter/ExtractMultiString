# ExtractMultiString
抽离字符串插件：通过gradle插件结合jxl，将app or lib 库中的资源文件，抽出成 excel 文件，
方便开发人查看，方便多语言整理(支持 strings-array)；


# 使用说明

1. 在项目的根目录`build.gradle`,添加依赖如下

```java
    dependencies {
        ...
        classpath 'com.github.extract:ExtractString:1.0.0'
    }

```

2. 在对应的 module 下 `build.gradle`下，apply 插件，如下：

```java
apply plugin: 'com.android.application'
apply plugin: 'plugin.extractString'  //apply 插件

// 配置插件dsl
extractConfig {
    postfix = ['en', 'zh-rTW']    // 表示提出 en、 zh-rTW 下的资源
    //targetFileFullPath = "D://aaa.xls"  // 目标文件全路径，可不填，默认生成在build文件下；
}
```

3. clean 工程后，可以发现，在对应的 module 下，可发现 `extract strings` 组，展开，点击执行；

## TODO
1. 直接获取 res 目录的资源文件有待支持；