package com.github.itsoo.plugin.toolkit;

import java.util.Map;

/**
 * PageUtils
 *
 * @author zxy
 */
public class PageUtils {

    public static int toNumber(Object obj) {
        return Integer.parseInt(obj.toString());
    }

    public static int getNumber(Map<String, Object> map, String key) {
        return toNumber(map.get(key));
    }

    public static <T> T getOrDefault(T obj, T def) {
        return obj == null ? def : obj;
    }

    public static boolean isEmpty(Object obj) {
        return obj == null || obj.toString().length() == 0;
    }

    public static boolean isEquals(Object obj1, Object obj2) {
        boolean notEmpty = !isEmpty(obj1) && !isEmpty(obj2);

        return notEmpty && isEquals(obj1.toString(), obj2.toString());
    }

    public static boolean isEquals(String str1, String str2) {
        return str1.equals(str2);
    }

    public static boolean isFalseValue(Object obj) {
        if (obj instanceof String) {
            return isEquals(obj, Boolean.FALSE);
        } else if (obj instanceof Boolean) {
            return Boolean.FALSE.equals(obj);
        }

        return false;
    }
}
