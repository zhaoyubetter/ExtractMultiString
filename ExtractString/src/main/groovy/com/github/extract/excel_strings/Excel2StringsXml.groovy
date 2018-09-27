package com.github.extract.excel_strings

import com.github.extract.res_extract.ResStringRecord
import groovy.xml.MarkupBuilder
import jxl.Cell
import jxl.Range
import jxl.Sheet
import jxl.Workbook

/**
 * 将 excel 中的 values_compare 转换成各自对应 语言资源文件
 */
class Excel2StringsXml {

    /**
     * excel 文件目录
     */
    def excelFilePath = ""

    /**
     * app 编译目录
     */
    def app_buildPath

    public Excel2StringsXml(excelFilePath, appBuildPath) {
        this.excelFilePath = excelFilePath
        this.app_buildPath = appBuildPath
    }

    static void main(String[] args) {
//        def excelFilePath = "/Users/zhaoyu1/Documents/github/ExtractMultiString/app/build/export_201809271122.xls"
//        def app_buildPath = "/Users/zhaoyu1/Documents/github/ExtractMultiString/app/build/"
//        Excel2StringsXml demo = new Excel2StringsXml(excelFilePath, app_buildPath)
//        demo.doWork()
    }

    public def doWork() {
        // === 1. 前置检查
        File excelFile = new File(excelFilePath)
        if (!excelFile.exists()) {
            throw IllegalArgumentException("The file path ${excelFilePath} is not exist!")
        }
        // 获取比较sheet
        Sheet compareSheet = null
        Workbook workbook = Workbook.getWorkbook(excelFile)
        workbook.getSheets().eachWithIndex { sheet, int index ->
            if ("values_compare" == sheet.getName()) {
                compareSheet = sheet
                return
            }
        }
        if (compareSheet == null) {
            throw IllegalArgumentException("The excel file ${excelFile.getName()} not have sheet named 'values_compare', please check !")
        }

        // 2. 生成的strings.xml 名字,key_value
        def created_fileNamesMap = [:]  // key: col num, value: strings.xml name
        Cell[] titleCells = compareSheet.getRow(0)
        titleCells.eachWithIndex { cell, index ->
            if (index > 0) {
                def cellValue = cell.getContents()
                created_fileNamesMap << [(index): "strings_" + cellValue + ".xml"]
            }
        }

        // 3. 分别创建 string xml file
        created_fileNamesMap.each { entry ->
            println("------ 获取 ${entry.value.substring(0, entry.value.lastIndexOf('.'))} 列数据")
            def result = getFileData(compareSheet, entry.key)
            println("------ 创建 ${entry.value} 文件")
            createFile(result, entry.value)
        }

        workbook.close()
    }

    /**
     * createXmlFile
     * @param result
     * @param fileName
     */
    private def createFile(result, fileName) {
        def stringsItem = result[0]
        def arrayItems = result[1]
        def noTranslateItems = result[2]

        // <resources xmlns:tools="http://schemas.android.com/tools"
        // xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2" tools:ignore="MissingTranslation">

        def mb = new MarkupBuilder(new File(app_buildPath, fileName).newPrintWriter())
        mb.setDoubleQuotes(true)
        mb.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        mb.resources("xmlns:tools": "http://schemas.android.com/tools", "xmlns:xliff": "urn:oasis:names:tc:xliff:document:1.2",
                "tools:ignore": "MissingTranslation") {

            // 1.标准string   <string name="xxx">XXX</string>
            stringsItem.each { item ->
                mb.string('name': item.key, item.value)
            }

            // 2.string-array  <string-array name="skin_title"><item>时尚经典</item></string-array>
            if (arrayItems.size() > 0) {
                mkp.yieldUnescaped('\n')
                mkp.comment("################ string-array ################ ")
                arrayItems.each { a ->
                    mb."string-array"('name': a.key) {
                        a.value.each { b ->
                            mb.item(b)
                        }
                    }
                }
            }

            // 3.不需要翻译的
            if (noTranslateItems.size() > 0) {
                mkp.yieldUnescaped('\n')
                mkp.comment("################ The following lines do not need translation ################ ")
                noTranslateItems.each { no ->
                    mb.string('name': no.key, 'translatable': 'false', no.value)
                }
            }
        }
    }

    /**
     * 获取数据，返回list
     * @param sheet
     * @param col
     * @param filename
     */
    private def getFileData(Sheet sheet, col) {
        def result = []
        def stringsItem = [:]
        def arrayItems = [:]
        def noTranslateItems = [:]

        // 分割标志
        boolean isNoTranslateSeparate = false

        for (int i = 1; i < sheet.getRows(); i++) {                                // 从第2行开始获取数据
            Cell key_cell = sheet.getCell(0, i)
            if (ResStringRecord.NO_TRANSLATE == key_cell.getContents()) {
                isNoTranslateSeparate = true
                continue
            }

            Cell value_cell = sheet.getCell(col, i)
            String key = key_cell.getContents()      // string_key

            // 只有key有合并状态
            Range range = getMergeRange(sheet, key_cell)  //  是否有合并
            if (range != null) {
                int startRow = range.getTopLeft().getRow()
                int endRow = range.getBottomRight().getRow()
                def items = []
                for (c in startRow..endRow) {
                    def cc = sheet.getCell(col, c).getContents()
                    if (cc != null && cc.length() > 0) {
                        items << cc
                    } else {
                        arrayItems.clear()
                        break
                    }
                }

                i += (endRow - startRow)     // 更新i
                if (!items.isEmpty()) {
                    arrayItems << [(key): items]                // string-array
                }
            } else if (isNoTranslateSeparate) {
                String value = value_cell.getContents()          // string_value 不需要翻译的
                if (value != null && value.length() > 0) {
                    noTranslateItems << [(key): value]
                }
            } else {  // 普通
                String value = value_cell.getContents()          // string_value 普通的
                if (value != null && value.length() > 0) {
                    stringsItem << [(key): value]
                }
            }
        }

        result << stringsItem
        result << arrayItems
        result << noTranslateItems

        return result
    }

    /**
     * 判断单元格cell是否在合并单元格中，并返回对应的range
     * @param sheet
     * @param cell
     * @return
     */
    private Range getMergeRange(Sheet sheet, Cell cell) {
        Range result = null
        //获取所有的合并单元格
        Range[] ranges = sheet.getMergedCells()
        for (Range range : ranges) {
            int startRow = range.getTopLeft().getRow();
            int startCol = range.getTopLeft().getColumn();
            int endRow = range.getBottomRight().getRow();
            int endCol = range.getBottomRight().getColumn();
            if (cell.getColumn() <= endCol && cell.getColumn() >= startCol &&
                    cell.getRow() >= startRow && cell.getRow() <= endRow) {
                result = range
                break
            }
        }
        return result
    }
}