package com.github.extract.res_extract

import com.github.extract.ExtractConfiguration
import com.github.extract.api.ExtractStringResAPI
import jxl.Workbook
import jxl.format.Alignment
import jxl.format.Colour
import jxl.format.UnderlineStyle
import jxl.format.VerticalAlignment
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.write.WritableWorkbook

import java.text.SimpleDateFormat

/**
 * 从 res 资源目录下获取资源文件
 * @author zhaoyubetter
 */
class ResStringRecord implements ExtractStringResAPI {

    /**
     * 默认 values folder
     */
    final String DEFAULT_FOLDER = "values"

    /**
     * 访问的values文件夹后缀，如：[values, values-en,values-zh-rTW]
     */
    def postfix = []          // 访问的values文件夹前缀，默认values

    /**
     * 每个语言，都有对应的 strings， 与 string-Array
     * key: String， values(-[en|zh-rTW])?
     * value: List， [strings, stringArray, noTranslateStringItems]
     */
    def langStrings = [:]     // 每个语言对应的 [string,stringArray]
    /**
     * excel文件全名
     */
    def excelFileName = ""
    /**
     * 资源目录
     */
    def resFolderPath = ""     // 资源目录

    public ResStringRecord(String resFolderPath) {
        this.resFolderPath = resFolderPath
    }

    static void main(String[] args) {
//        ResStringRecord aa = new ResStringRecord("")
//        // 文件配置
//        def fileName = "/export_" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".xls"
//        aa.excelFileName = fileName
//        aa.postfix << "values"                  // 添加默认
//        aa.postfix << "values-en"
//        aa.resFolderPath = "/Users/zhaoyu/Documents/github/ExtractMultiString/app/src/main/res"
//        aa.testGetXml()

        def postfix = ['a', 'b', 'c']
        postfix.forEach {
            println(it + ">>>>==")
        }
        println(postfix)
    }

    @Override
    void create(ExtractConfiguration configuration, File buildFile) {
        this.postfix << DEFAULT_FOLDER                  // 添加默认  ->  [values]
        configuration.postfix.each {
            // 资源文件夹 ->  [values, values-en, values-zh-rTW]
            this.postfix << "values-${it}"
        }

        // 文件配置
        def fileName = "export_" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".xls"
        this.excelFileName = configuration.targetFileFullPath ?: (buildFile.getAbsolutePath() + "/" + fileName)
        this.excelFileName = fileName
        doWork()
    }

    private void doWork() {
        def invalidDir = []     // 无效的目录，可能配置了一些 values-5678，或者是一些其他不存在的目录

        // 遍历各个语言目录，获取资源信息
        postfix.each { it ->
            def filePath = "${resFolderPath}/${it}"
            File dir = new File(filePath)
            // all xml file in res/values or (res/values-%s)
            def files = []
            if (dir.exists() && dir.isDirectory()) {
                dir.listFiles(new FileFilter() {
                    @Override
                    boolean accept(File pathname) {
                        return pathname.getName().endsWith(".xml")
                    }
                })?.each { files << it }     // handle all xml file

                langStrings.put(it, handleXmlFiles(files))
            } else {
                invalidDir << it
            }
        }
        postfix.removeAll(invalidDir)       // 移除无效目录

        // 生成各个excel
        createAllExcel(langStrings)        // 生成excel
    }

    /**
     * 生成Excel
     * @param langStrings 各个语言的资源集合
     */
    private def createAllExcel(Map langStrings) {
        // 1、创建工作簿(WritableWorkbook)对象
        WritableWorkbook writeBook = Workbook.createWorkbook(new File(excelFileName))

        langStrings.each { lang ->
            def title = lang.key
            def stringsItems = lang.value[0]
            def stringsArrayItems = lang.value[1]
            def noTranslateStringItems = lang.value[2]

            // --- 2. 创建工作表
            def sheet = createSheet(writeBook, "${title}")
            // --- 设置表头
            // 创建单元格(Label)对象，这里是表头，并设置一下格式
            def titleCellFormat = getTitleCellFormat()
            def cell_title_key = new Label(0, 0, "string_key", titleCellFormat)
            def cell_title_default_value = new Label(1, 0, "${title == ("values") ? "default" : title.substring(title.indexOf('-'))}_value", titleCellFormat)
            sheet.addCell(cell_title_key)
            sheet.addCell(cell_title_default_value)

            // --- 添加内容 stringsItems
            stringsItems.eachWithIndex { key, value, index ->
                def cell_key = new Label(0, index + 1, key)
                def cell_value = new Label(1, index + 1, value)
                sheet.addCell(cell_key)
                sheet.addCell(cell_value)
            }

            // --- 添加内容 stringsArrayItems, 这里的value为数组
            def currentRow = sheet.rows
            stringsArrayItems.eachWithIndex { array, index ->
                currentRow = sheet.rows
                def cell_key = new Label(0, currentRow, array.key)
                // 数组
                array.value.eachWithIndex { it, i ->
                    def cell_value = new Label(1, currentRow + i, it)
                    sheet.addCell(cell_value)
                }

                sheet.mergeCells(0, currentRow, 0, array.value.size + currentRow - 1) // 合并单元格
                sheet.addCell(cell_key)
            }

            // --- 添加内容 noTranslate, 不需要翻译的
            currentRow = sheet.rows
            noTranslateStringItems.eachWithIndex { key, value, index ->
                def cell_key = new Label(0, index + 1, key)
                def cell_value = new Label(1, index + 1, value)
                sheet.addCell(cell_key)
                sheet.addCell(cell_value)
            }
        }

        // 3.生成对比的excel
        createDiffSheet(writeBook, langStrings)

        // --- 写入文件
        writeBook.write()
        writeBook.close()
    }

    /**
     * 各种语言对比的 sheet
     * @param writeBook
     * @return
     */
    private def createDiffSheet(writeBook, langStrings) {
        def defaultStringsItems = [:]
        def defaultStringArrayItems = [:]
        def defaultStringsItemNoTranslate = [:]
        (defaultStringsItems, defaultStringArrayItems, defaultStringsItemNoTranslate) = langStrings.get("values")
        postfix.remove("values")      // 移除默认

        // 默认语言，直接return
        if (postfix.size() <= 0) {
            return
        }

        def stringsItems = [:]            // string
        def stringsArrayItems = [:]       // string-array
        def stringsItemNoTranslate = [:]

        // --- 创建工作表
        def sheet = createSheet(writeBook, "values_compare")

        // --- 设置表头 （默认）
        def titleCellFormat = getTitleCellFormat()
        def cell_title_key = new Label(0, 0, "string_key", titleCellFormat)
        def cell_title_default_value = new Label(1, 0, "default_value", titleCellFormat)
        sheet.addCell(cell_title_key)
        sheet.addCell(cell_title_default_value)

        // --- 设置表头（其他语言）
        postfix.eachWithIndex { it, index ->
            // --- 设置表头（其他语言）
            def cell_title_value = new Label(index + 2, 0, "${it.substring(it.indexOf('-'))}_value", titleCellFormat)
            sheet.addCell(cell_title_value)
        }

        // --- 设置内容 stringsItems（默认语言）
        defaultStringsItems.eachWithIndex { key, value, index ->
            def cell_key = new Label(0, index + 1, key)
            def cell_value = new Label(1, index + 1, value)
            sheet.addCell(cell_key)
            sheet.addCell(cell_value)

            // 其他语言
            postfix.eachWithIndex { it, i ->
                (stringsItems, stringsArrayItems, stringsItemNoTranslate) = langStrings.get(it)
                stringsItems.find { it.key == key }?.each {
                    def cell_other_value = new Label(2 + i, index + 1, it.value)   // 3列1行开始
                    sheet.addCell(cell_other_value)
                }
            }
        }

        // --- 添加内容 stringsArrayItems（默认语言）
        def currentRow = sheet.rows
        defaultStringArrayItems.eachWithIndex { array, index ->
            currentRow = sheet.rows
            def cell_key = new Label(0, currentRow, array.key)
            // 数组
            array.value.eachWithIndex { it, i ->
                def cell_value = new Label(1, currentRow + i, it)
                sheet.addCell(cell_value)
            }
            sheet.mergeCells(0, currentRow, 0, array.value.size + currentRow - 1) // 合并单元格
            sheet.addCell(cell_key)

            // 其他语言处理
            postfix.eachWithIndex { it, i ->
                (stringsItems, stringsArrayItems, stringsItemNoTranslate) = langStrings.get(it)
                def arraysItems = stringsArrayItems.find { it.key == array.key }
                if (arraysItems != null && arraysItems.value.size == array.value.size) {
                    // 为null或数量不对
                    arraysItems.value.eachWithIndex { item, inx ->
                        def cell_value = new Label(2 + index, currentRow + inx, item)
                        sheet.addCell(cell_value)
                    }
                } else {
                    if (arraysItems == null) {
                        println("Can't found string-array [${array.key}] in ${it}")
                    } else {
                        println("string-array [${array.key}] in ${it} size not equals")
                    }
                }
            }
        }

        // --- 添加内容 stringsItem - 不需要翻译的 (默认语言)
        currentRow = sheet.rows
        stringsItemNoTranslate.eachWithIndex {key, value, index ->
            def cell_key = new Label(0, index + 1, key)
            def cell_value = new Label(1, index + 1, value)
            sheet.addCell(cell_key)
            sheet.addCell(cell_value)

            // 其他语言
            postfix.eachWithIndex { it, i ->
                (stringsItems, stringsArrayItems) = langStrings.get(it)
                stringsItems.find { it.key == key }?.each {
                    def cell_other_value = new Label(2 + i, index + 1, it.value)   // 3列1行开始
                    sheet.addCell(cell_other_value)
                }
            }
        }
    }

    /**
     * 创建工作表
     * @param writeBook
     * @param name 表名称
     * @return
     */
    private createSheet(writeBook, name) {
        def count = writeBook.getNumberOfSheets() > 0 ? writeBook.getNumberOfSheets() : 0
        def sheet = writeBook.createSheet(name, count)
        sheet
    }

    /**
     * 表头 title 格式化
     * @return
     */
    private WritableCellFormat getTitleCellFormat() {
        WritableCellFormat cellFormat = new WritableCellFormat()
        cellFormat.setBackground(Colour.GREEN)
        cellFormat.setAlignment(Alignment.CENTRE)                   //设置文字居中对齐方式;
        cellFormat.setVerticalAlignment(VerticalAlignment.CENTRE)   //设置垂直居中;
        WritableFont font2 = new WritableFont(WritableFont.ARIAL, 13, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.WHITE)
        cellFormat.setFont(font2)
        return cellFormat
    }

    /**
     * 获取执行xml文件中的所有 string，stringArray，并返回长度为3的list集合
     * @param files xml 文件
     * @return list
     */
    private def handleXmlFiles(List files) {
        def stringItems = [:]           // string           (key,value)
        def arrayArrayItems = [:]       // string-array     (key, [])
        def noTranslateItems = [:]      //
        files.each { it ->
            def items = getStrings(it)[0]
            if (items.size() > 0) {
                stringItems << items // normal
            }

            // 不需要翻译的
            def noTranslate = getStrings(it)[1]
            if (noTranslate.size() > 0) {
                noTranslateItems << noTranslate
            }
        }

        // 获取所有string后，再来处理string-array，因为string-array可能引入string里面的内容
        files.each { it ->
            def items = getStringArrays(it, stringItems)
            if (items.size() > 0) {
                arrayArrayItems << items
            }
        }

        // 普通strings，数组，不需要翻译的
        [stringItems, arrayArrayItems, noTranslateItems]
    }

    /**
     * get all string in specified file
     * 获取指定文件下的所有string，并返回list，list 长度为2，类型为map；
     * map key 为 string 名称，value 为string的值
     * @param file
     * @return map
     */
    private def getStrings(File file) {
        println("--------> Get string from file [${file.getParentFile().getName()}/${file.getName()}]")
        def root = new XmlParser().parse(file)
        def result = []
        def stringItems = [:]
        def notTranslateItems = [:]   //
        // strings
        root.string?.each { it ->
            def translatableValue = it.attributes()["translatable"]
            def item = it.attributes()["name"]
            // 过滤掉不需要国际化数组配置
            if (null == translatableValue || Boolean.valueOf(translatableValue)) {
                stringItems << [(item): it.text()]
            } else {
                notTranslateItems << [(item): it.text()]
            }
        }
        println("string size:${stringItems.size()}")

        result << stringItems
        result << notTranslateItems

        result
    }

    /**
     * get all string-array in specified file
     * 获取指定文件下的所有string-array，并返回map，map key 为 string 名称，value 为string[]，或者@
     * @param file
     * @return map
     */
    private Map getStringArrays(File file, stringItems) {
        println("--------> Get string-array from file [${file.getParentFile().getName()}/${file.getName()}]")
        def root = new XmlParser().parse(file)
        def arrayArrayItems = [:]       // key 与 list
        root."string-array".each {
            def translatableValue = it.attributes()["translatable"]
            //检测不需要国际化数组配置
            if (null == translatableValue || Boolean.valueOf(translatableValue)) {
                def arrayItems = []
                it.value().each {
                    def value = it.text()
                    //解出引用嵌引用的
                    def matcher = value =~ /@string\/(.+)/
                    if (matcher) {
                        arrayItems << stringItems[matcher[0][1]]
                    } else {
                        arrayItems << value
                    }
                }
                arrayArrayItems << [(it.attributes()["name"]): (arrayItems)]
            }
        }
        println("array size:${arrayArrayItems.size()}")
        arrayArrayItems
    }
}