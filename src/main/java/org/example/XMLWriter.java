package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * The XMLWriter class is responsible for generating XML statistics files based on attribute value counts.
 */
public class XMLWriter {
    private static final int BUFFER_SIZE_IN_CHARS = 125;
    private final ExecutorService executorService;

    /**
     * Constructs an XMLWriter object with the specified ExecutorService.
     *
     * @param executorService The ExecutorService for concurrent writing operations
     */
    public XMLWriter(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Generates an XML statistics file for each attribute based on the provided attribute value counts.
     *
     * @param attributeValueCounts The map containing attribute value counts
     */
    public void generateStatisticsFile(Map<String, Map<String, Integer>> attributeValueCounts) {
        attributeValueCounts.forEach((key, value) -> executorService.submit(() -> {
            String fileName = "statistics_by_" + key + ".xml";
            try {
                List<Map.Entry<String, Integer>> sortedData = value.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .toList();
                writeStatisticsToXMLFile(fileName, sortedData);
            } catch (IOException e) {
                throw new RuntimeException("An error occurred while generating statistics file: " + e.getMessage());
            }
        }));
    }

    /**
     * Writes the XML statistics data to a file.
     *
     * @param fileName   The name of the XML file to write
     * @param sortedData The sorted list of attribute value counts
     * @throws IOException If an I/O error occurs while writing to the file
     */
    private void writeStatisticsToXMLFile(
            String fileName,
            List<Map.Entry<String, Integer>> sortedData
    ) throws IOException {
        try (FileWriter fileWriter = new FileWriter(fileName);
             BufferedWriter writer = new BufferedWriter(fileWriter, BUFFER_SIZE_IN_CHARS)) {
            writer.write("<statistics>\n");
            for (Map.Entry<String, Integer> entry : sortedData) {
                String itemXML = """
                          <item>
                           <value>%s</value>
                           <count>%s</count>
                          </item>
                        """.formatted(entry.getKey(), entry.getValue());
                writer.write(itemXML);
            }
            writer.write("</statistics>");
        }
    }
}
