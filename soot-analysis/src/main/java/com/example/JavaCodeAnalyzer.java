package com.example;

import com.example.graph.GraphGenerator;
import com.example.graph.SootSetup;
import org.json.JSONArray;
import org.json.JSONObject;
import soot.*;
import soot.jimple.internal.JNopStmt;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.example.util.ExcelUtils.readExcel;
import static com.example.util.MethodSignature.getMethodSignature;

public class JavaCodeAnalyzer {

    private static final Logger logger = Logger.getLogger(JavaCodeAnalyzer.class.getName());
    private static String operatorName;
    private static String lineNo;
    private static String methodName;
    private static String className;
    private static String className_F;
    private static String mutationStatement;
    private static String packageName;
    private static String projectName;
    private static String filepath;
    private static String graphPath;
    private static String JimplePath;
    private static String logType;
    private static final Set<String> stringSet = new HashSet<>();
    private static boolean logAppend;

    public static void main(String[] args) {
        logAppend = false;
        String mutantPath = "../MutantParse/data/mutant_statistic_1.xlsx";
        // test();
        sootMutants(mutantPath);
    }

    private static void sootMutants(String filePath) {
        try {
            String fileName = new File(filePath).getName();
            int dotIndex = fileName.lastIndexOf('.');
            String baseName = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
            String logFile = "logs/" + baseName + ".log";
            setupLogger(logFile);
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
                System.out.print(i +" th ");
                sootOriginalProgram(filepath);
                if (graphNotExist(graphPath)){
                    run();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to set up logger: " + e.getMessage());
        }
    }

    private static void sootOriginalProgram(String filePath) {
        filepath = Paths.get(filePath).getParent().getParent().getParent().toString()+"/original";
        // 防止重复生成
        String curHash = filepath+"/"+methodName;
        if (!stringSet.contains(curHash)) {
            graphPath = filepath + "/" + methodName + "/graph";
            logType = "mutant";
            String absolutePath = new File(filepath+"/"+methodName).
                    getAbsolutePath().replace("\\", "/");;
            System.out.println("Soot for: " + absolutePath);
            if (graphNotExist(graphPath)){
                run();
                stringSet.add(curHash);
            }
        }
        logType = "original";
        filepath = filePath;  // 还原变异体路径
        graphPath = filepath+"/graph";  // 还原graph输出路径
        String absolutePath = new File(filepath).getAbsolutePath().replace("\\", "/");;
        System.out.println("Soot for: " + absolutePath);
    }

    public static boolean graphNotExist(String graphPath) {
        String[] filesToCheck = {"ast.jsonl", "cfg.jsonl", "dfg.jsonl", "manifest.json"};

        boolean allFilesExist = false;

        // 检查每个文件是否存在
        for (String fileName : filesToCheck) {
            Path filePath = Paths.get(graphPath, fileName);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                allFilesExist = true;
                break;
            }
        }
        return allFilesExist;
    }

    private static void test(){
        String excelPath = "../MutantParse/data/mutant_statistic.xlsx";
        List<List<Object>> excelData = readExcelFile(excelPath);
        String fileP = 
        int i = -1;
        for (i = 1; i < excelData.size(); i++) {
            if (fileP.equals(excelData.get(i).get(7)))
                break;
        }
        operatorName = (String) excelData.get(i).get(0);
        lineNo = (String) excelData.get(i).get(1);
        methodName = (String) excelData.get(i).get(2);
        mutationStatement = (String) excelData.get(i).get(3);
        className = (String) excelData.get(i).get(4);
        packageName = (String) excelData.get(i).get(5);
        projectName = (String) excelData.get(i).get(6);
        filepath = new File((String) excelData.get(i).get(7)).getParent();
        run();
    }

    private static List<List<Object>> readExcelFile(String filePath) {
        try {
            return readExcel(filePath, 0);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static void run() {
        try {
            // 初始化 Soot
            SootSetup.setup(filepath);

            // 分析类和方法
            analyzeClass();
        } catch (Exception e) {
            System.err.println("Analysis failed for " + className + "####" + methodName + "####" + e.getMessage());
            logger.log(Level.SEVERE, "Analysis failed for ####"
                    + logType + "####"+ operatorName + "####" + lineNo + "####" + methodName  + "####"
                    + mutationStatement + "####" + className + "####" + className_F + "####"
                    + packageName + "####" + projectName + "####" + filepath + "####"+ e.getMessage());
            e.printStackTrace();
        } finally {
            // 重置 Soot 状态
            G.reset();
        }
    }

    private static void analyzeClass() throws Exception {
        SootClass sootClass = Scene.v().getSootClass(className);
        SootMethod method = null;
        for (SootMethod tmp : sootClass.getMethods()) {
            if (methodName.equals(getMethodSignature(tmp))) {
                method = tmp;
                break;
            }
        }

        assert method != null;

        if (!method.isConcrete()) return;

        Body body = method.retrieveActiveBody();

        Path outputDir = Paths.get(graphPath);
        Files.createDirectories(outputDir);

        // 生成AST图
        GraphGenerator.saveASTGraph(body, outputDir, methodName);

        // 生成CFG图
        GraphGenerator.saveCFGGraph(body, outputDir, methodName);

        // 生成DFG图
        GraphGenerator.saveDFGGraph(body, outputDir, methodName);

        // 生成IR中构建文件
        GraphGenerator.saveIR(body, outputDir);

        // 保存元数据
        saveManifest(sootClass, outputDir);

    }

    private static void saveManifest(SootClass sootClass, Path outputDir) throws IOException {
        JSONObject manifest = new JSONObject()
                .put("class_name", sootClass.getName())
                .put("method_name", methodName)
                .put("mutationStatement", mutationStatement)
                .put("timestamp", System.currentTimeMillis())
                .put("graph_types", new JSONArray().put("AST").put("CFG").put("DFG"));

        Path manifestFile = Paths.get(outputDir.toString(), "manifest.json");
        Files.write(manifestFile, manifest.toString().getBytes());
    }

    private static void setupLogger(String logFile) throws IOException {
        Files.createDirectories(Paths.get("logs"));

        FileHandler fileHandler = new FileHandler(logFile, logAppend);
        fileHandler.setFormatter(new SimpleFormatterWithoutPrefix());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(fileHandler);
        rootLogger.setLevel(Level.SEVERE);

        // 移除默认的控制台输出
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof java.util.logging.ConsoleHandler) {
                rootLogger.removeHandler(handler);
            }
        }

        logger.info("Logger initialized.");
    }

    private static class SimpleFormatterWithoutPrefix extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getLevel() + ": " + record.getMessage() + "\n";
        }
    }
}
