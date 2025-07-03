package com.example.statistic;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.statistic.LocalJarFinder.getFileDir;

public class MethodLineCounter {
    public static String SYSTEM_HOME = System.getProperty("user.dir");
    public static String SRC_PATH = SYSTEM_HOME + "/src";

    private static String operatorName;
    private static String lineNo;
    private static int count;
    private static String methodName;
    private static String className;
    private static String className_F;
    private static String mutationStatement;
    private static String packageName;
    private static String projectName;
    private static boolean append;
    private static final Set<String> stringSet = new HashSet<>();

    public static void main(String[] args) {
        append = false;
        String s = null;
        try {
            String maven = System.getProperty("user.dir") + "/mujava.config";
            String jsonResult = getFileDir(maven);
            assert jsonResult != null;

            JSONArray jsonArray = new JSONArray(jsonResult);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject dependency = jsonArray.getJSONObject(i);
                String groupId = dependency.getString("groupId");
                String artifactId = dependency.getString("artifactId");
                String version = dependency.getString("version");
                String jarPath = dependency.optString("jarPath", "[未找到]");

                if(artifactId.contains("commons-math4")){
                    s = "commons-math"+"-"+version+"/"+artifactId.replace("math4", "math");
                }else if (artifactId.contains("commons-numbers")){
                    s = "commons-numbers"+"-"+version+"/"+artifactId;
                }
                else {
                    s = artifactId+"-"+version;
                }
                projectName = s;
                SYSTEM_HOME = System.getProperty("user.dir") + "/../testset/"+s;
                SRC_PATH = SYSTEM_HOME + "/src";
                getJavaFile();
            }
        }catch (Exception e){
            System.out.println("[Error] " + s + e.toString());
        }
    }

    public static void writeToTxt(String outputFilePath) {
        String line = count + "####" + className_F + "####" + projectName;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, append))) {
            writer.write(line);
            writer.newLine(); // 换行
        } catch (IOException e) {
            System.err.println("写入失败: " + e.getMessage());
        }
    }

    public static void getJavaFile() throws IOException {
        Vector file_names = getNewTragetFiles();

        for (int i=0;i< file_names.size();i++) {
            String file_name = (String) file_names.get(i);

            // 统一路径分隔符为正斜杠
            file_name = file_name.replace("\\", "/");

            String regex = "(^|/|\\.\\.|\\./)test(/|$|\\.)"; // 过滤掉 test 目录下的.java文件
            Pattern pattern = Pattern.compile(regex);
            // 如果路径中包含 "/test/"，则过滤掉
            Matcher matcher = pattern.matcher(file_name);
            if (matcher.find()) {
                continue;
            }
            getLine(SRC_PATH+"/"+file_name);
        }
    }

    private static void getLine(String filePath) throws IOException {
        if (stringSet.contains(filePath)) {
            return;
        }
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
            if (method.getBody().isPresent()) {
                count += (int) method.getBody().get().getStatements().stream()
                        .filter(stmt -> !stmt.isEmptyStmt())  // 忽略空语句
                        .count();
            }
        }

        Path path = Paths.get(filePath).normalize();
        // 类文件名
        String baseName = path.getFileName().toString(); // Task1.java

        // 类名
        className_F = baseName.substring(0, baseName.lastIndexOf('.'));
        System.out.println(count + "####"+ className_F + "####" + projectName);
        String outputFilePath = "../MutantParse/data"+"/LoC.txt";
        writeToTxt(outputFilePath);
        append = true;
    }

    public static Vector getNewTragetFiles()
    {
        Vector targetFiles = new Vector();
        getJavacArgForDir (SRC_PATH, "", targetFiles);
        return targetFiles;
    }

    protected static String getJavacArgForDir (String dir, String str, Vector targetFiles)
    {
        String result = str;
        String temp = "";

        File dirF = new File(dir);
        File[] javaF = dirF.listFiles (new ExtensionFilter("java"));
        if (javaF.length > 0)
        {
            result = result + dir + "/*.java ";

            for (int k=0; k<javaF.length; k++)
            {
                temp = javaF[k].getAbsolutePath();
                temp.replace('\\', '/');
                targetFiles.add(temp.substring(SRC_PATH.length()+1, temp.length()));
            }
        }

        File[] sub_dir = dirF.listFiles (new DirFileFilter());
        if (sub_dir.length == 0)    return result;

        for (int i=0; i<sub_dir.length; i++)
        {
            result = getJavacArgForDir(sub_dir[i].getAbsolutePath(), result, targetFiles);
        }
        return result;
    }
}
