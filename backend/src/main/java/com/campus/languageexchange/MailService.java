package com.campus.languageexchange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(
        JavaMailSender mailSender,
        @Value("${app.mail.from:${spring.mail.username:}}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
    }

    public void sendVerificationCode(String toEmail, String code) {
        if (fromAddress.isBlank()) {
            throw new IllegalStateException("邮件发件箱未配置，请先设置系统发件邮箱。");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Language Exchange Platform 验证码");
        message.setText("""
            您的注册验证码是：%s

            验证码 10 分钟内有效。如果不是您本人操作，请忽略这封邮件。
            """.formatted(code));
        mailSender.send(message);
    }
}
