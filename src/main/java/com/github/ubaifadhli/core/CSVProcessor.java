package com.github.ubaifadhli.core;

import com.github.ubaifadhli.annotations.CSVColumn;
import com.github.ubaifadhli.data.HeaderDetail;
import com.github.ubaifadhli.exceptions.ColumnNotFoundException;
import com.github.ubaifadhli.reflections.ReflectionHelper;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
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

        boolean fileAlreadyExists;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(csvFile));

            // Assumption : Header and data exist when file exists
            String headerString = reader.lines().findFirst().get();

            headerDetails = determineHeaderDetails(headerString);

            fileAlreadyExists = true;

        } catch (FileNotFoundException e) {

            try {
                csvFile.getParentFile().mkdirs();
                csvFile.createNewFile();

            } catch (IOException ex) {
                throw new RuntimeException("Error occured when creating file.", ex);
            }

            headerDetails = getDefaultHeaderDetails();

            fileAlreadyExists = false;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, fileAlreadyExists));

            if (!fileAlreadyExists) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();

                    // Adding first header separately because it doesn't need preceding delimiter.
                    stringBuilder.append(headerDetails.get(0).getCsvName());

                    for (int i = 1; i < headerDetails.size(); i++)
                        stringBuilder
                                .append(DEFAULT_DELIMITER)
                                .append(headerDetails.get(i).getCsvName());


                    writer.write(stringBuilder.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<HeaderDetail> lambdaHeaderDetails = headerDetails;

            datum.forEach(data -> {
                StringBuilder stringBuilder = new StringBuilder();

                try {
                    writer.newLine();

                    // Adding first column separately because it doesn't need preceding delimiter.
                    stringBuilder.append(ReflectionHelper.getFieldValue(data, lambdaHeaderDetails.get(0).getField()));

                    for (int i = 1; i < lambdaHeaderDetails.size(); i++) {
                        HeaderDetail currentDetail = lambdaHeaderDetails.get(i);

                        stringBuilder.append(DEFAULT_DELIMITER);

                        if (currentDetail.hasAnnotation() && currentDetail.getAnnotation().splitByCharacter() != DEFAULT_IGNORED_SPLIT_REGEX) {
                            StringBuilder joinStringBuilder = new StringBuilder();
                            char splitCharacter = currentDetail.getAnnotation().splitByCharacter();

                            // TODO Make this work for other primitives
                            Object value = ReflectionHelper.getFieldValue(data, currentDetail.getField());

                            List<?> splitTexts = new ArrayList<>((Collection<?>) value);

                            joinStringBuilder.append(splitTexts.get(0));

                            splitTexts.remove(0);

                            splitTexts.forEach(text ->
                                    joinStringBuilder
                                            .append(splitCharacter)
                                            .append(text)
                            );

                            stringBuilder.append(joinStringBuilder);
                        } else
                            stringBuilder.append(ReflectionHelper.getFieldValue(data, currentDetail.getField()));
                    }

                    writer.write(stringBuilder.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
