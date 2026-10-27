package com.cafe.backend.controller.admin;

import com.cafe.backend.security.RateLimitService;
import com.cafe.backend.service.storage.ImageUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

/*
 * Admin photo upload for menu items and categories.
 * ADMIN only (every /api/admin/** URL requires the ADMIN role).
 *
 * POST /api/admin/uploads/image?folder=menu      (form field "file")
 * POST /api/admin/uploads/image?folder=category
 * Response: { "url": "https://..." }  -> saved as imageUrl by the normal save call
 */
@RestController
@RequestMapping("/api/admin/uploads")
public class AdminImageUploadController {

    private final ImageUploadService imageUploadService;
    private final RateLimitService rateLimitService;

    public AdminImageUploadController(
            ImageUploadService imageUploadService,
            RateLimitService rateLimitService
    ) {
        this.imageUploadService = imageUploadService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") String folder,
            HttpServletRequest httpRequest
    ) {
        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:image-upload:" + httpRequest.getRemoteAddr(),
                30,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        String url = imageUploadService.upload(file, folder);

        return ResponseEntity.ok(Map.of("url", url));
    }
}
