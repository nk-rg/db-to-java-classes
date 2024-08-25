package com.dico.gen.util;

public class StrUtil {
    public static String toCamelCase(String s, boolean capitalizeFirst) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = capitalizeFirst;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
