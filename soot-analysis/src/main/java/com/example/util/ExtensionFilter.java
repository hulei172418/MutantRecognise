package com.example.util;

import java.io.FilenameFilter;
import java.io.File;


public class ExtensionFilter implements FilenameFilter {
  private String mask;

  public ExtensionFilter() {
  }

  public ExtensionFilter(String str) {
    this.mask = str;
  }

  @Override
  public boolean accept(File dir, String name) {
    // 忽略带 $ 的类（通常是内部类），你保留了这个逻辑
    if (name.contains("$")) return false;

    if (mask != null) {
      int index = name.lastIndexOf(".");
      // ✅ 确保有扩展名并且不是最后一位
      if (index > 0 && index < name.length() - 1) {
        String ext = name.substring(index + 1);
        return ext.equals(mask);
      } else {
        return false; // 没有扩展名，直接过滤掉
      }
    }

    // 如果没设置 mask，就不过滤
    return true;
  }
}

