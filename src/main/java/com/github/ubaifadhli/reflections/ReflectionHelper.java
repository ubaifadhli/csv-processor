package com.github.ubaifadhli.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionHelper {
    public static List<Field> getFields(Class<?> specifiedClass) {
        return Arrays.stream(specifiedClass.getDeclaredFields())
                .collect(Collectors.toList());
    }

    public static Object getFieldValue(Object object, Field field) {
        Object value;

        try {
            field.setAccessible(true);

            value = field.get(object);

            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value.");
        }

        return value;
    }

    public static List<Field> getAnnotatedFields(Class<?> specifiedClass, Class<? extends Annotation> annotationClass) {
        return getFields(specifiedClass)
                .stream()
                .filter(field -> field.isAnnotationPresent(annotationClass))
                .collect(Collectors.toList());
    }

    public static Field getField(Class<?> specifiedClass, String fieldName) throws NoSuchFieldException {
        return specifiedClass.getDeclaredField(fieldName);
    }

    public static void setValue(Object object, Field field, Object value) {
        try {
            field.setAccessible(true);

            if (field.getType().equals(Integer.TYPE))
                field.setInt(object, Integer.parseInt(value.toString()));

            else
                field.set(object, value);

            field.setAccessible(false);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
