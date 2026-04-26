package com.campus.languageexchange;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

record LoginRequest(
    @NotBlank(message = "账号或邮箱不能为空")
    @Size(min = 4, max = 100, message = "账号或邮箱长度需为 4-100 位")
    String account,
    @NotBlank(message = "密码不能为空")
    String password
) {
}

record RegisterRequest(
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 50, message = "邮箱不能超过 50 位")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    String email,
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d~!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/\\\\|`]+$",
        message = "密码需同时包含字母和数字"
    )
    String password,
    @NotBlank(message = "请再次输入密码")
    String confirmPassword,
    @NotBlank(message = "角色不能为空")
    String role,
    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度需为 2-20 位")
    String name,
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
    String verificationCode
) {
    String account() {
        return email;
    }
}

record SendEmailCodeRequest(
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 50, message = "邮箱不能超过 50 位")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    String email
) {
}

record ResetPasswordRequest(
    @NotBlank String account,
    @NotBlank String newPassword,
    @NotBlank String confirmPassword
) {
}

record UpdateProfileRequest(
    @NotBlank String name,
    String gender,
    String school,
    String college,
    String grade,
    String avatar,
    @NotBlank String nativeLanguage,
    @NotBlank String targetLanguage,
    String languageLevel,
    @NotEmpty List<String> interestTags,
    List<String> targetTags,
    String availableTime,
    String preferredMode,
    String communicationGoal,
    String introduction
) {
}

record MatchFeedbackRequest(@NotBlank String action) {
}

record SendMessageRequest(
    @NotNull Long senderId,
    @NotBlank String messageType,
    @NotBlank String content,
    String fileUrl
) {
}

record CreatePostRequest(
    @NotNull Long userId,
    @NotBlank String content,
    List<String> images,
    List<String> topics
) {
}

record PostActionRequest(@NotNull Long userId, @NotBlank String actionType) {
}

record CreateCommentRequest(@NotNull Long userId, @NotBlank String content) {
}

record ActivitySignupRequest(@NotNull Long userId, @NotBlank String signupType) {
}

record ActivityReviewRequest(@NotNull Long userId, @NotNull @Min(1) @Max(5) Integer score, String content) {
}

record SaveActivityRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotBlank String activityType,
    String location,
    String liveUrl,
    String replayUrl,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    Integer limitCount,
    @NotBlank String status,
    @NotNull Long createdBy
) {
}

record ProcessActivitySignupRequest(@NotBlank String status) {
}

record UpdateUserStatusRequest(@NotBlank String status) {
}

record ProcessReportRequest(@NotBlank String status) {
}
