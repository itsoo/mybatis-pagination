package com.github.itsoo.plugin.toolkit;

import java.lang.reflect.Field;

/**
 * ReflectHelper
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
     * @throws NoSuchFieldException NoSuchFieldException
     */
    public static Field getObjectField(Object obj, String fieldName) throws NoSuchFieldException {
        Class<?> clazz = obj.getClass();
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException ignore) {
            return clazz.getField(fieldName);
        }
    }

    /**
     * 获取属性值
     *
     * @param obj       Object
     * @param fieldName String
     * @return Object
     * @throws IllegalAccessException IllegalAccessException
     * @throws NoSuchFieldException   NoSuchFieldException
     */
    public static Object getFieldValue(Object obj, String fieldName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = getObjectField(obj, fieldName);
        if (field.isAccessible()) {
            return field.get(obj);
        }

        field.setAccessible(true);
        Object value = field.get(obj);
        field.setAccessible(false);

        return value;
    }

    /**
     * 设置属性值
     *
     * @param obj       Object
     * @param fieldName String
     * @param value     Object
     * @throws NoSuchFieldException   NoSuchFieldException
     * @throws IllegalAccessException IllegalAccessException
     */
    public static void setFieldValue(Object obj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = getObjectField(obj, fieldName);
        if (field.isAccessible()) {
            field.set(obj, value);
        } else {
            field.setAccessible(true);
            field.set(obj, value);
            field.setAccessible(false);
        }
    }

    /**
     * 复制对象属性
     *
     * @param originObj Object
     * @param targetObj Object
     * @param fieldName String
     * @throws NoSuchFieldException   NoSuchFieldException
     * @throws IllegalAccessException IllegalAccessException
     */
    public static void copyObjectField(Object originObj, Object targetObj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        if (getObjectField(originObj, fieldName) != null) {
            setFieldValue(targetObj, fieldName, getFieldValue(originObj, fieldName));
        }
    }
}
