package org.example;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class JsonProcessor {
    private final ExecutorService executorService;
    private final Map<String, Map<String, Integer>> attributeValueCounts;
    private final List<String> attributeNames;

    public JsonProcessor(ExecutorService executorService, List<String> attributeNames,
                         Map<String, Map<String, Integer>> attributeValueCounts
    ) {
        this.executorService = executorService;
        this.attributeNames = attributeNames;
        this.attributeValueCounts = attributeValueCounts;
    }

    public void processJsonFiles(Path dirPath) throws IOException {
        try (Stream<Path> pathStream = Files.list(dirPath)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(filePath -> filePath.toString().toLowerCase().endsWith(".json"))
                    .forEach(filePath -> executorService.submit(() -> {
                        try {
                            parseJson(filePath);
                        } catch (IOException e) {
                            throw new RuntimeException("Error parsing file: " + filePath, e);
                        }
                    }));
        } catch (IOException e) {
            throw new IOException("An error occurred while getting the file list: " + e.getMessage());
        }
    }

    private void parseJson(Path filePath) throws IOException {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(filePath))) {
            processJsonElement(reader);
        }
    }

    private void processJsonElement(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue();
                } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    processJsonElement(reader);
                } else {
                    processValue(reader, key);
                }
            }
            reader.endObject();
        }
        reader.endArray();
    }

    private void processValue(JsonReader reader, String key) throws IOException {
        String value = reader.nextString();
        if (!attributeNames.contains(key) || value.isEmpty()) {
            return;
        }
        String[] arrayOfValues = parseValues(value);
        for (String valueFromArray : arrayOfValues) {
            attributeValueCounts.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                    .merge(valueFromArray, 1, Integer::sum);
        }
    }

    private String[] parseValues(String values){
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }
}


