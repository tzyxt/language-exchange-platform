package com.campus.languageexchange;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/admin")
public class AdminController {

    private final DataStore dataStore;

    public AdminController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(dataStore.adminDashboard());
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users(@RequestParam(required = false) String status) {
        return ApiResponse.ok(dataStore.adminUsers(status));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.ok("用户状态已更新", dataStore.updateUserStatus(userId, request.status()));
    }

    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities() {
        return ApiResponse.ok(dataStore.adminActivities());
    }

    @PostMapping("/activities")
    public ApiResponse<Map<String, Object>> createActivity(@Valid @RequestBody SaveActivityRequest request) {
        return ApiResponse.ok("活动已创建", dataStore.saveActivity(null, request));
    }

    @PutMapping("/activities/{activityId}")
    public ApiResponse<Map<String, Object>> updateActivity(@PathVariable Long activityId, @Valid @RequestBody SaveActivityRequest request) {
        return ApiResponse.ok("活动已更新", dataStore.saveActivity(activityId, request));
    }

    @PatchMapping("/activities/{activityId}/signups/{signupId}")
    public ApiResponse<Map<String, Object>> processActivitySignup(@PathVariable Long activityId,
                                                                  @PathVariable Long signupId,
                                                                  @Valid @RequestBody ProcessActivitySignupRequest request) {
        return ApiResponse.ok("报名记录已处理", dataStore.processActivitySignup(activityId, signupId, request.status()));
    }

    @GetMapping("/reports")
    public ApiResponse<List<Map<String, Object>>> reports(@RequestParam(required = false) String status) {
        return ApiResponse.ok(dataStore.adminReports(status));
    }

    @PatchMapping("/reports/{reportId}")
    public ApiResponse<Map<String, Object>> processReport(@PathVariable Long reportId, @Valid @RequestBody ProcessReportRequest request) {
        return ApiResponse.ok("举报处理完成", dataStore.processReport(reportId, request.status()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}
