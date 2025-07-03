package com.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtils {

    /**
     * 读取 Excel 文件，返回一个二维 List 形式的数据
     * @param filePath Excel 文件路径
     * @param sheetIndex 需要读取的 sheet 索引（从 0 开始）
     * @return 包含 Excel 数据的二维 List
     * @throws IOException 如果文件读取失败
     */
    public static List<List<Object>> readExcel(String filePath, int sheetIndex) throws IOException {
        List<List<Object>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            for (Row row : sheet) {
                List<Object> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            // 处理日期格式
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getDateCellValue());
                            } else {
                                rowData.add(cell.getNumericCellValue());
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(cell.getBooleanCellValue());
                            break;
                        case FORMULA:
                            rowData.add(cell.getCellFormula());
                            break;
                        case BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add("UNKNOWN");
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }

    public static void main(String[] args) {
        String filePath = "../MutantParse/data/mutant_statistic.xlsx";
        try {
            List<List<Object>> excelData = readExcel(filePath, 0);
            for (List<Object> row : excelData) {
                System.out.println(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


