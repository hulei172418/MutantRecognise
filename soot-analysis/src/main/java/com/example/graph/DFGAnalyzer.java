package com.example.graph;

import soot.*;
import soot.toolkits.scalar.*;
import soot.toolkits.graph.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import soot.util.dot.DotGraph;


public class DFGAnalyzer {
    public static void analyzeDFG(String className, String methodName) {
        SootClass sootClass = Scene.v().getSootClass(className);
        SootMethod method = sootClass.getMethodByName(methodName);

        if (method.isConcrete()) {
            Body body = method.retrieveActiveBody();
            UnitGraph cfg = new BriefUnitGraph(body);

            // 生成文本格式的DFG
            generateTextDFG(body, className, methodName);

            // 生成DOT格式的DFG
            generateDotDFG(body, className, methodName);

//            // 打印数据流分析信息
//            printDataFlowInfo(body, cfg);
        }
    }

    private static void generateTextDFG(Body body, String className, String methodName) {
        if (methodName.equals("<init>")) {
            methodName = "init";
        }
        String[] parts = className.split("\\.");
        String dirPath = parts[parts.length - 1];
        dirPath = "./demo/"+dirPath;
        String fileName = dirPath+"/dfg_" + methodName + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            // 1. 收集定义-使用链
            Map<Value, Set<Unit>> defSites = new HashMap<>();
            Map<Value, Set<Unit>> useSites = new HashMap<>();

            // 创建语句索引映射
            Map<Unit, Integer> unitToIndex = new HashMap<>();
            int index = 0;
            for (Unit unit : body.getUnits()) {
                unitToIndex.put(unit, index++);

                // 记录定义
                for (ValueBox defBox : unit.getDefBoxes()) {
                    Value value = defBox.getValue();
                    defSites.computeIfAbsent(value, k -> new HashSet<>()).add(unit);
                }

                // 记录使用
                for (ValueBox useBox : unit.getUseBoxes()) {
                    Value value = useBox.getValue();
                    useSites.computeIfAbsent(value, k -> new HashSet<>()).add(unit);
                }
            }

            // 2. 输出数据依赖关系
            out.println("=== Data Dependencies ===");
            for (Unit unit : body.getUnits()) {
                out.println("\nStatement " + unitToIndex.get(unit) + ": " + unit);

                // 输出使用的变量及其定义位置
                for (ValueBox useBox : unit.getUseBoxes()) {
                    Value value = useBox.getValue();
                    if (defSites.containsKey(value)) {
                        out.println("  Uses: " + value);
                        for (Unit defUnit : defSites.get(value)) {
                            out.println("    Defined at: " + unitToIndex.get(defUnit) + ": " + defUnit);
                        }
                    }
                }
            }

            System.out.println("Text format DFG saved to " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateDotDFG(Body body, String className, String methodName) {
        DotGraph dotGraph = new DotGraph("DFG_" + className + "_" + methodName);
        dotGraph.setNodeShape("box");
        dotGraph.setGraphLabel("Data Flow Graph for " + className + "." + methodName);

        // 1. 收集定义-使用链
        Map<Value, Set<Unit>> defSites = new HashMap<>();
        Map<Unit, Integer> unitToIndex = new HashMap<>();

        int index = 0;
        for (Unit unit : body.getUnits()) {
            unitToIndex.put(unit, index++);

            // 记录定义
            for (ValueBox defBox : unit.getDefBoxes()) {
                Value value = defBox.getValue();
                defSites.computeIfAbsent(value, k -> new HashSet<>()).add(unit);
            }
        }

        // 2. 添加节点
        for (Unit unit : body.getUnits()) {
            String nodeLabel = unitToIndex.get(unit) + ": " + unit.toString();
            dotGraph.drawNode(nodeLabel).setLabel(nodeLabel);
        }

        // 3. 添加数据依赖边
        for (Unit unit : body.getUnits()) {
            String targetLabel = unitToIndex.get(unit) + ": " + unit.toString();

            for (ValueBox useBox : unit.getUseBoxes()) {
                Value value = useBox.getValue();
                if (defSites.containsKey(value)) {
                    for (Unit defUnit : defSites.get(value)) {
                        String sourceLabel = unitToIndex.get(defUnit) + ": " + defUnit.toString();
                        dotGraph.drawEdge(sourceLabel, targetLabel).setLabel(value.toString());
                    }
                }
            }
        }

        // 4. 保存到文件
        if (methodName.equals("<init>")) {
            methodName = "init";
        }
        String[] parts = className.split("\\.");
        String dirPath = parts[parts.length - 1];
        dirPath = "./demo/"+dirPath;
        String fileName = dirPath+"/dfg_" + methodName + ".dot";
        dotGraph.plot(fileName);
        System.out.println("DOT format DFG saved to " + fileName);
    }

    private static void printDataFlowInfo(Body body, UnitGraph cfg) {
        System.out.println("\n=== Data Flow Analysis Details ===");

        // 活跃变量分析
        SimpleLiveLocals liveLocals = new SimpleLiveLocals(cfg);

        // 创建语句索引映射
        Map<Unit, Integer> unitToIndex = new HashMap<>();
        int index = 0;
        for (Unit unit : body.getUnits()) {
            unitToIndex.put(unit, index++);
        }

        for (Unit unit : body.getUnits()) {
            System.out.println("\nStatement " + unitToIndex.get(unit) + ": " + unit);

            // 定义和使用的变量
            System.out.println("Defined variables:");
            for (ValueBox defBox : unit.getDefBoxes()) {
                System.out.println("  " + defBox.getValue());
            }

            System.out.println("Used variables:");
            for (ValueBox useBox : unit.getUseBoxes()) {
                System.out.println("  " + useBox.getValue());
            }

            // 活跃变量
            System.out.println("Live variables before:");
            for (Local local : liveLocals.getLiveLocalsBefore(unit)) {
                System.out.println("  " + local);
            }
        }
    }
}