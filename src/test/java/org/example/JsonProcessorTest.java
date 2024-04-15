package org.example;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;


class JsonProcessorTest {
    private JsonProcessor jsonProcessor;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        List<String> attributeNames = Arrays.asList("genre", "author");
        Map<String, Map<String, Integer>> attributeValueCounts = new ConcurrentHashMap<>();
        jsonProcessor = new JsonProcessor(executorService, attributeNames, attributeValueCounts);
    }

    @Test
    void testConstructor() {
        assertNotNull(jsonProcessor.getExecutorService());
        assertNotNull(jsonProcessor.getAttributeNames());
        assertNotNull(jsonProcessor.getAttributeValueCounts());
    }

    @Test
    void processJsonFiles() throws IOException, InterruptedException {
        String path = "src\\test\\resources";
        jsonProcessor.processJsonFiles(Path.of(path));

        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("genre"));
        assertTrue(result.containsKey("author"));

        Map<String, Integer> genreCounts = result.get("genre");
        assertEquals(5, genreCounts.size());
        assertEquals(2, genreCounts.get("Romance"));
        assertEquals(1, genreCounts.get("Political Fiction"));
        assertEquals(1, genreCounts.get("Dystopian"));
        assertEquals(1, genreCounts.get("Satire"));
        assertEquals(1, genreCounts.get("Tragedy"));


        Map<String, Integer> authorCounts = result.get("author");
        assertEquals(3, authorCounts.size());
        assertEquals(1, authorCounts.get("George Orwell"));
        assertEquals(1, authorCounts.get("Jane Austen"));
        assertEquals(1, authorCounts.get("William Shakespeare"));
    }

    @Test
    void processJsonFiles_WhenDirPathNotExist_ThrowIOException() {
        String dirPath = "notExist";

        assertThrows(IOException.class,
                () -> jsonProcessor.processJsonFiles(Path.of(dirPath)));
    }

    @Test
    void parseJson() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = JsonProcessor.class.getDeclaredMethod("parseJson", Path.class);
        method.setAccessible(true);

        Path filePath = Path.of("src/test/resources/test.json");

        method.invoke(jsonProcessor, filePath);

        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("genre"));
        assertTrue(result.containsKey("author"));
    }

    @Test
    void parseJson_WhenFilePathNotExist_ThrowIOException() throws NoSuchMethodException {
        Method method = JsonProcessor.class.getDeclaredMethod("parseJson", Path.class);
        method.setAccessible(true);

        Path filePath = Path.of("notExist.json");

        assertThrows(IOException.class, () -> {
            try {
                method.invoke(jsonProcessor, filePath);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        });
    }

    @Test
    void processJsonElement_WhenKeyHasValue() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        JsonReader reader = mock(JsonReader.class);

        when(reader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);

        when(reader.nextName()).thenReturn("author");
        when(reader.nextString()).thenReturn("George Orwell");
        when(reader.peek()).thenReturn(JsonToken.BEGIN_OBJECT).thenReturn(JsonToken.STRING);

        Method method = JsonProcessor.class.getDeclaredMethod("processJsonElement", JsonReader.class);
        method.setAccessible(true);

        method.invoke(jsonProcessor, reader);

        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();
        assertEquals(1, result.size());
        assertEquals(1, result.get("author").size());
    }

    @Test
    void processJsonElement_WhenNoArrayStart_ThrowIOException() throws NoSuchMethodException, IOException {
        Method method = JsonProcessor.class.getDeclaredMethod("processJsonElement", JsonReader.class);
        method.setAccessible(true);
        JsonReader mockReader = mock(JsonReader.class);

        doThrow(IOException.class).when(mockReader).beginArray();

        assertThrows(IOException.class, () -> {
            try {
                method.invoke(jsonProcessor, mockReader);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        });
    }

    @Test
    void processJsonElement_WhenKeyHasArrayOfValues() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        JsonReader reader = mock(JsonReader.class);

        when(reader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true)
                .thenReturn(true).thenReturn(false);

        when(reader.nextName()).thenReturn("genre");
        when(reader.nextString()).thenReturn("Romance").thenReturn("Tragedy");
        when(reader.peek()).thenReturn(JsonToken.BEGIN_OBJECT).thenReturn(JsonToken.BEGIN_ARRAY)
                .thenReturn(JsonToken.STRING).thenReturn(JsonToken.STRING);

        Method method = JsonProcessor.class.getDeclaredMethod("processJsonElement", JsonReader.class);
        method.setAccessible(true);

        method.invoke(jsonProcessor, reader);

        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();
        assertEquals(1, result.size());
        assertEquals(2, result.get("genre").size());
    }

    @Test
    public void processValue_WhenKeyNotPresent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = JsonProcessor.class.getDeclaredMethod("processValue", String.class, String.class);
        method.setAccessible(true);

        method.invoke(jsonProcessor, "nonexistentKey", "value");

        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();
        assertTrue(result.isEmpty(), "Counts should remain empty if key is not present");
    }

    @Test
    public void processValue_WhenValueIsEmpty() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = JsonProcessor.class.getDeclaredMethod("processValue", String.class, String.class);
        method.setAccessible(true);

        method.invoke(jsonProcessor, "genre", "");

        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();
        assertTrue(result.isEmpty(), "Counts should remain empty if value is empty");
    }

    @Test
    public void processValue_WhenKeyPresentAndValueNotEmpty() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = JsonProcessor.class.getDeclaredMethod("processValue", String.class, String.class);
        method.setAccessible(true);

        method.invoke(jsonProcessor, "genre", "value1, value2, value1");
        Map<String, Map<String, Integer>> result = jsonProcessor.getAttributeValueCounts();

        assertEquals(2, result.get("genre").get("value1"), "Value1 count should be 2");
        assertEquals(1, result.get("genre").get("value2"), "Value2 count should be 1");
    }
}
