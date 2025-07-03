package com.example.graph;
import com.example.util.VariableRenamer;
import soot.G;
import soot.Scene;
import soot.options.Options;
import soot.PhaseOptions;

import java.io.File;
import java.util.Collections;

public class SootSetup {
    public static void setup(String absolutePath) {
        G.reset();

        // 设置 Soot 选项
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_process_dir(Collections.singletonList(absolutePath));
        Options.v().set_soot_classpath(absolutePath + File.pathSeparator + System.getProperty("java.class.path"));

        Options.v().setPhaseOption("jb", "use-original-names:true");  // 禁用原始名更保险

        // 加载必要的类
        Scene.v().loadNecessaryClasses();

        VariableRenamer.register();

    }
}
