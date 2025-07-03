

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Test {


    public static void main(String[] args) {
        // 测试路径字符串
        String[] paths = {
                "test",
                "./test/",
                "/test/.",
                "test/",
                "/test",
                "./test/file.txt",
                "../test/abc/def",
                "C:/test/abc",
                "test/java/org/joda/time/MockZone.java",
                "/usr/local/test/abc",
                "something/test/another",
                "/abc/def/test",
                "test123",
                "/test123/",
                "not_a_test_path"
        };

        // 正则表达式
        String regex = "(^|/|\\.\\.|\\./)test(/|$|\\.)";

        // 逐个测试
        Pattern pattern = Pattern.compile(regex);
        for (String path : paths) {
            System.out.print(path);
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                System.out.print("  匹配到路径: " + path);
            }
            System.out.println("");
        }
    }
}

