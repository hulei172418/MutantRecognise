package com.example.TCE;

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.*;
import soot.util.Chain;

import java.io.File;
import java.util.*;

public class TCEDetector {

    public static void main(String[] args) {
        String originalClasspath = "target/classes/com/example/demo/origin";
        String mutatedClasspath = "target/classes/com/example/demo/m2";
        String className = "Example";

        TCEDetector detector = new TCEDetector();

        // 分别分析原始类和变异类
        System.out.println("=== Analyzing original class ===");
        detector.setupSoot(originalClasspath, className);
        SootClass originalClass = Scene.v().getSootClass(className);
        Map<String, Body> originalMethods = detector.extractMethodBodies(originalClass);

        System.out.println("\n=== Analyzing mutated class ===");
        detector.setupSoot(mutatedClasspath, className);
        SootClass mutatedClass = Scene.v().getSootClass(className);
        Map<String, Body> mutatedMethods = detector.extractMethodBodies(mutatedClass);

        // 比较方法
        System.out.println("\n=== Comparison Results ===");
        detector.compareMethodBodies(originalMethods, mutatedMethods);
    }

    public void setupSoot(String classpath, String mainClass) {
        G.reset();

        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_soot_classpath(classpath);
        Options.v().set_prepend_classpath(true);  // 确保优先使用指定类路径

        Scene.v().addBasicClass(mainClass, SootClass.BODIES);
        Scene.v().loadNecessaryClasses();
    }

    public static void abstractVariableNames(Body body) {
        int counter = 0;
        Map<Local, Local> varMap = new HashMap<>();

        for (Local local : body.getLocals()) {
            if (local.getName().equals("this")) {
                varMap.put(local, local);
            } else {
                Local newLocal = Jimple.v().newLocal("var" + counter++, local.getType());
                varMap.put(local, newLocal);
            }
        }

        for (Unit unit : body.getUnits()) {
            for (ValueBox box : unit.getUseAndDefBoxes()) {
                Value v = box.getValue();
                if (v instanceof Local) {
                    box.setValue(varMap.get(v));
                }
            }
        }

        Chain<Local> locals = body.getLocals();
        locals.clear();
        locals.addAll(varMap.values());
    }

    public Map<String, Body> extractMethodBodies(SootClass sootClass) {
        Map<String, Body> methodBodies = new HashMap<>();

        System.out.println("Extracting methods from class: " + sootClass.getName());

        for (SootMethod method : sootClass.getMethods()) {
            if (!method.isConcrete()) {
                continue;
            }

            try {
                Body body = method.retrieveActiveBody();
                abstractVariableNames(body);
                methodBodies.put(method.getSubSignature(), body);
                System.out.println("  Loaded method: " + method.getSubSignature());
            } catch (RuntimeException e) {
                System.err.println("  Failed to load body for method: " + method.getSubSignature());
            }
        }

        return methodBodies;
    }

    public void compareMethodBodies(Map<String, Body> originalMethods, Map<String, Body> mutatedMethods) {
        Set<String> allMethodSignatures = new HashSet<>();
        allMethodSignatures.addAll(originalMethods.keySet());
        allMethodSignatures.addAll(mutatedMethods.keySet());

        for (String signature : allMethodSignatures) {
            Body originalBody = originalMethods.get(signature);
            Body mutatedBody = mutatedMethods.get(signature);

            if (originalBody == null) {
                System.out.println("Method added in mutation: " + signature);
                continue;
            }

            if (mutatedBody == null) {
                System.out.println("Method removed in mutation: " + signature);
                continue;
            }

            boolean isEquivalent = isEquivalent(originalBody, mutatedBody);

            System.out.println("Method " + signature + ": " +
                    (isEquivalent ? "EQUIVALENT" : "NOT EQUIVALENT"));

            if (!isEquivalent) {
                printDifferences(originalBody, mutatedBody);
            }
        }
    }

    private SootMethod findMethod(SootClass sootClass, String name, List<Type> paramTypes) {
        for (SootMethod method : sootClass.getMethods()) {
            if (method.getName().equals(name) &&
                    method.getParameterTypes().equals(paramTypes)) {
                return method;
            }
        }
        return null;
    }

    public boolean isEquivalent(Body original, Body mutated) {
        if (!compareLocals(original, mutated)) {
            return false;
        }

        if (!compareCFG(original, mutated)) {
            return false;
        }

        if (!compareStatements(original, mutated)) {
            return false;
        }

        if (!compareTraps(original, mutated)) {
            return false;
        }

        return true;
    }

    private boolean compareLocals(Body body1, Body body2) {
        if (body1.getLocalCount() != body2.getLocalCount()) {
            return false;
        }

        List<Local> locals1 = new ArrayList<>(body1.getLocals());
        List<Local> locals2 = new ArrayList<>(body2.getLocals());
        Set<String> localsStringSet1 = new HashSet<>();
        Set<String> localsStringSet2 = new HashSet<>();

        for (int i = 0; i < locals1.size(); i++) {
            Local local1 = locals1.get(i);
            Local local2 = locals2.get(i);

            localsStringSet1.add(local1.getType().toString());
            localsStringSet2.add(local2.getType().toString());
        }
        return localsStringSet1.equals(localsStringSet2);
    }

    private boolean compareCFG(Body body1, Body body2) {
        UnitGraph cfg1 = new BriefUnitGraph(body1);
        UnitGraph cfg2 = new BriefUnitGraph(body2);

        if (cfg1.size() != cfg2.size()) {
            return false;
        }

        Map<Unit, Integer> unitToId1 = mapUnitsToIds(body1);
        Map<Unit, Integer> unitToId2 = mapUnitsToIds(body2);

        for (Unit u1 : body1.getUnits()) {
            Unit u2 = findCorrespondingUnit(u1, body2);
            if (u2 == null) {
                return false;
            }

            List<Unit> succs1 = cfg1.getSuccsOf(u1);
            List<Unit> succs2 = cfg2.getSuccsOf(u2);

            if (succs1.size() != succs2.size()) {
                return false;
            }

            for (int i = 0; i < succs1.size(); i++) {
                Unit s1 = succs1.get(i);
                Unit s2 = succs2.get(i);

                if (!unitToId1.get(s1).equals(unitToId2.get(s2))) {
                    return false;
                }
            }
        }

        return true;
    }

    private Map<Unit, Integer> mapUnitsToIds(Body body) {
        Map<Unit, Integer> map = new HashMap<>();
        int id = 0;
        for (Unit unit : body.getUnits()) {
            map.put(unit, id++);
        }
        return map;
    }

    private Unit findCorrespondingUnit(Unit source, Body targetBody) {
        String sourceStr = source.toString();
        for (Unit unit : targetBody.getUnits()) {
            if (unit.toString().equals(sourceStr)) {
                return unit;
            }
        }
        return null;
    }

    private boolean compareStatements(Body body1, Body body2) {
        List<Unit> units1 = new ArrayList<>(body1.getUnits());
        List<Unit> units2 = new ArrayList<>(body2.getUnits());

        if (units1.size() != units2.size()) {
            return false;
        }

        for (int i = 0; i < units1.size(); i++) {
            Stmt stmt1 = (Stmt) units1.get(i);
            Stmt stmt2 = (Stmt) units2.get(i);

            if (!compareStatements(stmt1, stmt2)) {
                return false;
            }
        }

        return true;
    }

    private boolean compareStatements(Stmt stmt1, Stmt stmt2) {
        if (!stmt1.getClass().equals(stmt2.getClass())) {
            return false;
        }

        if (stmt1 instanceof AssignStmt) {
            return compareAssignStmts((AssignStmt) stmt1, (AssignStmt) stmt2);
        } else if (stmt1 instanceof InvokeStmt) {
            return compareInvokeStmts((InvokeStmt) stmt1, (InvokeStmt) stmt2);
        } else if (stmt1 instanceof IfStmt) {
            return compareIfStmts((IfStmt) stmt1, (IfStmt) stmt2);
        } else if (stmt1 instanceof GotoStmt) {
            return compareGotoStmts((GotoStmt) stmt1, (GotoStmt) stmt2);
        } else if (stmt1 instanceof ReturnStmt) {
            return compareReturnStmts((ReturnStmt) stmt1, (ReturnStmt) stmt2);
        }

        return stmt1.toString().equals(stmt2.toString());
    }

    private boolean compareAssignStmts(AssignStmt stmt1, AssignStmt stmt2) {
        return stmt1.getLeftOp().toString().equals(stmt2.getLeftOp().toString()) &&
                stmt1.getRightOp().toString().equals(stmt2.getRightOp().toString());
    }

    private boolean compareInvokeStmts(InvokeStmt stmt1, InvokeStmt stmt2) {
        return stmt1.getInvokeExpr().toString().equals(stmt2.getInvokeExpr().toString());
    }

    private boolean compareIfStmts(IfStmt stmt1, IfStmt stmt2) {
        return stmt1.getCondition().toString().equals(stmt2.getCondition().toString()) &&
                stmt1.getTarget().toString().equals(stmt2.getTarget().toString());
    }

    private boolean compareGotoStmts(GotoStmt stmt1, GotoStmt stmt2) {
        return stmt1.getTarget().toString().equals(stmt2.getTarget().toString());
    }

    private boolean compareReturnStmts(ReturnStmt stmt1, ReturnStmt stmt2) {
        if (stmt1.getOp() == null && stmt2.getOp() == null) {
            return true;
        }
        if (stmt1.getOp() == null || stmt2.getOp() == null) {
            return false;
        }
        return stmt1.getOp().toString().equals(stmt2.getOp().toString());
    }

    private boolean compareTraps(Body body1, Body body2) {
        Chain<Trap> traps1 = body1.getTraps();
        Chain<Trap> traps2 = body2.getTraps();

        if (traps1.size() != traps2.size()) {
            return false;
        }

        Iterator<Trap> it1 = traps1.iterator();
        Iterator<Trap> it2 = traps2.iterator();

        while (it1.hasNext() && it2.hasNext()) {
            Trap trap1 = it1.next();
            Trap trap2 = it2.next();

            if (!trap1.getException().getName().equals(trap2.getException().getName())) {
                return false;
            }

            if (!trap1.getBeginUnit().toString().equals(trap2.getBeginUnit().toString())) {
                return false;
            }

            if (!trap1.getEndUnit().toString().equals(trap2.getEndUnit().toString())) {
                return false;
            }

            if (!trap1.getHandlerUnit().toString().equals(trap2.getHandlerUnit().toString())) {
                return false;
            }
        }

        return true;
    }

    private void printDifferences(Body original, Body mutated) {
        System.out.println("=== Differences ===");

        if (original.getLocalCount() != mutated.getLocalCount()) {
            System.out.println("  Local variable count differs: original=" + original.getLocalCount() +
                    ", mutated=" + mutated.getLocalCount());
        }

        List<Unit> originalUnits = new ArrayList<>(original.getUnits());
        List<Unit> mutatedUnits = new ArrayList<>(mutated.getUnits());

        int maxLines = Math.max(originalUnits.size(), mutatedUnits.size());
        for (int i = 0; i < maxLines; i++) {
            if (i >= originalUnits.size()) {
                System.out.println("+ " + mutatedUnits.get(i));
                continue;
            }
            if (i >= mutatedUnits.size()) {
                System.out.println("- " + originalUnits.get(i));
                continue;
            }

            Stmt stmt1 = (Stmt) originalUnits.get(i);
            Stmt stmt2 = (Stmt) mutatedUnits.get(i);

            if (!stmt1.toString().equals(stmt2.toString())) {
                System.out.println("- " + stmt1);
                System.out.println("+ " + stmt2);
            }
        }

        if (original.getTraps().size() != mutated.getTraps().size()) {
            System.out.println("  Exception handler count differs: original=" + original.getTraps().size() +
                    ", mutated=" + mutated.getTraps().size());
        }
    }
}

