package org.example;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Main {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Main <directory_path> <attribute_name>." +
                    " If the attribute name has multiple values, please separate them with commas and avoid spaces");
            return;
        }

        String directory = args[0];
        String attributeName = args[1];

        List<String> attributeNames = Arrays.stream(attributeName.split(","))
                .map(String::trim)
                .toList();

        for (String a : attributeNames)
            System.out.println(a);

        Path dirPath = Path.of(directory);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            System.out.println("Invalid directory path.");
            return;
        }

        Map<String, Map<String, Integer>> statistics = new ConcurrentHashMap<>();

        try (Stream<Path> pathStream = Files.list(dirPath)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(filePath -> filePath.toString().toLowerCase().endsWith(".json"))
                    .forEach(filePath -> executorService.submit(() -> processJsonFile(filePath, attributeNames, statistics)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Statistics for attribute '" + attributeName + "':");

        for (Map.Entry<String, Map<String, Integer>> outerEntry : statistics.entrySet()) {
            System.out.print(outerEntry.getKey() + "|| ");
            Map<String, Integer> innerMap = outerEntry.getValue();
            for (Map.Entry<String, Integer> innerEntry : innerMap.entrySet()) {
                System.out.print(innerEntry.getKey()+ ": " + innerEntry.getValue() + " | ");
            }
            System.out.println();
        }

        statistics.forEach((key, value) -> {
            executorService.submit(() -> {
                String fileName = "statistics_by_" + key + ".xml";
                try {
                    writeStatisticsToXML(fileName, value);
                    System.out.println("Statistics file created: " + fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });

        executorService.shutdown();
        try {
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static void writeStatisticsToXML(String fileName, Map<String, Integer> data) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("<statistics>\n");
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                writer.write("  <item>\n");
                writer.write("    <value>" + entry.getKey() + "</value>\n");
                writer.write("    <count>" + entry.getValue() + "</count>\n");
                writer.write("  </item>\n");
            }
            writer.write("</statistics>");
        }
    }
    private static void processJsonFile(Path filePath, List<String> attributeNames, Map<String, Map<String, Integer>> statistics) {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(filePath))) {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (attributeNames.contains(name) && reader.peek() != JsonToken.NULL) {
                        String attributeValue = reader.nextString();
                        statistics.computeIfAbsent(name, k -> new ConcurrentHashMap<>())
                                .merge(attributeValue, 1, Integer::sum);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            }
            reader.endArray();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}