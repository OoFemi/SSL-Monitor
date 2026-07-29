package com.uptime;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

public class AdminController {

    public static void uploadLogo(Context ctx) {
        try {
            UploadedFile uploadedFile = ctx.uploadedFile("logo");
            if (uploadedFile != null && uploadedFile.filename().toLowerCase().endsWith(".png")) {
                File uploadDir = new File("resources/public/uploads");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                File destination = new File(uploadDir, "company-logo.png");
                Files.copy(uploadedFile.content(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                ctx.status(200).json(Collections.singletonMap("logoUrl", "/uploads/company-logo.png"));
            } else {
                ctx.status(400).result("Invalid file format. Only PNG files are accepted.");
            }
        } catch (Exception e) {
            ctx.status(500).result("Failed to upload logo: " + e.getMessage());
        }
    }
}
