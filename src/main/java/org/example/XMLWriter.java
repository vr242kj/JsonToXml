package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class XMLWriter {

    private final ExecutorService executorService;

    public XMLWriter(ExecutorService executorService) {
        this.executorService = executorService;
    }

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

    private void writeStatisticsToXMLFile(
            String fileName,
            List<Map.Entry<String, Integer>> sortedData
    ) throws IOException {
        try (FileWriter fileWriter = new FileWriter(fileName);
             BufferedWriter writer = new BufferedWriter(fileWriter, 110)) {
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
