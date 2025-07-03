package com.example.graph;

import soot.*;
import soot.toolkits.graph.*;
import soot.jimple.*;
import soot.util.dot.DotGraph;

import java.io.*;
import java.util.*;

public class CFGVisualizer {
    public static void visualizeCFG(String className, String methodName) {
        SootClass sootClass = Scene.v().getSootClass(className);
        SootMethod method = sootClass.getMethodByName(methodName);

        if (method.isConcrete()) {
            Body body = method.retrieveActiveBody();
            UnitGraph cfg = new BriefUnitGraph(body);

            // 生成简单文本格式的CFG
            generateTextCFG(cfg, className, methodName);

            // 生成DOT格式的CFG
            generateDotCFG(cfg, className, methodName);
        }
    }

    private static void generateTextCFG(UnitGraph cfg, String className, String methodName) {
        if (methodName.equals("<init>")) {
            methodName = "init";
        }
        String[] parts = className.split("\\.");
        String dirPath = parts[parts.length - 1];
        dirPath = "./demo/"+dirPath;
        String fileName = dirPath+"/cfg_" + methodName + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            // 记录已处理的边，避免重复
            Set<String> processedEdges = new HashSet<>();

            // 添加开始节点
            if (!cfg.getHeads().isEmpty()) {
                Unit firstUnit = cfg.getHeads().get(0);
                out.println("[start] -> [" + firstUnit + "]");
            }

            // 遍历所有基本块
            for (Unit unit : cfg) {
                // 获取当前语句的所有后继
                List<Unit> successors = cfg.getSuccsOf(unit);

                // 如果有后继，则输出边
                if (!successors.isEmpty()) {
                    for (Unit succ : successors) {
                        String edgeKey = unit + "->" + succ;
                        if (!processedEdges.contains(edgeKey)) {
                            // 获取边的类型（条件分支会有true/false标签）
                            String edgeLabel = "";
                            if (unit instanceof IfStmt) {
                                // 新版本的Soot中获取目标地址的方式
                                IfStmt ifStmt = (IfStmt) unit;
                                // 获取条件语句的默认后继（false分支）
                                Unit defaultTarget = cfg.getSuccsOf(ifStmt).get(0);
                                // 获取跳转目标（true分支）
                                Stmt targetStmt = (Stmt) ifStmt.getTarget();

                                edgeLabel = (succ == targetStmt) ? " (true)" : " (false)";
                            } else if (unit instanceof GotoStmt) {
                                edgeLabel = " (goto)";
                            }

                            out.println("[" + unit + "] -> [" + succ + "]" + edgeLabel);
                            processedEdges.add(edgeKey);
                        }
                    }
                } else {
                    // 没有后继的节点（通常是return语句）
                    out.println("[" + unit + "] -> [end]");
                }
            }

            System.out.println("Text format CFG saved to " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateDotCFG(UnitGraph cfg, String className, String methodName) {
        DotGraph dotGraph = new DotGraph("CFG_" + className + "_" + methodName);

        // 添加节点
        for (Unit unit : cfg) {
            dotGraph.drawNode(unit.toString());
        }

        // 添加边
        for (Unit unit : cfg) {
            for (Unit succ : cfg.getSuccsOf(unit)) {
                String edgeLabel = "";
                if (unit instanceof IfStmt) {
                    IfStmt ifStmt = (IfStmt) unit;
                    Stmt targetStmt = (Stmt) ifStmt.getTarget();
                    edgeLabel = (succ == targetStmt) ? "true" : "false";
                }
                dotGraph.drawEdge(unit.toString(), succ.toString()).setLabel(edgeLabel);
            }
        }

        // 保存到文件
        if (methodName.equals("<init>")) {
            methodName = "init";
        }
        String[] parts = className.split("\\.");
        String dirPath = parts[parts.length - 1];
        dirPath = "./demo/"+dirPath;
        String fileName = dirPath+"/cfg_" + methodName + ".dot";
        dotGraph.plot(fileName);
        System.out.println("DOT format CFG saved to " + fileName);
    }
}