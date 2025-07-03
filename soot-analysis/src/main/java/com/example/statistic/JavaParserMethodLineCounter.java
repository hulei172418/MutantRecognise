package com.example.statistic;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.example.util.ExcelUtils.readExcel;

public class JavaParserMethodLineCounter {
    private static String operatorName;
    private static String lineNo;
    private static int count;
    private static String methodName;
    private static String className;
    private static String className_F;
    private static String mutationStatement;
    private static String packageName;
    private static String projectName;
    private static String filepath;
    private static boolean append;
    private static final Set<String> stringSet = new HashSet<>();

    public static void main(String[] args) throws IOException {

        String filePath = "../MutantParse/data/mutant_statistic_1.xlsx";
        List<List<Object>> excelData = readExcelFile(filePath);
        for (int i = 1; i < excelData.size(); i++) {
            operatorName = (String) excelData.get(i).get(0);
            lineNo = (String) excelData.get(i).get(1);
            methodName = (String) excelData.get(i).get(2);
            className = (String) excelData.get(i).get(3);
            className_F = (String) excelData.get(i).get(4);
            mutationStatement = (String) excelData.get(i).get(5);
            packageName = (String) excelData.get(i).get(6);
            projectName = (String) excelData.get(i).get(7);
            filepath = new File((String) excelData.get(i).get(8)).getParent().replace("\\", "/");
            filepath = filepath.replace("//?/", "");
            filepath = Paths.get(filepath).getParent().getParent().getParent().toString()+"/original";
            getLine(filepath+"/"+className_F+".java");
        }

    }

    private static void getLine(String filePath) throws IOException {
        if (stringSet.contains(filePath)) {
            return;
        }
        append = false;
        count = 0;
        stringSet.add(filePath);
        File file = new File(filePath);

        // Java 8 兼容读取文件内容
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append("\n");
        }

        String code = builder.toString();

        // 创建解析器
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(code).getResult().orElse(null);

        if (cu == null) {
            System.out.println("解析失败！");
            return;
        }

        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
            methodName = getMethodSignature(method);
            if (method.getBody().isPresent()) {
                count += (int) method.getBody().get().getStatements().stream()
                        .filter(stmt -> !stmt.isEmptyStmt())  // 忽略空语句
                        .count();
            }
        }
        System.out.println(count + "####"+ className + "####" + className_F + "####"
                + packageName + "####" + projectName + "####" + filepath);
        String outputFilePath = filepath+"/LoC.txt";
        writeToTxt(outputFilePath);
    }

    public static void writeToTxt(String outputFilePath) {
        String line = count + "####" + className + "####" +
                className_F + "####" + packageName + "####" + projectName;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, append))) {
            writer.write(line);
            writer.newLine(); // 换行
        } catch (IOException e) {
            System.err.println("写入失败: " + e.getMessage());
        }
    }

    public static String getMethodSignature(MethodDeclaration method)
    {
        String methodName;
        String temp;
        temp = method.getType().toString();
        methodName = method.getName().toString();
        if(temp.contains("<") && temp.contains(">")){
            temp = temp.substring(0, temp.indexOf("<")) + temp.substring(temp.lastIndexOf(">") + 1, temp.length());
        }
        temp = temp + "_" + methodName;

        StringBuilder str = new StringBuilder(temp + "(");
        NodeList<Parameter> pars = method.getParameters();

        for (int i = 0; i < pars.size(); i++)
        {
            String tempParameter = pars.get(i).getType().toString();
            if ( tempParameter.contains("<") && tempParameter.contains(">")){
                tempParameter = tempParameter.substring(0, tempParameter.indexOf("<")) + tempParameter.substring(tempParameter.lastIndexOf(">") + 1, tempParameter.length());
            }
            str.append(tempParameter);

            if (i != (pars.size()-1))
                str.append(",");
        }
        str.append(")");
        return str.toString();
    }

    private static List<List<Object>> readExcelFile(String filePath) {
        try {
            return readExcel(filePath, 0);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}

