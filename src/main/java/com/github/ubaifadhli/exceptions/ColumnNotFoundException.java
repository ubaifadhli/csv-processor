package com.github.ubaifadhli.exceptions;

public class ColumnNotFoundException extends RuntimeException {
    public ColumnNotFoundException(String columnName, Class<?> objectClass) {
        super(String.format("Column %s was not found in class %s. Make sure you have variable or annotation with specified name.",
                columnName,
                objectClass.getName())
        );
    }
}
