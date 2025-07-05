package de.happybavarian07.coolstufflib.testing;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InGameTestRunner {
    private final JavaPlugin plugin;
    private final Map<String, Class<?>> testClasses = new ConcurrentHashMap<>();
    private final List<TestResult> results = Collections.synchronizedList(new ArrayList<>());

    public InGameTestRunner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerTestClass(Class<?> testClass) {
        testClasses.put(testClass.getSimpleName(), testClass);
    }

    public CompletableFuture<List<TestResult>> runAllTests() {
        return CompletableFuture.supplyAsync(() -> {
            results.clear();
            for (Class<?> testClass : testClasses.values()) {
                runTestClass(testClass);
            }
            return new ArrayList<>(results);
        });
    }

    public CompletableFuture<List<TestResult>> runTest(String className) {
        return CompletableFuture.supplyAsync(() -> {
            Class<?> testClass = testClasses.get(className);
            if (testClass == null) {
                return Collections.emptyList();
            }

            List<TestResult> classResults = new ArrayList<>();
            runTestClass(testClass, classResults);
            return classResults;
        });
    }

    private void runTestClass(Class<?> testClass) {
        runTestClass(testClass, results);
    }

    private void runTestClass(Class<?> testClass, List<TestResult> resultList) {
        try {
            Object testInstance = testClass.getDeclaredConstructor().newInstance();

            Method[] methods = testClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(InGameTest.class)) {
                    runTestMethod(testInstance, method, resultList);
                }
            }
        } catch (Exception e) {
            resultList.add(new TestResult(testClass.getSimpleName(), "ClassInit", false,
                "Failed to initialize test class: " + e.getMessage()));
        }
    }

    private void runTestMethod(Object testInstance, Method method, List<TestResult> resultList) {
        String testName = method.getName();
        String className = testInstance.getClass().getSimpleName();

        try {
            method.setAccessible(true);
            method.invoke(testInstance);
            resultList.add(new TestResult(className, testName, true, null));
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            resultList.add(new TestResult(className, testName, false, errorMessage));
        }
    }

    public void printResults(CommandSender sender) {
        sender.sendMessage("§6=== Test Results ===");

        Map<String, List<TestResult>> groupedResults = new HashMap<>();
        for (TestResult result : results) {
            groupedResults.computeIfAbsent(result.className(), k -> new ArrayList<>()).add(result);
        }

        int totalTests = 0;
        int passedTests = 0;

        for (Map.Entry<String, List<TestResult>> entry : groupedResults.entrySet()) {
            sender.sendMessage("§e" + entry.getKey() + ":");

            for (TestResult result : entry.getValue()) {
                totalTests++;
                if (result.passed()) {
                    passedTests++;
                    sender.sendMessage("  §a✓ " + result.methodName());
                } else {
                    sender.sendMessage("  §c✗ " + result.methodName() + ": " + result.errorMessage());
                }
            }
        }

        sender.sendMessage("§6Total: " + totalTests + " | Passed: " + passedTests + " | Failed: " + (totalTests - passedTests));
    }

    public Map<String, Class<?>> getTestClasses() {
        return Collections.unmodifiableMap(testClasses);
    }

    public record TestResult(String className, String methodName, boolean passed, String errorMessage) {}
}
