package com.acuver.autwit.internal.reporting;

import io.qameta.allure.Allure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AllureAttachmentUtils {

    private static final Path BASE = Paths.get("allure-artifacts");

    static {
        try { Files.createDirectories(BASE); } catch (IOException ignored) {}
    }

    private AllureAttachmentUtils(){}

    public static void saveLargePayload(String name, String content, String ext) {
        try {
            String fileName = name.replaceAll("[^a-zA-Z0-9-_\\.]", "_") + "." + ext;
            Path p = BASE.resolve(fileName);
            Files.writeString(p, content);
            Allure.link(name, p.toUri().toString());
        } catch (Exception e) {
            Allure.addAttachment(name + " (error)", e.getMessage());
        }
    }
}
