package com.github.extract.res

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
 * 考虑从 res 目录下获取资源文件，并实现demo例子，明天来写 gralde 插件
 */
class GroovyEachFiles {

    def postfix = []          // 访问的values文件夹前缀，默认values
    def langStrings = [:]     // 每个语言对应的 [string,stringArray]
    def excelFileName = ""

    void testGetXml() {
        def langs = ["en", "zh-rTW"]    // 外界传入
        postfix << "values"             // 添加默认
        langs.each {
            postfix << "values-${it}"
        }
        def fileName = "export_" + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".xls"
        //this.excelFileName = configuration.targetFileFullPath ?: (buildFile.getAbsolutePath() + "/" + fileName)
        this.excelFileName = fileName

        // 遍历各个语言目录，获取资源信息
        postfix.each { it ->
            def filePath = "/Users/zhaoyu/Documents/github/ExtractMultiString/app/src/main/res/${it}"
            File dir = new File(filePath)
            def files = []      // all xml file in res/values or (res/values-%s)
            if (dir.isDirectory()) {
                dir.listFiles(new FileFilter() {
                    @Override
                    boolean accept(File pathname) {
                        return pathname.getName().endsWith(".xml")
                    }
                })?.each { files << it }     // handle all xml file
                langStrings.put(it, handleXmlFiles(files))
            }
        }

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
        (defaultStringsItems, defaultStringArrayItems) = langStrings.get("values")
        postfix.remove("values")      // 移除

        // 默认语言，直接return
        if (postfix.size() <= 0) {
            return
        }

        def stringsItems = [:]           // string
        def stringsArrayItems = [:]     // string-array

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
            (stringsItems, stringsArrayItems) = langStrings.get(it)
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
                (stringsItems, stringsArrayItems) = langStrings.get(it)
                stringsItems.find { it.key == key }?.each {
                    def cell_other_value = new Label(2 + i, index + 1, it.value)   // 3列1行开始
                    sheet.addCell(cell_other_value)
                }
            }
        }

        // --- 添加内容 stringsArrayItems
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
                (stringsItems, stringsArrayItems) = langStrings.get(it)
                def arraysItems = stringsArrayItems.find { it.key == array.key }
                if (arraysItems != null && arraysItems.value.size == array.value.size) {  // 为null或数量不对
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

    private def handleXmlFiles(List files) {
        def stringItems = [:]           // string           (key,value)
        def arrayArrayItems = [:]       // string-array     (key, [])
        files.each { it ->
            def items = getStrings(it)
            if (items.size() > 0) {
                stringItems << items
            }
        }

        // 获取所有string后，在来处理string-array，因为string-array可能引入string里面的内容
        files.each { it ->
            def items = getStringArrays(it, stringItems)
            if (items.size() > 0) {
                arrayArrayItems << items
            }
        }

        [stringItems, arrayArrayItems]
    }

    /**
     * get all string in specified file
     * @param file
     * @return map
     */
    private Map getStrings(File file) {
        println("--------> Get string from file [${file.getParentFile().getName()}/${file.getName()}]")
        def root = new XmlParser().parse(file)
        def stringItems = [:]
        // strings
        root.string?.each { it ->
            def translatableValue = it.attributes()["translatable"]
            // 检测不需要国际化数组配置
            if (null == translatableValue || Boolean.valueOf(translatableValue)) {
                def item = it.attributes()["name"]
                stringItems << [(item): it.text()]
            }
        }
        println("string size:${stringItems.size()}")
        stringItems
    }

    /**
     * get all string-array in specified file
     * @param file
     * @return
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