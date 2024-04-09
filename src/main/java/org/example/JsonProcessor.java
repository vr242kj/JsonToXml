package org.example;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class JsonProcessor {
    private final ExecutorService executorService;

    public JsonProcessor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void processJsonFiles(
            Path dirPath,
            Map<String, Map<String, Integer>> attributeValueCounts,
            List<String> attributeNames) throws IOException {
        try (Stream<Path> pathStream = Files.list(dirPath)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(filePath -> filePath.toString().toLowerCase().endsWith(".json"))
                    .forEach(filePath -> executorService.submit(() -> {
                        try {
                            parseJson(filePath, attributeNames, attributeValueCounts);
                        } catch (IOException e) {
                            throw new RuntimeException("Error parsing file: " + filePath, e);
                        }
                    }));
        } catch (IOException e) {
            throw new IOException("An error occurred while getting the file list: " + e.getMessage());
        }
    }

    private void parseJson(
            Path filePath,
            List<String> attributeNames,
            Map<String, Map<String, Integer>> attributeValueCounts
    ) throws IOException {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(filePath))) {
            processJsonElement(reader, attributeNames, attributeValueCounts);
        }
    }

    private void processJsonElement(
            JsonReader reader,
            List<String> attributeNames,
            Map<String, Map<String, Integer>> attributeValueCounts
    ) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (reader.peek() == JsonToken.NULL) {
                    reader.skipValue();
                } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    processJsonElement(reader, attributeNames, attributeValueCounts);
                } else {
                    processValue(reader, key, attributeNames, attributeValueCounts);
                }
            }
            reader.endObject();
        }
        reader.endArray();
    }

    private void processValue(
            JsonReader reader,
            String key,
            List<String> attributeNames,
            Map<String, Map<String, Integer>> attributeValueCounts
    ) throws IOException {
        String value = reader.nextString();
        if (attributeNames.contains(key) && !value.isEmpty()) {
            String firstValue = getFirstValue(value);
            attributeValueCounts.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                    .merge(firstValue, 1, Integer::sum);
        }
    }

    private String getFirstValue(String values){
        String[] arrayOfValues = values.split(",");
        return arrayOfValues[0].trim();
    }
}


