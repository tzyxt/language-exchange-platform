package com.campus.languageexchange;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final DataStore dataStore;

    public UserController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home(@RequestParam Long userId) {
        return ApiResponse.ok(dataStore.homeSummary(userId));
    }

    @GetMapping("/profile/{userId}")
    public ApiResponse<Map<String, Object>> profile(@PathVariable Long userId) {
        return ApiResponse.ok(dataStore.getProfile(userId));
    }

    @PutMapping("/profile/{userId}")
    public ApiResponse<Map<String, Object>> updateProfile(@PathVariable Long userId, @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("资料更新成功", dataStore.updateProfile(userId, request));
    }

    @GetMapping("/matches/{userId}")
    public ApiResponse<List<Map<String, Object>>> matches(@PathVariable Long userId,
                                                          @RequestParam(required = false) String language,
                                                          @RequestParam(required = false) String interest,
                                                          @RequestParam(required = false) String college,
                                                          @RequestParam(required = false) String sortBy) {
        return ApiResponse.ok(dataStore.listMatches(userId, language, interest, college, sortBy));
    }

    @GetMapping("/users/{targetUserId}")
    public ApiResponse<Map<String, Object>> userDetail(@PathVariable Long targetUserId, @RequestParam Long viewerId) {
        return ApiResponse.ok(dataStore.getUserDetail(viewerId, targetUserId));
    }

    @PostMapping("/matches/{userId}/{targetUserId}/connect")
    public ApiResponse<Map<String, Object>> connect(@PathVariable Long userId, @PathVariable Long targetUserId) {
        return ApiResponse.ok("已建立联系", dataStore.connectUsers(userId, targetUserId));
    }

    @PostMapping("/matches/{userId}/{targetUserId}/feedback")
    public ApiResponse<Map<String, Object>> feedback(@PathVariable Long userId,
                                                     @PathVariable Long targetUserId,
                                                     @Valid @RequestBody MatchFeedbackRequest request) {
        return ApiResponse.ok("反馈已记录", dataStore.feedbackMatch(userId, targetUserId, request.action()));
    }

    @GetMapping("/me/{userId}/overview")
    public ApiResponse<Map<String, Object>> overview(@PathVariable Long userId) {
        return ApiResponse.ok(dataStore.myOverview(userId));
    }

    @DeleteMapping("/me/{userId}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long userId) {
        dataStore.deleteAccount(userId);
        return ApiResponse.ok("账号已注销", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOtherException(Exception exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank() ? "请求失败" : exception.getMessage();
        return ApiResponse.fail(message);
    }
}
