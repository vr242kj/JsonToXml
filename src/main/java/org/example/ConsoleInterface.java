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

/**
 * ConsoleInterface class provides a command-line interface for processing JSON files and generating XML statistics based on specified attributes.
 * The class utilizes multi-threading for efficient processing.
 */
public class ConsoleInterface {
    private static ExecutorService executorService;
    private static final String USAGE_MESSAGE = """
            Usage: mvn compile exec:java "-Dexec.args=<directory_path> <attribute_names>"
            "The <attribute_names> parameter should be a comma-separated list of attribute names, without any spaces.
            """;
    private static Path directoryPath;
    private static String attributeName;

    /**
     * Starts the processing of JSON files and generation of XML statistics based on provided command-line arguments.
     *
     * @param args Command-line arguments: <directory_path> <attribute_names>
     */
    public static void start(String[] args) {

        try {
            parseInputArguments(args);
            List<String> attributeNames = parseAttributes(attributeName);
            Map<String, Map<String, Integer>> attributeValueCounts = new ConcurrentHashMap<>();

            jsonProcessor(attributeNames, attributeValueCounts);

            shutdownExecutorService();

            writeXML(attributeNames, attributeValueCounts);

            shutdownExecutorService();

        } catch (IllegalArgumentException | IOException | InterruptedException e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * Parses the input command-line arguments.
     *
     * @param args Command-line arguments provided
     * @throws IllegalArgumentException If the arguments are invalid or missing
     */
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
            throw new IllegalArgumentException("Attribute list is empty.");
        }
    }

    /**
     * Parses the attribute names provided as a comma-separated string.
     *
     * @param attributeNames Comma-separated string of attribute names
     * @return List of parsed attribute names
     */
    private static List<String> parseAttributes(String attributeNames) {
        return Arrays.stream(attributeNames.split(",")).toList();
    }

    /**
     * Processes JSON files based on the provided attribute names and counts the occurrences of attribute values.
     *
     * @param attributeNames       The list of attribute names to be processed.
     * @param attributeValueCounts A ConcurrentHashMap to store the counts of attribute values.
     * @throws IOException          If an I/O error occurs while processing JSON files.
     */
    private static void jsonProcessor(
            List<String> attributeNames,
            Map<String, Map<String, Integer>> attributeValueCounts
    ) throws IOException {
        int fileCount = (int) Files.walk(directoryPath).count();
        executorService =  Executors.newFixedThreadPool(fileCount);
        JsonProcessor jsonParser = new JsonProcessor(executorService, attributeNames, attributeValueCounts);
        jsonParser.processJsonFiles(directoryPath);
    }

    /**
     * Generates XML statistics file based on the provided attribute names and their occurrence counts.
     *
     * @param attributeNames       The list of attribute names for which statistics are generated.
     * @param attributeValueCounts A ConcurrentHashMap containing the counts of attribute values.
     */
    private static void writeXML(List<String> attributeNames, Map<String, Map<String, Integer>> attributeValueCounts) {
        int numberOfAttributes = attributeNames.size();
        executorService =  Executors.newFixedThreadPool(numberOfAttributes);
        XMLWriter xmlWriter = new XMLWriter(executorService);
        xmlWriter.generateStatisticsFile(attributeValueCounts);
    }

    /**
     * Initiates shutdown of the ExecutorService.
     *
     * @throws InterruptedException If interrupted while waiting for termination after shutdown
     */
    public static void shutdownExecutorService() throws InterruptedException {
        executorService.shutdown();
        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }
}
