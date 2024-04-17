package org.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XMLWriterTest {
    private XMLWriter xmlWriter;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        xmlWriter = new XMLWriter(executorService);    }
    @Test
    void generateStatisticsFile() throws IOException, InterruptedException {
        Map<String, Map<String, Integer>> attributeValueCounts = new ConcurrentHashMap<>();
        Map<String, Integer> data = new ConcurrentHashMap<>();
        data.put("value1", 10);
        data.put("value2", 5);
        attributeValueCounts.put("attribute1", data);

        xmlWriter.generateStatisticsFile(attributeValueCounts);

        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.MILLISECONDS);

        try (BufferedReader reader = new BufferedReader(new FileReader("statistics_by_attribute1.xml"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            assertTrue(content.toString().contains("<value>value1</value>"));
            assertTrue(content.toString().contains("<count>10</count>"));
            assertTrue(content.toString().contains("<value>value2</value>"));
            assertTrue(content.toString().contains("<count>5</count>"));
        }
    }

    @AfterAll
    public static void deleteTestFile() throws IOException {
        Files.delete(Path.of("statistics_by_attribute1.xml"));
    }
}
