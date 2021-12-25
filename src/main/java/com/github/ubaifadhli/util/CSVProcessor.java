package com.github.ubaifadhli.util;

import com.github.ubaifadhli.annotations.CSVColumn;
import com.github.ubaifadhli.reflections.FieldHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CSVProcessor<T> {
    private File csvFile;
    private Class<T> objectClass;

    private final String DEFAULT_DELIMITER = ", ";
    private final String ESCAPE_SEQUENCE = "\\";
    private final char DEFAULT_EMPTY_SPLIT_MATCHER = ',';

    public CSVProcessor(String filePath, Class<T> objectClass) {
        csvFile = Paths.get(filePath).toFile();
        this.objectClass = objectClass;
    }

    private List<String> splitString(String text) {
        return Arrays.asList(text.split(DEFAULT_DELIMITER));
    }

    private List<HeaderDetail> determineHeaderDetail(String csvHeaderString) {
        List<String> csvHeaders = splitString(csvHeaderString);

        List<HeaderDetail> headerDetails = new ArrayList<>();

        for (int i = 0; i < csvHeaders.size(); i++) {
            String currentHeader = csvHeaders.get(i);

            HeaderDetail.HeaderDetailBuilder builder = HeaderDetail.builder();

            builder.objectClass(objectClass)
                    .csvName(currentHeader)
                    .index(i);

            try {
                Field field = FieldHelper.getField(objectClass, currentHeader);
                builder.field(field);

            } catch (NoSuchFieldException e) {
                List<Field> annotatedFields = FieldHelper.getAnnotatedFields(objectClass, CSVColumn.class);

                Optional<Field> foundField = annotatedFields.stream()
                        .filter(field -> field.getAnnotation(CSVColumn.class).name().equals(currentHeader))
                        .findFirst();

                if (foundField.isPresent()) {
                    builder.field(foundField.get());

                } else
                    throw new RuntimeException(String.format("Column %s was not found in class %s. Please annotate your variable for different CSV header name.", currentHeader, objectClass.getName()));

            }

            headerDetails.add(builder.build());
        }

        return headerDetails;
    }


    public List<T> readFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(csvFile));

        List<String> csvRows = reader.lines().collect(Collectors.toList());

        String csvHeader = csvRows.get(0);

        // Removing first line which should be the header.
        csvRows.remove(0);

        List<HeaderDetail> headerDetails = determineHeaderDetail(csvHeader);

        List<T> datum = new ArrayList<>();

        csvRows.forEach(row -> {
            List<String> columns = splitString(row);

            T data = createInstance();

            for (int i = 0; i < columns.size(); i++) {
                int currentIndex = i;

                HeaderDetail headerDetail = headerDetails.stream()
                        .filter(detail -> detail.getIndex() == currentIndex)
                        .findFirst()
                        .get();

                if (headerDetail.hasAnnotation() && headerDetail.getAnnotation().splitByCharacter() != DEFAULT_EMPTY_SPLIT_MATCHER) {
                    char splitCharacter = headerDetail.getAnnotation().splitByCharacter();

                    List<String> splitTexts = Arrays.asList(columns.get(i).split(ESCAPE_SEQUENCE + splitCharacter));

                    FieldHelper.setValue(data, headerDetail.getField(), splitTexts);

                } else
                    FieldHelper.setValue(data, headerDetail.getField(), columns.get(i));
            }

            datum.add(data);
        });

        return datum;
    }

    private T createInstance() {
        try {
            return objectClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeToFile(List<T> data) {

    }


}
