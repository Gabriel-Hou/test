package com.hsbc.test.common.infrastructure.utils.string;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtil {

    private static final int SALT = 4;
    private static String zhPattern = "[\\u4e00-\\u9fa5]";

    public static boolean equals(String content1, String content2) {
        return content1 != null && content2 != null && content1.equals(content2);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

}
