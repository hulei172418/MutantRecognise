package com.example;

import com.example.graph.ASTVisualizer;
import com.example.graph.CFGVisualizer;
import com.example.graph.DFGAnalyzer;
import soot.*;
import soot.options.Options;
import java.util.*;

public class SootAnalysis {
    public static void main(String[] args) {
        // 设置Soot的classpath
        String classpath = System.getProperty("java.class.path");

        // 配置Soot选项
        Options.v().set_soot_classpath(classpath);
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_output_format(Options.output_format_jimple);

        // 设置要分析的类
//        List<String> classes = new ArrayList<>();
//        classes.add("com.example.demo.Triangle");
        Options.v().set_process_dir(Collections.singletonList("./target/classes"));

        // 加载并分析类
        Scene.v().loadNecessaryClasses();

        // 进行AST分析 com/example/demo/ConstantPoolEntry/m1/ConstantPoolEntry.class
        ASTVisualizer.visualizeAST("com.example.demo.ConstantPoolEntry.m2.ConstantPoolEntry", "PoolEntry");

        // 进行CFG分析
        CFGVisualizer.visualizeCFG("com.example.demo.ConstantPoolEntry.m2.ConstantPoolEntry", "PoolEntry");

        // 进行DFG分析
        DFGAnalyzer.analyzeDFG("com.example.demo.ConstantPoolEntry.m2.ConstantPoolEntry", "PoolEntry");
    }
}