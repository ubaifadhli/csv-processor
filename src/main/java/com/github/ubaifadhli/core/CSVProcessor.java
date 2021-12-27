package com.github.ubaifadhli.core;

import com.github.ubaifadhli.annotations.CSVColumn;
import com.github.ubaifadhli.data.HeaderDetail;
import com.github.ubaifadhli.exceptions.ColumnNotFoundException;
import com.github.ubaifadhli.reflections.ReflectionHelper;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CSVProcessor<T> {
    private final String DEFAULT_DELIMITER = ", ";
    private final String ESCAPE_SEQUENCE = "\\";
    private final char DEFAULT_IGNORED_SPLIT_REGEX = ',';

    private File csvFile;
    private Class<T> objectClass;

    public CSVProcessor(String filePath, Class<T> objectClass) {
        csvFile = Paths.get(filePath).toFile();
        this.objectClass = objectClass;
    }

    private List<String> splitString(String text, String regex) {
        return Arrays.asList(text.split(regex));
    }

    private List<HeaderDetail> getDefaultHeaderDetails() {
        List<Field> fields = ReflectionHelper.getFields(objectClass);

        List<HeaderDetail> headerDetails = new ArrayList<>();

        for (int i = 0; i < fields.size(); i++) {
            Field currentField = fields.get(i);

            HeaderDetail.HeaderDetailBuilder builder = HeaderDetail.builder()
                    .index(i)
                    .csvName(currentField.getName())
                    .field(currentField);

            headerDetails.add(builder.build());
        }

        return headerDetails;
    }

    private List<HeaderDetail> determineHeaderDetails(String csvHeaderString) {
        List<String> csvHeaders = splitString(csvHeaderString, DEFAULT_DELIMITER);

        List<HeaderDetail> headerDetails = new ArrayList<>();

        for (int i = 0; i < csvHeaders.size(); i++) {
            String currentHeader = csvHeaders.get(i);

            HeaderDetail.HeaderDetailBuilder builder = HeaderDetail.builder();

            builder.csvName(currentHeader)
                    .index(i);

            try {
                Field field = ReflectionHelper.getField(objectClass, currentHeader);
                builder.field(field);

            } catch (NoSuchFieldException e) {
                List<Field> annotatedFields = ReflectionHelper.getAnnotatedFields(objectClass, CSVColumn.class);

                System.out.println(annotatedFields.size());

                Optional<Field> foundField = annotatedFields.stream()
                        .filter(field -> field.getAnnotation(CSVColumn.class).name().equals(currentHeader))
                        .findFirst();

                if (foundField.isPresent()) {
                    builder.field(foundField.get());

                } else
                    throw new ColumnNotFoundException(currentHeader, objectClass);
            }

            headerDetails.add(builder.build());
        }

        return headerDetails;
    }

    public List<T> readFile() {
        if (csvFile.isFile()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(csvFile));

                List<String> csvRows = reader.lines().collect(Collectors.toList());

                String csvHeader = csvRows.get(0);

                // Removing first line which should be the header.
                csvRows.remove(0);

                List<HeaderDetail> headerDetails = determineHeaderDetails(csvHeader);

                List<T> datum = new ArrayList<>();

                csvRows.forEach(row -> {
                    List<String> columns = splitString(row, DEFAULT_DELIMITER);

                    T data = createInstance();

                    for (int i = 0; i < columns.size(); i++) {
                        int currentIndex = i;

                        // Should be guaranteed to get the instance, so no need for isPresent checking.
                        HeaderDetail headerDetail = headerDetails.stream()
                                .filter(detail -> detail.getIndex() == currentIndex)
                                .findFirst()
                                .get();

                        if (headerDetail.hasAnnotation() && headerDetail.getAnnotation().splitByCharacter() != DEFAULT_IGNORED_SPLIT_REGEX) {
                            char splitCharacter = headerDetail.getAnnotation().splitByCharacter();

                            List<String> splitTexts = splitString(columns.get(i), ESCAPE_SEQUENCE + splitCharacter);

                            ReflectionHelper.setValue(data, headerDetail.getField(), splitTexts);

                        } else
                            ReflectionHelper.setValue(data, headerDetail.getField(), columns.get(i));
                    }

                    datum.add(data);
                });

                return datum;

            // Should not throw exception because file has been checked using isFile().
            } catch (FileNotFoundException e) {
                throw new RuntimeException("File does not exists.");
            }
        }

        else
            throw new RuntimeException("File does not exists");
    }

    private T createInstance() {
        try {
            return objectClass.newInstance();

        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(String.format("Cannot create instance of %s", objectClass));
        }
    }

    public void writeToFile(List<T> datum) {
        List<HeaderDetail> headerDetails;

        boolean addHeader = csvFile.isFile();



        // Assumption : Header and data exist when file exists
        if (csvFile.isFile()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(csvFile));
                String headerString = reader.lines().findFirst().get();

                headerDetails = determineHeaderDetails(headerString);

            } catch (FileNotFoundException e) {
                throw new RuntimeException("Unknown error occured");
            }

        } else {
            try {
                csvFile.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }

            headerDetails = getDefaultHeaderDetails();
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile));

            boolean isFirstColumn = true;

            datum.forEach(data -> {
                StringBuilder stringBuilder = new StringBuilder();

                headerDetails.forEach(detail -> {
                    if (isFirstColumn) {
                        stringBuilder.append(DEFAULT_DELIMITER);
                        isFirstColumn = false;
                    }

                    try {
                        stringBuilder.append(detail.getField().get(data))
                                .append(DEFAULT_DELIMITER);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
            });

        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            // Assumption : When file exists then Header and data also exist
            if (csvFile.isFile()) {
                if (headerDetails != null) {
                    BufferedReader reader = new BufferedReader(new FileReader(csvFile));


                } else {
                    // Read
                }

                if (headerDetails.size() > 0) {
                    headerDetails.forEach(headerDetail -> {

                    });

                } else {

                }
            }

        } catch (FileNotFoundException e) {

        }


        // If has file && has header
            // Fill according to header order

        // If doesn't have file
            // Fill according to class order
    }
}
