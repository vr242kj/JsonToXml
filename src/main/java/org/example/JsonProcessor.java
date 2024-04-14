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

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Map<String, Map<String, Integer>> getAttributeValueCounts() {
        return attributeValueCounts;
    }

    public List<String> getAttributeNames() {
        return attributeNames;
    }

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
            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                processJsonObject(reader);
            }
        }
        reader.endArray();
    }

    private void processJsonObject(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            switch (reader.peek()) {
                case STRING, NUMBER -> {
                    String value = reader.nextString();
                    processValue(key, value);
                }
                case BEGIN_OBJECT -> processJsonObject(reader);
                case BEGIN_ARRAY -> processJsonArray(reader, key);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void processJsonArray(JsonReader reader, String parentKey) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            switch (reader.peek()) {
                case STRING, NUMBER -> {
                    String value = reader.nextString();
                    processValue(parentKey, value);
                }
                case BEGIN_OBJECT -> processJsonObject(reader);
                default -> reader.skipValue();
            }
        }
        reader.endArray();
    }

    private void processValue(String key, String value) {
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


