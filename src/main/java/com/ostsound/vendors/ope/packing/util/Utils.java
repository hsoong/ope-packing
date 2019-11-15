package com.ostsound.vendors.ope.packing.util;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    public static String workDir() {
        String path = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (path.contains(File.pathSeparator)) {
            path = path.split(File.pathSeparator)[1];
        }

        if (path.contains(".jar")) {
            int idx = path.indexOf(".jar");
            path = path.substring(0, idx);

            idx = path.lastIndexOf(File.separator);
            path = path.substring(0, idx);
        }
        return path;
    }

    public static String padding(String string, int length, boolean left) {
        StringBuffer buffer = new StringBuffer();
        int len = length - string.length();
        if (left) {
            while (len-- > 0) {
                buffer.append("    ");
            }
            buffer.append(string);
        } else {
            buffer.append(string);
            while (len-- > 0) {
                buffer.append("    ");
            }
        }
        return buffer.toString();
    }

    public static String writeFile(String content, String filename) {
        String workDir = Utils.workDir();
        System.out.println("workDir: " + workDir);

        Path path = Paths.get(workDir).resolve(filename).toAbsolutePath();
        try {
            System.out.println("filename: " + filename);
            System.out.println("path: " + path.toString());
            System.out.println("absolute path: " + path.toAbsolutePath().toString());

            File file = path.toFile();

            System.out.println("file: " + file.toString());
            System.out.println("file path: " + file.toPath().toString());

            try(FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(Charset.forName("utf8")));
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return e.getMessage();
        }

        return path.toAbsolutePath().toString();
    }


    public static String nameOnly(String filename) {
        String string = filename;
        if (filename.contains(File.separator)) {
            int idx = string.lastIndexOf(File.separator);
            string = string.substring(idx + 1);
        }
        if (string.contains(".")) {
            int idx = string.lastIndexOf(".");
            return string.substring(0, idx);
        } else {
            return "";
        }
    }


}
