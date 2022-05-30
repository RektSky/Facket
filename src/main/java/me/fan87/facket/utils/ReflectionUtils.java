package me.fan87.facket.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtils {

    public static List<Method> getAllMethods(Class<?> clazz, boolean allowedJavaLang) {
        ArrayList<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            if (!allowedJavaLang) {
                if (current.getName().startsWith("java.lang")) {
                    break;
                }
            }
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));

            current = current.getSuperclass();
        }
        return methods;
    }
    public static List<Field> getAllFields(Class<?> clazz, boolean allowedJavaLang) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            if (!allowedJavaLang) {
                if (current.getName().startsWith("java.lang")) {
                    break;
                }
            }
            fields.addAll(Arrays.asList(current.getDeclaredFields()));

            current = current.getSuperclass();
        }
        return fields;
    }

}
