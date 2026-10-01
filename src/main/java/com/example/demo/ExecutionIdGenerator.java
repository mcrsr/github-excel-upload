package com.example.demo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ExecutionIdGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ExecutionIdGenerator() {
    }

    public static String generate() {

        String timestamp =
                LocalDateTime.now().format(FORMATTER);

        String uniqueId =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();

        return "EXEC-" + timestamp + "-" + uniqueId;
    }
}