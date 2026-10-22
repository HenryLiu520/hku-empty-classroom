/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.web;

import hku.ec.service.AuthService;
import hku.ec.service.ReviewPhotoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 评价照片：上传（只能加在自己写的评价上）、删除、取图。 */
@RestController
@RequestMapping("/api")
public class ReviewPhotoController {

    private final ReviewPhotoService photos;
    private final AuthService auth;

    public ReviewPhotoController(ReviewPhotoService photos, AuthService auth) {
        this.photos = photos;
        this.auth = auth;
    }

    /** 上传一张照片（multipart，字段名 file）。只有评价作者本人能加。 */
    @PostMapping("/reviews/{id}/photos")
    public ResponseEntity<?> upload(@PathVariable Long id,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthService.Session s;
        try {
            s = auth.require(Tokens.from(authHeader), null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(new Dtos.ApiError("UNAUTHORIZED", "Sign in first"));
        }
        try {
            return ResponseEntity.ok(photos.add(id, s.username(), file,
                    AuthService.atLeast(s.role(), "superadmin")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_PHOTO", e.getMessage()));
        }
    }

    /** 删除一张照片（作者本人或超级管理员） */
    @DeleteMapping("/review-photos/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthService.Session s;
        try {
            s = auth.require(Tokens.from(authHeader), null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(new Dtos.ApiError("UNAUTHORIZED", "Sign in first"));
        }
        try {
            photos.delete(id, s.username(), AuthService.atLeast(s.role(), "superadmin"));
            return ResponseEntity.ok(Map.of("deleted", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_PHOTO", e.getMessage()));
        }
    }

    /** 取图。任何人都能看（评价本身也是公开的）。 */
    @GetMapping("/review-photos/{id}")
    public ResponseEntity<Resource> serve(@PathVariable Long id) {
        try {
            ReviewPhotoService.PhotoFile f = photos.load(id);
            MediaType type = MediaType.parseMediaType(f.contentType());
            return ResponseEntity.ok()
                    .contentType(type)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    // 只当作图片内联显示，绝不让浏览器猜类型去执行别的东西
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new FileSystemResource(f.path()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
