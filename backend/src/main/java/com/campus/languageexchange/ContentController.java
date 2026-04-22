package com.campus.languageexchange;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ContentController {

    private static final Path CHAT_UPLOAD_DIR = Paths.get("uploads", "chat").toAbsolutePath().normalize();

    private final DataStore dataStore;

    public ContentController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping("/chats/{userId}/sessions")
    public ApiResponse<List<Map<String, Object>>> sessions(@PathVariable Long userId) {
        return ApiResponse.ok(dataStore.listSessions(userId));
    }

    @GetMapping("/chats/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> sessionMessages(@PathVariable Long sessionId, @RequestParam Long userId) {
        return ApiResponse.ok(dataStore.getSessionMessages(sessionId, userId));
    }

    @PostMapping("/chats/sessions/{sessionId}/messages")
    public ApiResponse<Map<String, Object>> sendMessage(@PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok("消息发送成功", dataStore.sendMessage(sessionId, request));
    }

    @PostMapping(value = "/uploads/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadChatFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        Files.createDirectories(CHAT_UPLOAD_DIR);

        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String storedName = UUID.randomUUID() + extension;
        Path target = CHAT_UPLOAD_DIR.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/chat/")
            .path(storedName)
            .toUriString();

        return ApiResponse.ok("上传成功", Map.of(
            "url", url,
            "filename", originalName,
            "contentType", file.getContentType() == null ? "application/octet-stream" : file.getContentType()
        ));
    }

    @GetMapping("/posts")
    public ApiResponse<List<Map<String, Object>>> posts(@RequestParam(defaultValue = "latest") String sortBy) {
        return ApiResponse.ok(dataStore.listPosts(sortBy));
    }

    @PostMapping("/posts")
    public ApiResponse<Map<String, Object>> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.ok("动态发布成功", dataStore.createPost(request));
    }

    @PostMapping("/posts/{postId}/actions")
    public ApiResponse<Map<String, Object>> postAction(@PathVariable Long postId, @Valid @RequestBody PostActionRequest request) {
        return ApiResponse.ok(dataStore.actOnPost(postId, request));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Map<String, Object>> comment(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.ok("评论成功", dataStore.commentPost(postId, request));
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(@RequestParam String keyword) {
        return ApiResponse.ok(dataStore.search(keyword));
    }

    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities(@RequestParam Long userId,
                                                             @RequestParam(required = false) String activityType,
                                                             @RequestParam(required = false) String status) {
        return ApiResponse.ok(dataStore.listActivities(activityType, status, userId));
    }

    @GetMapping("/activities/{activityId}")
    public ApiResponse<Map<String, Object>> activityDetail(@PathVariable Long activityId, @RequestParam Long userId) {
        return ApiResponse.ok(dataStore.getActivityDetail(activityId, userId));
    }

    @PostMapping("/activities/{activityId}/signups")
    public ApiResponse<Map<String, Object>> signup(@PathVariable Long activityId, @Valid @RequestBody ActivitySignupRequest request) {
        return ApiResponse.ok("报名成功", dataStore.signupActivity(activityId, request));
    }

    @DeleteMapping("/activities/{activityId}/signups")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long activityId, @RequestParam Long userId) {
        return ApiResponse.ok("已取消报名", dataStore.cancelSignup(activityId, userId));
    }

    @PostMapping("/activities/{activityId}/reviews")
    public ApiResponse<Map<String, Object>> review(@PathVariable Long activityId, @Valid @RequestBody ActivityReviewRequest request) {
        return ApiResponse.ok("评价已提交", dataStore.reviewActivity(activityId, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}
