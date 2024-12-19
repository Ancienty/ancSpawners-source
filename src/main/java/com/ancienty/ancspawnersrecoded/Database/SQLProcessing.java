package com.ancienty.ancspawnersrecoded.Database;

import com.ancienty.ancspawnersrecoded.Main;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SQLProcessing {

    private final ConcurrentLinkedQueue<DatabaseTask> operations_queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public void addDatabaseTask(DatabaseTask task) {
        if (!Main.getPlugin().isEnabled()) {
            // Process the task immediately since we cannot schedule new tasks
            processDatabaseTask(task);
        } else {
            operations_queue.add(task);
            // If processing is not running, start it
            if (isProcessing.compareAndSet(false, true)) {
                Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), this::processQueue);
            }
        }
    }

    public void processQueueSynchronously() {
        isProcessing.set(true);
        DatabaseTask task;
        while ((task = operations_queue.poll()) != null) {
            processDatabaseTaskWithDelay(task); // Use delay logic
        }
        isProcessing.set(false);
    }

    private void processDatabaseTask(DatabaseTask task) {
        processDatabaseTaskWithDelay(task); // Centralized to reuse delay logic
    }

    private void processDatabaseTaskWithDelay(DatabaseTask task) {
        try {
            // Introduce a small delay before each SQL operation
            Thread.sleep(10);

            final int MAX_RETRIES = 3; // Maximum retry attempts
            int attempt = 0;
            boolean success = false;

            while (attempt < MAX_RETRIES && !success) {
                try (Connection connection = SQLite.dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(task.getQuery())) {

                    Object[] parameters = task.getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        statement.setObject(i + 1, parameters[i]);
                    }

                    statement.executeUpdate();
                    success = true; // Mark as successful

                } catch (SQLException ex) {
                    attempt++;
                    if (attempt >= MAX_RETRIES) {
                        // Log the error after exhausting retries
                        String errorMessage = "Failed to execute database task after " + MAX_RETRIES + " attempts (Query: " + task.getQuery() + "): " + ex.getMessage();
                        Main.getPlugin().getLogger().severe(errorMessage);
                        Main.getPlugin().getAncLogger().writeError(errorMessage);
                    } else {
                        // Log a warning for retrying
                        Main.getPlugin().getLogger().warning("Retrying database task (" + attempt + "/" + MAX_RETRIES + "): " + task.getQuery());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = "Database task interrupted: " + e.getMessage();
            Main.getPlugin().getLogger().severe(errorMessage);
            Main.getPlugin().getAncLogger().writeError(errorMessage);
        }
    }

    private void processQueue() {
        DatabaseTask task;
        while ((task = operations_queue.poll()) != null) {
            processDatabaseTaskWithDelay(task); // Use delay logic for each task
        }
        // Set processing flag to false
        isProcessing.set(false);
        // Double-check if new tasks were added after finishing
        if (!operations_queue.isEmpty()) {
            if (isProcessing.compareAndSet(false, true)) {
                Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), this::processQueue);
            }
        }
    }

    // New method to execute database queries and process ResultSet with a callback, with delays and retries
    public void executeDatabaseQuery(DatabaseTask task, Consumer<ResultSet> callback) {
        if (!Main.getPlugin().isEnabled()) {
            // Handle the situation appropriately, perhaps log a warning or process synchronously if possible
            String warningMessage = "Cannot execute database query; plugin is disabled.";
            Main.getPlugin().getLogger().warning(warningMessage);
            Main.getPlugin().getAncLogger().writeError(warningMessage);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            try {
                // Introduce a small delay before starting the SQL operation
                Thread.sleep(10);

                final int MAX_RETRIES = 3; // Maximum retry attempts
                int attempt = 0;
                boolean success = false;

                while (attempt < MAX_RETRIES && !success) {
                    try (Connection connection = SQLite.dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement(task.getQuery())) {

                        Object[] parameters = task.getParameters();
                        for (int i = 0; i < parameters.length; i++) {
                            statement.setObject(i + 1, parameters[i]);
                        }

                        try (ResultSet resultSet = statement.executeQuery()) {
                            // Pass the ResultSet to the callback for processing
                            callback.accept(resultSet);
                            success = true; // Mark as successful
                        }
                    } catch (SQLException ex) {
                        attempt++;
                        if (attempt >= MAX_RETRIES) {
                            // Log the error after exhausting retries
                            String errorMessage = "Failed to execute database query after " + MAX_RETRIES + " attempts (Query: " + task.getQuery() + "): " + ex.getMessage();
                            Main.getPlugin().getLogger().severe(errorMessage);
                            Main.getPlugin().getAncLogger().writeError(errorMessage);
                        } else {
                            // Log a warning for retrying
                            Main.getPlugin().getLogger().warning("Retrying database query (" + attempt + "/" + MAX_RETRIES + "): " + task.getQuery());
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String errorMessage = "Database query interrupted: " + e.getMessage();
                Main.getPlugin().getLogger().severe(errorMessage);
                Main.getPlugin().getAncLogger().writeError(errorMessage);
            }
        });
    }
}
