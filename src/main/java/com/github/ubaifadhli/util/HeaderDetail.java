package com.github.ubaifadhli.util;

import com.github.ubaifadhli.annotations.CSVColumn;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

@Builder
@Data
public class HeaderDetail {
    private Class<?> objectClass;
    private int index;
    private Field field;
    private String csvName;

    public boolean hasAnnotation() {
        return field.isAnnotationPresent(CSVColumn.class);
    }

    public CSVColumn getAnnotation() {
        return field.getAnnotation(CSVColumn.class);
    }
}