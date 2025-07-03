package com.example.util;

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.util.Chain;

import java.util.*;

public class VariableRenamer extends BodyTransformer {

    private static VariableRenamer instance = new VariableRenamer();

    public static VariableRenamer v() {
        return instance;
    }

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        Chain<Local> locals = body.getLocals();
        int index = 0;
        Map<Local, String> renameMap = new HashMap<>();

        // 为每个变量生成新名字
        for (Local local : locals) {
            String newName = "v" + index++;
            renameMap.put(local, newName);
        }

        // 执行重命名
        for (Local local : renameMap.keySet()) {
            local.setName(renameMap.get(local));
        }
    }

    // 注册执行器（main方法中调用）
    public static void register() {
        PackManager.v().getPack("jb").add(new Transform("jb.rename", VariableRenamer.v()));
    }
}

