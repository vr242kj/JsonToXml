package org.example;

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

public class ConsoleInterface {
    private static final int THREAD_POOL_SIZE = 4;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final String USAGE_MESSAGE = """
            Usage: mvn compile exec:java "-Dexec.args=<directory_path> <attribute_names>"
            "The <attribute_names> parameter should be a comma-separated list of attribute names, without any spaces.
            """;
    private static Path directoryPath;
    private static String attributeName;

    public static void start(String[] args) {

        try {
            parseInputArguments(args);
            List<String> attributeNames = parseAttributes(attributeName);

            Map<String, Map<String, Integer>> attributeValueCounts = new ConcurrentHashMap<>();

            JsonProcessor jsonParser = new JsonProcessor(executorService, attributeNames, attributeValueCounts);
            jsonParser.processJsonFiles(directoryPath);

            waitForExecutorServiceTermination();

            XMLWriter xmlWriter = new XMLWriter(executorService);

            xmlWriter.generateStatisticsFile(attributeValueCounts);

            shutdownExecutorService();

        } catch (IllegalArgumentException | IOException | InterruptedException e){
            System.out.println(e.getMessage());
        }
    }

    private static void parseInputArguments(String[] args)  {
        if (args.length != 2) {
            throw new IllegalArgumentException(USAGE_MESSAGE);
        }

        String directory = args[0];
        attributeName = args[1];

        directoryPath = Path.of(directory);

        if (!Files.exists(directoryPath ) || !Files.isDirectory(directoryPath )) {
            throw new IllegalArgumentException("Invalid directory path.");
        }

        if (attributeName.isEmpty()) {
            throw new IllegalArgumentException("Attribute list is empty");
        }
    }
    private static List<String> parseAttributes(String attributeNames) {
        return Arrays.stream(attributeNames.split(",")).toList();
    }

    private static void waitForExecutorServiceTermination() throws InterruptedException {
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static void shutdownExecutorService() throws InterruptedException {
        executorService.shutdown();
        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }
}
