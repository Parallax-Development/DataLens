package dev.darkblade.datalens.repository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Appends human-readable edit records to {@code plugins/DataLens/changelog.log}.
 */
public final class ChangeLogRepository {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File logFile;
    private final Logger logger;
    private final boolean enabled;

    public ChangeLogRepository(File dataFolder, Logger logger, boolean enabled) {
        this.logFile = new File(dataFolder, "changelog.log");
        this.logger = logger;
        this.enabled = enabled;
        if (enabled) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Appends a single edit record to the log file.
     *
     * @param actor    the player who made the edit
     * @param objectId the inspectable object identifier
     * @param action   SET or REMOVE
     * @param path     the data path affected
     * @param oldValue previous value (null for REMOVE)
     * @param newValue new value (null for REMOVE)
     */
    public void log(String actor, String objectId, String action, String path,
                    String oldValue, String newValue) {
        if (!enabled) return;

        String timestamp = LocalDateTime.now().format(FMT);
        String line = String.format("[%s] [%s] [%s] %s @ %s  OLD=%s  NEW=%s%n",
                timestamp, actor, action, path, objectId, oldValue, newValue);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
        } catch (IOException e) {
            logger.warning("[DataLens] Failed to write changelog entry: " + e.getMessage());
        }
    }
}
