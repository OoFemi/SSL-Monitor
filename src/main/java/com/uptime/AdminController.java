package com.uptime;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

public class AdminController {

    public static void uploadLogo(Context ctx) {
        try {
            UploadedFile uploadedFile = ctx.uploadedFile("logo");
            
            if (uploadedFile == null) {
                ctx.status(400).result("No file uploaded with key 'logo'.");
                return;
            }

            String filename = uploadedFile.filename();
            if (filename == null || !filename.toLowerCase().endsWith(".png")) {
                ctx.status(400).result("Invalid file format. Only PNG files are accepted.");
                return;
            }

            // Target directory configuration matching static resource paths
            File uploadDir = new File("src/main/resources/public/uploads");
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    ctx.status(500).result("Failed to create target upload directory.");
                    return;
                }
            }

            File destination = new File(uploadDir, "company-logo.png");
            
            // Perform safe file transfer using NIO paths
            Path targetPath = destination.toPath();
            Files.copy(uploadedFile.content(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            ctx.status(200).json(Collections.singletonMap("logoUrl", "/uploads/company-logo.png"));

        } catch (Exception e) {
            ctx.status(500).result("Failed to upload logo: " + e.getMessage());
        }
    }
}
