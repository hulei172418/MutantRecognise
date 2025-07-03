package com.example.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvReaderUtil {

    /**
     * 读取 CSV 文件，返回 List<List<String>> 结构
     *
     * @param filePath CSV 文件路径
     * @return 所有行的数据列表
     * @throws IOException 读取异常
     */
    public static List<List<String>> readCsv(String filePath)
            throws IOException, CsvValidationException {
        List<List<String>> data = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                List<String> row = new ArrayList<>(Arrays.asList(line));
                data.add(row);
            }
        }

        return data;
    }

}

