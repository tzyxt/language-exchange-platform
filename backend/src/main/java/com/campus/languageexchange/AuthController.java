package com.campus.languageexchange;

import jakarta.validation.Valid;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DataStore dataStore;

    public AuthController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", dataStore.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", dataStore.register(request));
    }

    @PostMapping("/email-code")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        dataStore.sendRegisterEmailCode(request);
        return ApiResponse.ok("验证码已发送", null);
    }

    @GetMapping("/check-account")
    public ApiResponse<Map<String, Object>> checkAccount(@RequestParam String account) {
        return ApiResponse.ok(dataStore.checkAccountAvailability(account));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        dataStore.resetPassword(request);
        return ApiResponse.ok("密码已重置", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MailException.class, IllegalStateException.class})
    public ApiResponse<Void> handleValidation(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            String message = validationException.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数不合法" : error.getDefaultMessage())
                .orElse("请求参数不合法");
            return ApiResponse.fail(message);
        }
        if (exception instanceof MailException) {
            return ApiResponse.fail(resolveMailErrorMessage(exception));
        }
        return ApiResponse.fail(exception.getMessage() == null ? "请求失败" : exception.getMessage());
    }

    private String resolveMailErrorMessage(Throwable exception) {
        String message = collectExceptionMessage(exception).toLowerCase();
        if (message.contains("authentication") || message.contains("auth")) {
            return "邮件发送失败：SMTP 认证失败，请检查发件邮箱或授权码。";
        }
        if (message.contains("mailfrom first")) {
            return "邮件发送失败：发件邮箱配置无效，请检查发件邮箱地址。";
        }
        if (message.contains("invalid addresses") || message.contains("address failed")) {
            return "邮件发送失败：邮箱地址格式不正确，请检查收件邮箱和发件邮箱。";
        }
        if (message.contains("timed out") || message.contains("connect")) {
            return "邮件发送失败：无法连接邮件服务器，请检查 SMTP 主机、端口或网络。";
        }
        return "邮件发送失败：请检查邮箱 SMTP 配置。";
    }

    private String collectExceptionMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(" | ");
                }
                builder.append(current.getMessage());
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}
