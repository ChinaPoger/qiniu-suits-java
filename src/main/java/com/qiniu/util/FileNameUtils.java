package com.qiniu.util;

import java.io.IOException;

public class FileNameUtils {

    public static String addSuffix(String name, String suffix) {
        return name + suffix;
    }

    public static String addPrefix(String prefix, String name) {
        return prefix + name;
    }

    public static String addPrefixAndSuffixKeepExt(String prefix, String name, String suffix) throws IOException {

        return prefix + addSuffixKeepExt(name, suffix);
    }

    public static String addSuffixKeepExt(String name, String suffix) throws IOException {

        return addSuffixWithExt(name, suffix, null);
    }

    public static String addPrefixAndSuffixWithExt(String prefix, String name, String suffix, String ext)
            throws IOException {
        return prefix + addSuffixWithExt(name, suffix, ext);
    }

    public static String replaceExt(String name, String ext) throws IOException {
        return addSuffixWithExt(name, "", ext);
    }

    public static String addSuffixWithExt(String name, String suffix, String ext) throws IOException {

        if (name == null || "".equals(name)) throw new IOException("the name is empty");
        String[] names = name.split("\\.");
        if (names.length < 2) return name + suffix;
        else {
            StringBuilder shortName = new StringBuilder();
            for (int i = 0; i < names.length - 1; i++) {
                shortName.append(names[i]).append(".");
            }
            ext = (ext == null || "".equals(ext)) ? names[names.length - 1] : ext;
            return shortName.toString().substring(0, shortName.length() - 1) + suffix + "." + ext;
        }
    }
}