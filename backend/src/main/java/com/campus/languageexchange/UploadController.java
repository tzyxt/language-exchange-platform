package com.campus.languageexchange;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private static final Path CHAT_UPLOAD_DIR = Paths.get("uploads", "chat").toAbsolutePath().normalize();

    @GetMapping("/chat/{filename:.+}")
    public ResponseEntity<Resource> getChatUpload(@PathVariable String filename) throws Exception {
        Path target = CHAT_UPLOAD_DIR.resolve(filename).normalize();
        if (!target.startsWith(CHAT_UPLOAD_DIR) || !Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = toResource(target);
        String contentType = Files.probeContentType(target);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + target.getFileName() + "\"")
            .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
            .body(resource);
    }

    private Resource toResource(Path target) throws MalformedURLException {
        return new UrlResource(target.toUri());
    }
}
