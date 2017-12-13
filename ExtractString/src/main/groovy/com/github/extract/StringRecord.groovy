package com.github.extract

import jxl.Workbook
import jxl.format.Alignment
import jxl.format.Colour
import jxl.format.VerticalAlignment
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableWorkbook

/**
 * Created by zhaoyu1 on 2017/7/31.
 */
class StringRecord {
    // values-zh/values-zh.xml
    def final TEMPLATE_VALUES = "/intermediates/res/merged/debug/values%s/values%s.xml"
    def final APP_BUILD_PATH = "C:/Users/zhaoyu1/Documents/KotlinAndroidDemo/app/build"     // app build 目录

    def project
    def valuesPath
    def excelFile
    def postfix = ["", "zh-rCN", "zh-rHK", "zh-rTW"]                    // 访问的values文件
    def langStrings = [:]
    def excelFileName = "write.xls"

    static void main(String[] args) {
        def test = new StringRecord()
        test.getDefaultStrings()
    }

    def getDefaultStrings() {
        // 1、创建工作簿(WritableWorkbook)对象
        WritableWorkbook writeBook = Workbook.createWorkbook(new File(excelFileName))

        // 2.各个语言的分别写入
        langStrings = [:]
        postfix.each {
            langStrings.put(it, createLangSheet(it, writeBook))
        }

        // 3.写入对比的
        createDiffSheet(writeBook)

        // --- 写入文件
        writeBook.write()
        writeBook.close()
    }

    /**
     * 各种语言对比的 sheet
     * @param writeBook
     * @return
     */
    def createDiffSheet(writeBook) {
        def defaultStringsItems = [:]
        def defaultStringArrayItems = [:]
        (defaultStringsItems, defaultStringArrayItems) = langStrings.get("")
        postfix.remove("")      // 移除

        def stringsItems = [:]           // string
        def stringsArrayItems = [:]     // string-array

        // --- 创建工作表
        def sheet = createSheet(writeBook, "values_compare")

        // --- 设置表头 （默认）
        def titleCellFormat = getTitleCellFormat()
        def cell_title_key = new Label(0, 0, "string_key", titleCellFormat)
        def cell_title_default_value = new Label(1, 0, "_value", titleCellFormat)
        sheet.addCell(cell_title_key)
        sheet.addCell(cell_title_default_value)

        // --- 设置表头（其他语言）
        postfix.eachWithIndex { it, index ->
            (stringsItems, stringsArrayItems) = langStrings.get(it)
            // --- 设置表头（其他语言）
            def cell_title_value = new Label(index + 2, 0, "${it}_value", titleCellFormat)
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
                        println("Can't found string-array [${array.key}] in values-${it}.xml")
                    } else {
                        println("string-array [${array.key}] in values-${it}.xml size not equals values.xml")
                    }
                }
            }
        }
    }

    /**
     * 根据语言标签生成的不同的 sheet
     * @param langTag
     * @param writeBook
     * @return
     */
    def createLangSheet(langTag, writeBook) {
        // --- 必要的前置检查
        langTag = langTag?.length() > 0 ? "-".concat(langTag) : ""       // 处理短杠
        println("Start parse file values${langTag}.xml  ...")
        def pre = APP_BUILD_PATH
        final values_path = pre + String.format(TEMPLATE_VALUES, langTag, langTag)
        def file = new File(values_path)
        if (!file.exists()) {
            println("File [values$langTag].xml not found,the absolute path is $values_path")
            println("End parse file values${langTag}")
            return null
        }

        // --- 创建工作表
        def sheet = createSheet(writeBook, "values${langTag}")

        def stringsItems = [:]           // string
        def stringsArrayItems = [:]      // string-array
        (stringsItems, stringsArrayItems) = parserStringValuesFile(file)

        // --- 设置表头
        // 3、创建单元格(Label)对象，这里是表头，并设置一下格式
        def titleCellFormat = getTitleCellFormat()
        def cell_title_key = new Label(0, 0, "string_key", titleCellFormat)
        def cell_title_default_value = new Label(1, 0, "${langTag.startsWith("-") ? langTag.substring(1) : langTag}_value", titleCellFormat)
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

        println("End parse file values${langTag}.xml")
        [stringsItems, stringsArrayItems]
    }

    private def parserStringValuesFile(File file) {
        def valuesFile = file
        def root = new XmlParser().parse(valuesFile)
        def stringItems = [:]
        root.string.each {
            def translatableValue = it.attributes()["translatable"]
            //检测不需要国际化数组配置
            if (null == translatableValue || Boolean.valueOf(translatableValue)) {
                def item = it.attributes()["name"]
                stringItems << [(item): it.text()]
            }
        }
        println("string size:${stringItems.size()}")
        def arrayArrayItems = [:]
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
        [stringItems, arrayArrayItems]
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
        return cellFormat
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
}
