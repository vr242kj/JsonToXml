package org.example;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonProcessorTest {
    private JsonProcessor jsonProcessor;
    private ExecutorService executorService;
    private Map<String, Map<String, Integer>> attributeValueCounts;

    @BeforeAll
    static void prepareData() throws IOException {
        Gson gson = new Gson();

        List<Book> books = new ArrayList<>();
        books.add(new Book("1984", "George Orwell", 1949, "Dystopian, Political Fiction"));
        books.add(new Book("Pride and Prejudice", "Jane Austen", 1813, "Romance, Satire"));
        books.add(new Book("Romeo and Juliet", "William Shakespeare", 1597, "Romance, Tragedy"));

        try (FileWriter file = new FileWriter("src/test/resources/test.json")) {
            gson.toJson(books, file);
        }
    }

    static class Book {
        String title;
        String author;
        int yearPublished;
        String genre;

        Book(String title, String author, int yearPublished, String genre) {
            this.title = title;
            this.author = author;
            this.yearPublished = yearPublished;
            this.genre = genre;
        }
    }

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(4);
//        executorService = mock(ExecutorService.class); // Создание мок-объекта
        List<String> attributeNames = Arrays.asList("genre", "author");
        attributeValueCounts = new HashMap<>();
        jsonProcessor = new JsonProcessor(executorService, attributeNames, attributeValueCounts);
    }

    @Test
    void testConstructor() {
        assertNotNull(jsonProcessor.getExecutorService());
        assertNotNull(jsonProcessor.getAttributeNames());
        assertNotNull(jsonProcessor.getAttributeValueCounts());
    }

    @Test
    void testProcessJsonFiles() throws IOException {
        Path dirPath = Path.of("src/test/resources");
        jsonProcessor.processJsonFiles(dirPath);

        System.out.println(jsonProcessor.getAttributeValueCounts());
    }
}


