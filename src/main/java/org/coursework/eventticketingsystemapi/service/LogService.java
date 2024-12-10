package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.model.LogEntry;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class LogService {
    private static final String LOG_FILE_PATH = "logs/application.log";

    // Thread-safe list to store log entries
    private List<LogEntry> logs = new CopyOnWriteArrayList<>();
    private long currentId = 1;

    public LogService() {
        // Initial log load
        loadExistingLogs();

        // Start watching log file
        startLogFileWatcher();
    }

    /**
     * Load existing logs from the log file
     */
    private void loadExistingLogs() {
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = createLogEntry(line);
                logs.add(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load existing logs: " + e.getMessage());
        }
    }


    /**
     * Start watching the log file for new logs
     */
    private void startLogFileWatcher() {
        try {
            Path path = Paths.get(LOG_FILE_PATH);
            // Reference: https://docs.oracle.com/javase/tutorial/essential/io/notification.html
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // Start watching in a separate thread
            new Thread(() -> {
                while (true) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                // Read new lines
                                readNewLogLines();
                            }
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start log file watcher: " + e.getMessage());
        }
    }

    /**
     * Read new log lines from the log file
     */
    private void readNewLogLines() {
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            // Skip already read lines
            for (int i = 0; i < logs.size(); i++) {
                reader.readLine();
            }

            // Read and process new lines
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = createLogEntry(line);
                logs.add(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read new log lines: " + e.getMessage());
        }
    }

    /**
     * Create a log entry from a log line
     */
    private LogEntry createLogEntry(String line) {
        LogEntry entry = new LogEntry();
        entry.setId(currentId++);
        entry.setFullLogLine(line);
        return entry;
    }

    /**
     * Get the last log entries
     */
    public List<LogEntry> getTailLogs(int limit, Long afterId) {
        // If afterId is provided, filter logs after that ID
        if (afterId != null) {
            return logs.stream()
                    .filter(log -> log.getId() > afterId)
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // Return last  logs
        return logs.stream()
                .skip(Math.max(0, logs.size() - limit))
                .collect(Collectors.toList());
    }
}