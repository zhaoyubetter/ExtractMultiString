## 1.  ExtractString 介绍

Android 抽离字符串插件：

通过gradle 插件结合jxl，将app 或者 lib 库中的strings多语言资源文件，抽出成 excel 文件，并根据语言标识，生成多个excel表格，用以方便开发人查看，方便多语言整理(支持 strings-array)；



> 特别说明：本插件不能自动翻译，如需自动翻译，可以如下插件：
>
> 地址：https://github.com/Airsaid/AndroidLocalizePlugin
> 如果您要对比各个语言的差异，那本插件您不要错过！



**功能：**

- 将资源目录res下的`values` ，`values-en` 等，目录下strings资源文件，按照多语言类型，抽取并生成的相应的`excel`表格;
- 将翻译好的excel表格，反向生成相应的`values-xx`/strings.xml文件；

## 2. 效果展示

不录屏了。。。。。




## 3. 使用方法

### 3.1  配置步骤

1. 在项目的根目录gradle新增仓库gradle `classpath` 配置如下：

   ```groovy
   buildscript {
       dependencies {
           classpath 'com.android.tools.build:gradle:3.1.3'
           // 插件
           classpath 'com.github.extract:ExtractString:1.1.1'
       }
   ```




2. 在具体的module中 或者 app 中 apply 插件，并配置插件：

   ```groovy
   // apply 插件
   apply plugin: 'plugin.extractString'

   // 配置插件
   extractConfig {
       // 抽离的多语言values-XXX 后缀，以下为：values-en,values-zh-rTW
       postfix = ['en', 'zh-rTW']
       // 目标文件全路径，可不填，默认生成在build文件下
       targetFileFullPath = "D://aaa.xls" ；

       // excel 转strings.xml时，配置 已翻译好的 excel文件全路径
       translatedExcelFilePath = 'C://export_201809271122.xls'
   }
   ```

### 3.2 使用步骤之 导出excel文件

1. 配置好之后，rebuild当前工程后，可以在右侧gradle菜单找到：

![图片](https://github.com/zhaoyubetter/MarkdownPhotos/raw/master/img/plugin/1_20180928213812.png)



有2个菜单：

- excel2Strings   excel 转成 strings.xml文件；
- strings2Excel   strings.xml 转成 ecxel文件；



通过上面步骤3.2的步骤：

双击  `strings2Excel   `将生成excel文件，生成的文件默认在 `app/build`目录下，如下图示：



![集成操作符总览](https://github.com/zhaoyubetter/MarkdownPhotos/raw/master/img/plugin/2_20180928213947.png)

2. 打开文件看下一吧：

   ![图片](https://github.com/zhaoyubetter/MarkdownPhotos/raw/master/img/plugin/3_20180928214227.png)

因为配置了 `en`,`zh-rTW` 所以这里生成4个excel表格。我们需要重点关注的是最后一个表格；



3. 我们先看看各自的strings.xml文件：

   ![图片](https://github.com/zhaoyubetter/MarkdownPhotos/raw/master/img/plugin/values_compare__20180928215356.png)



通过上图，我们可以看到，每个语言资源文件，除了默认的values外，其他翻译的都不全，values文件下语言资源是基准！



4. 再看看 `values_compare`工作表（`图中编辑了一下`），大家应该就明白了：

   ![图片](https://github.com/zhaoyubetter/MarkdownPhotos/raw/master/img/plugin/5_20180928214659.png)



编程此张工作表，确认各个语言的翻译；



### 3.3 使用步骤之 导出strings.xml文件

1. 将编辑好的 `values_compare`工作表，全路径配置一下：

```groovy
extractConfig {
    postfix = ['en', 'zh-rTW']
    // 翻译好的模板文件
    translatedExcelFilePath = 'D:\\github\\ExtractMultiString\\app\\build\\export_201809282139.xls'
}
```

2. 执行命名 `excel2Strings`, 然后我们可以在 对应的工程的 `build` 目录发现文件，如下：

   ![图片](https://github.com/zhaoyubetter/MarkdownPhotos/raw/master/img/plugin/11_20180928215825.png)

直接去覆盖 values-XX下的strings.xml文件，风险太大，所以在这里，可以打开文件查看，并copy其内容了；
