package io.github.raftServer.raft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Storage {

    private static final Path CURRENT_TERM_FILE = Path.of("currentTerm.txt");
    private static final Path VOTED_FOR_FILE = Path.of("votedFor.txt");
    private static final Path LOG_FILE = Path.of("raftLog.dat");
    private static final Path LOG_INDEX_FILE = Path.of("raftLog.dat");

    public void writeCurrentTerm(int currentTerm) {
        try {
            Files.writeString(
                    CURRENT_TERM_FILE,
                    String.valueOf(currentTerm),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ex) {
            System.err.println("Failed to write current term: " + ex.getMessage());
        }
    }

    public void writeVotedFor(String votedFor) {
        try {
            Files.writeString(
                    VOTED_FOR_FILE,
                    votedFor,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ex) {
            System.err.println("Failed to write votedFor: " + ex.getMessage());
        }
    }

    public void appendLogEntry(String logEntry) {
        try {
            Files.writeString(
                    LOG_FILE,
                    logEntry + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            System.err.println("Failed to append log entry: " + ex.getMessage());
        }
    }

    public int readCurrentTerm() {
        try {
            if (!Files.exists(CURRENT_TERM_FILE)) return 0;

            String value = Files.readString(CURRENT_TERM_FILE).trim();
            if (value.isEmpty()) return 0;

            return Integer.parseInt(value);
        } catch (IOException | NumberFormatException ex) {
            System.err.println("Failed to read current term: " + ex.getMessage());
            return 0;
        }
    }

    public String readVotedFor() {
        try {
            if (!Files.exists(VOTED_FOR_FILE)) return null;

            String value = Files.readString(VOTED_FOR_FILE).trim();
            if (value.isEmpty()) return null;

            return value;
        } catch (IOException | NumberFormatException ex) {
            System.err.println("Failed to read votedFor: " + ex.getMessage());
            return null;
        }
    }

    public List<String> readLogEntries() {
        try {
            if (!Files.exists(LOG_FILE)) return List.of();

            return Files.readAllLines(LOG_FILE);
        } catch (IOException ex) {
            System.err.println("Failed to read log entries: " + ex.getMessage());
            return List.of();
        }
    }

    public void initFiles() {
        try {
            if (!Files.exists(CURRENT_TERM_FILE)) {
                Files.writeString(
                        CURRENT_TERM_FILE,
                        "",
                        StandardOpenOption.CREATE
                );
            }
            if (!Files.exists(VOTED_FOR_FILE)) {
                Files.writeString(
                        VOTED_FOR_FILE,
                        "",
                        StandardOpenOption.CREATE
                );
            }
            if (!Files.exists(LOG_FILE)) {
                Files.writeString(
                        LOG_FILE,
                        "",
                        StandardOpenOption.CREATE
                );
            }
        } catch (IOException ex) {
            System.err.println("Failed to initialize storage files: " + ex.getMessage());
        }
    }
}