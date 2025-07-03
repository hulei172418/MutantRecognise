package com.example.graph;

import soot.*;
import soot.jimple.*;
import soot.util.dot.DotGraph;
import java.io.*;
import java.util.*;

public class ASTVisualizer {
    private static final int MAX_CODE_LENGTH = 2000;

    public static void visualizeAST(String className, String methodName) {
        SootClass sootClass = Scene.v().getSootClass(className);
        SootMethod method = sootClass.getMethodByName(methodName);
        if (method.isConcrete()) {
            Body body = method.retrieveActiveBody();

            // 生成文本格式的AST
            generateTextAST(body, className, methodName);

            // 生成DOT格式的AST
            generateDotAST(body, className, methodName);
        }
    }

    private static void generateTextAST(Body body, String className, String methodName) {
        String[] parts = className.split("\\.");
        String dirPath = parts[parts.length - 1];
        dirPath = "./demo/"+dirPath;
        File demo_path = new File(dirPath);
        if (!demo_path.exists()) {
            boolean created = demo_path.mkdirs();
            if (!created) {
                System.err.println("Failed to create directory: " + demo_path.getAbsolutePath());
            }
        }
        if (methodName.equals("<init>")) {
            methodName = "init";
        }
        String fileName = dirPath+"/ast_" + methodName + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.println("=== Abstract Syntax Tree for " + className + "." + methodName + " ===");

            // 创建语句索引映射
            Map<Unit, Integer> unitToIndex = new HashMap<>();
            int index = 0;

            for (Unit unit : body.getUnits()) {
                unitToIndex.put(unit, index++);
                out.println("\nStatement " + unitToIndex.get(unit) + ":");
                printStmtStructure(unit, out, 1);
            }

            System.out.println("Text format AST saved to " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printStmtStructure(Unit unit, PrintWriter out, int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        String indentStr = sb.toString();

        if (unit instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) unit;
            out.println(indentStr + "IfStmt:");
            out.println(indentStr + "  Condition:");
            printValueStructure(ifStmt.getCondition(), out, indent + 2);
        }
        else if (unit instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) unit;
            out.println(indentStr + "AssignStmt:");
            out.println(indentStr + "  LeftOp:");
            printValueStructure(assign.getLeftOp(), out, indent + 2);
            out.println(indentStr + "  RightOp:");
            printValueStructure(assign.getRightOp(), out, indent + 2);
        }
        // 其他语句类型的处理...
        else {
            out.println(indentStr + unit.getClass().getSimpleName() + ": " + unit);
        }
    }

    private static void printValueStructure(Value value, PrintWriter out, int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        String indentStr = sb.toString();

        if (value instanceof BinopExpr) {
            BinopExpr binop = (BinopExpr) value;
            out.println(indentStr + binop.getSymbol() + " Operation:");
            out.println(indentStr + "  Left:");
            printValueStructure(binop.getOp1(), out, indent + 2);
            out.println(indentStr + "  Right:");
            printValueStructure(binop.getOp2(), out, indent + 2);
        }
        else if (value instanceof Local) {
            out.println(indentStr + "Local: " + value);
        }
        else if (value instanceof Constant) {
            out.println(indentStr + "Constant: " + value);
        }
        // 其他值类型的处理...
        else {
            out.println(indentStr + value.getClass().getSimpleName() + ": " + value);
        }
    }

    public static void generateDotAST1(Body body, String className, String methodName) {
        DotGraph dotGraph = new DotGraph("AST_" + className + "_" + methodName);
        dotGraph.setNodeShape("box");
        dotGraph.setGraphLabel("Abstract Syntax Tree for " + className + "." + methodName);

        Map<Object, Integer> nodeIds = new HashMap<>();
        int nextId = 0;

        // 添加所有 Unit 节点
        for (Unit unit : body.getUnits()) {
            if (!nodeIds.containsKey(unit)) {
                nodeIds.put(unit, nextId);
                String label = nextId + ": [Unit] " + unit.getClass().getSimpleName() + "\\n" + truncateCode(unit.toString());
                dotGraph.drawNode(String.valueOf(nextId)).setLabel(label);
                nextId++;
            }
        }

        // 添加 Value 节点和边
        for (Unit unit : body.getUnits()) {
            int unitId = nodeIds.get(unit);

            for (ValueBox box : unit.getUseAndDefBoxes()) {
                Value value = box.getValue();

                if (!nodeIds.containsKey(value)) {
                    nodeIds.put(value, nextId);
                    String label = nextId + ": [Value] " + value.getClass().getSimpleName() + "\\n" + truncateCode(value.toString());
                    dotGraph.drawNode(String.valueOf(nextId)).setLabel(label);
                    nextId++;
                }

                int valueId = nodeIds.get(value);
                dotGraph.drawEdge(String.valueOf(unitId), String.valueOf(valueId)).setLabel("PARENT_OF");
            }
        }

        // 方法名处理
        if (methodName.equals("<init>")) {
            methodName = "init";
        }

        // 输出路径处理
        String[] parts = className.split("\\.");
        String dirName = parts[parts.length - 1];
        String dirPath = "./demo/" + dirName;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = dirPath + "/ast_" + methodName + ".dot";
        dotGraph.plot(fileName);
        System.out.println("DOT format AST saved to " + fileName);
    }

    private static void generateDotAST(Body body, String className, String methodName) {
        DotGraph dotGraph = new DotGraph("AST_" + className + "_" + methodName);
        dotGraph.setNodeShape("box");
        dotGraph.setGraphLabel("Abstract Syntax Tree for " + className + "." + methodName);

        // 创建语句索引映射
        Map<Unit, Integer> unitToIndex = new HashMap<>();
        int index = 0;

        // 第一遍：添加所有节点
        for (Unit unit : body.getUnits()) {
            unitToIndex.put(unit, index);
            String nodeLabel = index + ": " + unit.getClass().getSimpleName();
            dotGraph.drawNode(String.valueOf(index)).setLabel(nodeLabel);
            index++;
        }

        // 第二遍：添加结构关系
        index = 0;
        for (Unit unit : body.getUnits()) {
            if (unit instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) unit;
                addExprStructure(dotGraph, String.valueOf(index), "Condition", ifStmt.getCondition());
            }
            else if (unit instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) unit;
                addExprStructure(dotGraph, String.valueOf(index), "LHS", assign.getLeftOp());
                addExprStructure(dotGraph, String.valueOf(index), "RHS", assign.getRightOp());
            }
            index++;
        }

        // 保存到文件
        if (methodName.equals("<init>")) {
            methodName = "init";
        }
        String[] parts = className.split("\\.");
        String dirPath = parts[parts.length - 1];
        dirPath = "./demo/"+dirPath;
        String fileName = dirPath+"/ast_" + methodName + ".dot";
        dotGraph.plot(fileName);
        System.out.println("DOT format AST saved to " + fileName);
    }

    private static void addExprStructure(DotGraph dotGraph, String parentId, String relation, Value value) {
        String nodeId = parentId + "_" + relation;

        if (value instanceof BinopExpr) {
            BinopExpr binop = (BinopExpr) value;
            dotGraph.drawNode(nodeId).setLabel(binop.getSymbol());
            dotGraph.drawEdge(parentId, nodeId);

            addExprStructure(dotGraph, nodeId, "Left", binop.getOp1());
            addExprStructure(dotGraph, nodeId, "Right", binop.getOp2());
        }
        else {
            dotGraph.drawNode(nodeId).setLabel(value.toString());
            dotGraph.drawEdge(parentId, nodeId);
        }
    }

    private static String truncateCode(String code) {
        return code.length() > MAX_CODE_LENGTH
                ? code.substring(0, MAX_CODE_LENGTH)
                : code;
    }
}