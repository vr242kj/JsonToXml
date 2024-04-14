package org.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PerformanceTest {
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8})
    public void testPerformanceWithDifferentThreadPoolSizes(int threadCount) throws NoSuchFieldException, IllegalAccessException {
        long startTime = System.currentTimeMillis();

        String[] args = {"src\\test\\resources\\performance", "name"};

        Field field = ConsoleInterface.class.getDeclaredField("executorService");
        field.setAccessible(true);
        ExecutorService newExecutorService = Executors.newFixedThreadPool(threadCount);

        field.set(null, newExecutorService);

        ConsoleInterface.start(args);

        long endTime =  System.currentTimeMillis();
        long durationInMilliseconds = endTime - startTime;
        System.out.println("Execution time with thread pool size " + threadCount + ": " + durationInMilliseconds + " ms");
    }

    @AfterAll
    public static void deleteTestFile() throws IOException {
        Files.delete(Path.of("statistics_by_name.xml"));
    }
}
