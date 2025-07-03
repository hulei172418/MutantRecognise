package com.example.graph;

import org.json.JSONArray;
import org.json.JSONObject;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GraphGenerator {
    private static final int MAX_CODE_LENGTH = 2000;

    public static void saveASTGraph(Body body, Path outputDir, String methodName) throws IOException {
        Path astFile = outputDir.resolve("ast.jsonl");
        try (BufferedWriter writer = Files.newBufferedWriter(astFile)) {
            // AST节点映射
            Map<Object, Integer> nodeIds = new HashMap<>(); // 使用Object作为键类型
            int nextId = 0;

            // 遍历所有语句
            for (Unit unit : body.getUnits()) {
                // 确保Unit节点存在
                if (!nodeIds.containsKey(unit)) {
                    nodeIds.put(unit, nextId++);
                    JSONObject node = new JSONObject()
                            .put("type", "AST_NODE")
                            .put("node_type", unit.getClass().getSimpleName())
                            .put("code", truncateCode(unit.toString()))
                            //.put("line", unit.getJavaSourceStartLineNumber())
                            .put("id", nodeIds.get(unit));
                    writer.write(node.toString());
                    writer.newLine();
                }

                // 处理AST子节点（操作数）
                for (ValueBox box : unit.getUseAndDefBoxes()) {
                    Value value = box.getValue();

                    // 确保Value节点存在
                    if (!nodeIds.containsKey(value)) {
                        nodeIds.put(value, nextId++);
                        JSONObject childNode = new JSONObject()
                                .put("type", "AST_NODE")
                                .put("node_type", value.getClass().getSimpleName())
                                .put("code", truncateCode(value.toString()))
                                .put("id", nodeIds.get(value));
                        writer.write(childNode.toString());
                        writer.newLine();
                    }

                    // 添加边（添加前再次验证节点存在）
                    if (nodeIds.containsKey(unit) && nodeIds.containsKey(value)) {
                        JSONObject edge = new JSONObject()
                                .put("type", "AST_EDGE")
                                .put("edge_type", "PARENT_OF")
                                .put("source", nodeIds.get(unit))
                                .put("target", nodeIds.get(value));
                        writer.write(edge.toString());
                        writer.newLine();
                    } else {
                        System.err.println("WARNING: Missing node for edge - unit: " +
                                nodeIds.containsKey(unit) + ", value: " + nodeIds.containsKey(value));
                    }
                }
            }
        }
    }

    public static void saveCFGGraph(Body body, Path outputDir, String methodName) throws IOException {
        Path cfgFile = outputDir.resolve("cfg.jsonl");
        DirectedGraph<Unit> cfg = new BriefUnitGraph(body);

        try (BufferedWriter writer = Files.newBufferedWriter(cfgFile)) {
            // 节点ID映射
            Map<Unit, Integer> nodeIds = new HashMap<>();
            int nextId = 0;

            // 添加所有基本块节点
            for (Unit unit : cfg) {
                nodeIds.put(unit, nextId++);
                JSONObject node = new JSONObject()
                        .put("type", "CFG_NODE")
                        .put("node_type", "BASIC_BLOCK")
                        .put("code", truncateCode(unit.toString()))
                        //.put("line", unit.getJavaSourceStartLineNumber())
                        .put("id", nodeIds.get(unit));
                writer.write(node.toString());
                writer.newLine();
            }

            // 添加控制流边
            for (Unit src : cfg) {
                for (Unit dst : cfg.getSuccsOf(src)) {
                    JSONObject edge = new JSONObject()
                            .put("type", "CFG_EDGE")
                            .put("edge_type", "CONTROL_FLOW")
                            .put("source", nodeIds.get(src))
                            .put("target", nodeIds.get(dst));
                    writer.write(edge.toString());
                    writer.newLine();
                }
            }
        }
    }

    public static void saveDFGGraph(Body body, Path outputDir, String methodName) throws IOException {
        Path dfgFile = outputDir.resolve("dfg.jsonl");
        UnitGraph cfg = new BriefUnitGraph(body);

        try (BufferedWriter writer = Files.newBufferedWriter(dfgFile)) {
            // 1. 建立定义-使用链和节点映射
            Map<Value, Set<Unit>> defSites = new HashMap<>();  // 值 -> 定义该值的语句集合
            Map<Unit, Integer> unitToId = new HashMap<>();     // 语句 -> 节点ID
            Map<Integer, String> nodeLabels = new HashMap<>(); // 节点ID -> 代码文本
            int nextId = 0;

            // 第一遍扫描：收集定义点并分配节点ID
            for (Unit unit : body.getUnits()) {
                unitToId.put(unit, nextId);
                nodeLabels.put(nextId, truncateCode(unit.toString()));
                nextId++;

                // 记录变量定义点
                for (ValueBox defBox : unit.getDefBoxes()) {
                    Value value = defBox.getValue();
                    if (shouldIncludeInDFG(value)) {
                        defSites.computeIfAbsent(value, k -> new HashSet<>()).add(unit);
                    }
                }
            }

            // 2. 写入所有节点
            for (int i = 0; i < nextId; i++) {
                JSONObject node = new JSONObject()
                        .put("type", "DFG_NODE")
                        .put("node_type", "UNIT")
                        .put("code", nodeLabels.get(i))
                        .put("id", i);
                writer.write(node.toString());
                writer.newLine();
            }

            // 3. 写入数据流边（基于定义-使用链）
            Set<String> recordedEdges = new HashSet<>(); // 用于边去重
            for (Unit useUnit : body.getUnits()) {
                int targetId = unitToId.get(useUnit);

                // 处理该语句使用的变量
                for (ValueBox useBox : useUnit.getUseBoxes()) {
                    Value value = useBox.getValue();
                    if (!shouldIncludeInDFG(value) || !defSites.containsKey(value)) {
                        continue;
                    }

                    // 遍历该变量的所有定义点
                    for (Unit defUnit : defSites.get(value)) {
                        int sourceId = unitToId.get(defUnit);

                        //// 跳过自循环边（可选）
                        //if (sourceId == targetId) continue;

                        // 边去重
                        String edgeKey = sourceId + "->" + targetId + ":" + value;
                        if (!recordedEdges.contains(edgeKey)) {
                            JSONObject edge = new JSONObject()
                                    .put("type", "DFG_EDGE")
                                    .put("edge_type", "DATA_FLOW")
                                    .put("source", sourceId)
                                    .put("target", targetId)
                                    .put("value", value.toString());
                            writer.write(edge.toString());
                            writer.newLine();
                            recordedEdges.add(edgeKey);
                        }
                    }
                }
            }
        }
    }

    public static void saveIR(Body body, Path outputDir) throws IOException{
        Path irFile = outputDir.resolve("IR.jimple");
        try (BufferedWriter writer = Files.newBufferedWriter(irFile)) {
            writer.write(body.toString());
        }
    }

    // 辅助方法：过滤无效节点
    private static boolean shouldIncludeInDFG(Value value) {
        // 排除常量、this引用等
        return !(value instanceof Constant) &&
                !value.toString().equals("this") &&
                !value.toString().isEmpty();
    }

    // 辅助方法：获取值的类型分类
    private static String getValueType(Value value) {
        if (value instanceof Local) return "local";
        if (value instanceof FieldRef) return "field";
        if (value instanceof ArrayRef) return "array";
        return "other";
    }

    private static void saveManifest(SootClass sootClass, String originalPath) throws IOException {
        JSONObject manifest = new JSONObject()
                .put("original_file", originalPath)
                .put("class_name", sootClass.getName())
                .put("timestamp", System.currentTimeMillis())
                .put("graph_types", new JSONArray()
                        .put("AST")
                        .put("CFG")
                        .put("DFG"));

        Path manifestFile = Paths.get(originalPath, "manifest.json");
        Files.write(manifestFile, manifest.toString().getBytes());
    }

    private static String truncateCode(String code) {
        return code.length() > MAX_CODE_LENGTH
                ? code.substring(0, MAX_CODE_LENGTH)
                : code;
    }
}