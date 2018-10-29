package com.github.itsoo.plugin;

import java.lang.reflect.Field;

/**
 * 分页插件
 *
 * @author zxy
 */
public class ReflectHelper {

    /**
     * 属性名称得到属性
     *
     * @param obj       Object
     * @param fieldName String
     * @return Field
     */
    public static Field getFieldByFieldName(Object obj, String fieldName) {
        Class<?> clazz = obj.getClass();
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            try {
                return clazz.getField(fieldName);
            } catch (NoSuchFieldException ex) {
                return null;
            }
        }
    }

    /**
     * 获取属性值
     *
     * @param obj       Object
     * @param fieldName String
     * @return Object
     * @throws IllegalAccessException
     */
    public static Object getValueByFieldName(Object obj, String fieldName) throws IllegalAccessException {
        Field field = getFieldByFieldName(obj, fieldName);
        Object value = null;
        if (field != null) {
            if (field.isAccessible()) {
                value = field.get(obj);
            } else {
                field.setAccessible(true);
                value = field.get(obj);
                field.setAccessible(false);
            }
        }
        return value;
    }

    /**
     * 设置属性值
     *
     * @param obj       Object
     * @param fieldName String
     * @param value     Object
     * @throws IllegalAccessException
     */
    public static void setValueByFieldName(Object obj, String fieldName, Object value) throws IllegalAccessException {
        Field field = getFieldByFieldName(obj, fieldName);
        if (field.isAccessible()) {
            field.set(obj, value);
        } else {
            field.setAccessible(true);
            field.set(obj, value);
            field.setAccessible(false);
        }
    }
}
