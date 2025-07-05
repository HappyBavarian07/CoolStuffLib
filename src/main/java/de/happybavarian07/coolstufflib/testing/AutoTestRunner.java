package de.happybavarian07.coolstufflib.testing;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoTestRunner {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final List<Class<?>> testClasses = new ArrayList<>();
    private final List<TestResult> results = new ArrayList<>();
    private boolean hasRun = false;

    public AutoTestRunner(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void registerTestClass(Class<?> testClass) {
        testClasses.add(testClass);
    }

    public void registerTestClass(String className) {
        try {
            Class<?> testClass = Class.forName(className);
            registerTestClass(testClass);
        } catch (ClassNotFoundException ignored) {
            logger.log(Level.FINE, "Test class not found: " + className);
        }
    }

    public CompletableFuture<List<TestResult>> runAllTests() {
        if (hasRun) {
            return CompletableFuture.completedFuture(Collections.unmodifiableList(results));
        }

        return CompletableFuture.supplyAsync(() -> {
            results.clear();
            logger.info("Starting automatic test execution...");

            for (Class<?> testClass : testClasses) {
                runTestClass(testClass);
            }

            logResults();
            hasRun = true;
            return Collections.unmodifiableList(results);
        });
    }

    private void runTestClass(Class<?> testClass) {
        try {
            Object testInstance = testClass.getDeclaredConstructor().newInstance();

            Method[] methods = testClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(InGameTest.class)) {
                    InGameTest testAnnotation = method.getAnnotation(InGameTest.class);
                    if (testAnnotation.disabled()) {
                        results.add(new TestResult(testClass.getSimpleName(), method.getName(), true, "SKIPPED", testAnnotation.description()));
                        continue;
                    }

                    runTestMethod(testInstance, method, testAnnotation);
                }
            }
        } catch (Exception e) {
            results.add(new TestResult(testClass.getSimpleName(), "ClassInit", false,
                "Failed to initialize test class: " + e.getMessage(), ""));
            logger.log(Level.WARNING, "Failed to initialize test class: " + testClass.getSimpleName(), e);
        }
    }

    private void runTestMethod(Object testInstance, Method method, InGameTest testAnnotation) {
        String testName = method.getName();
        String className = testInstance.getClass().getSimpleName();
        String description = testAnnotation.description();

        try {
            method.setAccessible(true);
            method.invoke(testInstance);
            results.add(new TestResult(className, testName, true, null, description));
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            results.add(new TestResult(className, testName, false, errorMessage, description));
        }
    }

    private void logResults() {
        int totalTests = 0;
        int passedTests = 0;
        int skippedTests = 0;

        logger.info("=== Test Results ===");

        for (TestResult result : results) {
            totalTests++;

            if (result.status.equals("SKIPPED")) {
                skippedTests++;
                logger.info(String.format("[SKIPPED] %s::%s - %s",
                    result.className, result.methodName, result.description));
            } else if (result.passed) {
                passedTests++;
                logger.info(String.format("[PASSED] %s::%s - %s",
                    result.className, result.methodName, result.description));
            } else {
                logger.warning(String.format("[FAILED] %s::%s - %s: %s",
                    result.className, result.methodName, result.description, result.errorMessage));
            }
        }

        logger.info(String.format("Total: %d | Passed: %d | Failed: %d | Skipped: %d",
            totalTests, passedTests, (totalTests - passedTests - skippedTests), skippedTests));
    }

    public void scheduleAutomaticExecution(long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            runAllTests();
        }, delayTicks);
    }

    public List<TestResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public static class TestResult {
        private final String className;
        private final String methodName;
        private final boolean passed;
        private final String errorMessage;
        private final String description;
        private final String status;

        public TestResult(String className, String methodName, boolean passed, String errorMessage, String description) {
            this.className = className;
            this.methodName = methodName;
            this.passed = passed;
            this.errorMessage = errorMessage;
            this.description = description;
            this.status = "NORMAL";
        }

        public TestResult(String className, String methodName, boolean passed, String errorMessage, String description, String status) {
            this.className = className;
            this.methodName = methodName;
            this.passed = passed;
            this.errorMessage = errorMessage;
            this.description = description;
            this.status = status;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }
    }
}
