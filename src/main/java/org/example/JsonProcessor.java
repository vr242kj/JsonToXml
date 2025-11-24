package org.example;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;


/**
 * The JsonProcessor class is responsible for processing JSON files, extracting specified attributes, and
 * updating the counts of attribute values concurrently.
 */
public class JsonProcessor {
    private final ExecutorService executorService;
    private final Map<String, Map<String, Integer>> attributeValueCounts;
    private final List<String> attributeNames;

    /**
     * Constructs a JsonProcessor object with the specified ExecutorService, attribute names, and attribute value counts.
     *
     * @param executorService       The ExecutorService for concurrent processing
     * @param attributeNames        The list of attribute names to extract from JSON files
     * @param attributeValueCounts  The map to store counts of attribute values
     */
    public JsonProcessor(ExecutorService executorService, List<String> attributeNames,
                         Map<String, Map<String, Integer>> attributeValueCounts
    ) {
        this.executorService = executorService;
        this.attributeNames = attributeNames;
        this.attributeValueCounts = attributeValueCounts;
    }

    /**
     * Retrieves the ExecutorService associated with this JsonProcessor.
     *
     * @return The ExecutorService
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Retrieves the map containing attribute value counts.
     *
     * @return The map of attribute value counts
     */
    public Map<String, Map<String, Integer>> getAttributeValueCounts() {
        return attributeValueCounts;
    }

    /**
     * Retrieves the list of attribute names.
     *
     * @return The list of attribute names
     */
    public List<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * Processes JSON files in the specified directory, extracting attribute values and updating counts concurrently.
     *
     * @param dirPath The path to the directory containing JSON files
     * @return An array of CompletableFutures representing the asynchronous tasks for processing JSON files.
     * @throws IOException If an I/O error occurs while processing JSON files
     */
    public CompletableFuture<Void>[] processJsonFiles(Path dirPath) throws IOException {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try (Stream<Path> pathStream = Files.list(dirPath)) {
            pathStream
                    .filter(filePath -> Files.isRegularFile(filePath) && filePath.toString().toLowerCase().endsWith(".json"))
                    .forEach(filePath -> futures.add(runAsyncParseJson(filePath)));
        } catch (IOException e) {
            throw new IOException("An error occurred while getting the file list: " + e.getMessage(), e);
        }

        return futures.toArray(new CompletableFuture[0]);
    }

    /**
     * Runs asynchronous parsing of a single JSON file using the executor service.
     * Any IOException thrown during parsing is caught and logged to System.err.
     *
     * @param filePath The path to the JSON file to be parsed
     * @return A CompletableFuture representing the asynchronous parsing task
     */
    private CompletableFuture<Void> runAsyncParseJson(Path filePath) {
        return CompletableFuture.runAsync(() -> {
            try {
                parseJson(filePath);
            } catch (IOException e) {
                System.err.println("Error parsing file: " + filePath + ", " + e.getMessage());
            }
        }, executorService).exceptionally(ex -> null);
    }

    /**
     * Parses a JSON file, extracting attribute values and updating counts.
     *
     * @param filePath The path to the JSON file
     * @throws IOException If an I/O error occurs while parsing the JSON file
     */
    private void parseJson(Path filePath) throws IOException {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(filePath))) {
            processJsonElement(reader);
        }
    }

    /**
     * Processes a JSON element, which could be an object or an array.
     *
     * @param reader The JsonReader to read JSON elements
     * @throws IOException If an I/O error occurs while processing the JSON element
     */
    private void processJsonElement(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new IOException("Invalid JSON format: Expecting array at the root level.");
        }
        reader.beginArray();
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                throw new IOException("Invalid JSON format: Expecting beginning of the object - {.");
            }
            processJsonObject(reader);
        }
        reader.endArray();
    }

    /**
     * Processes a JSON object, extracting attribute values and updating counts.
     *
     * @param reader The JsonReader to read JSON objects
     * @throws IOException If an I/O error occurs while processing the JSON object
     */
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

    /**
     * Processes a JSON array, extracting attribute values and updating counts.
     *
     * @param reader    The JsonReader to read JSON arrays
     * @param parentKey The key of the parent JSON object
     * @throws IOException If an I/O error occurs while processing the JSON array
     */
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

    /**
     * Processes a JSON attribute value, updating counts if it matches one of the specified attributes.
     *
     * @param key   The attribute key
     * @param value The attribute value
     */
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

    /**
     * Parses a string containing multiple attribute values separated by commas.
     *
     * @param values The string containing attribute values
     * @return An array of parsed attribute values
     */
    private String[] parseValues(String values){
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }
}


