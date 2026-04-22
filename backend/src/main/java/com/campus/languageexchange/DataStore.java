package com.campus.languageexchange;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DataStore {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA);

    private final AtomicLong userId = new AtomicLong(1000);
    private final AtomicLong matchId = new AtomicLong(2000);
    private final AtomicLong sessionId = new AtomicLong(3000);
    private final AtomicLong messageId = new AtomicLong(4000);
    private final AtomicLong postId = new AtomicLong(5000);
    private final AtomicLong commentId = new AtomicLong(6000);
    private final AtomicLong activityId = new AtomicLong(7000);
    private final AtomicLong reviewId = new AtomicLong(8000);
    private final AtomicLong reportId = new AtomicLong(9000);
    private final AtomicLong noticeId = new AtomicLong(10000);

    private final Map<Long, UserData> users = new LinkedHashMap<>();
    private final List<MatchData> matches = new ArrayList<>();
    private final Map<Long, ChatSessionData> sessions = new LinkedHashMap<>();
    private final Map<Long, PostData> posts = new LinkedHashMap<>();
    private final Map<Long, ActivityData> activities = new LinkedHashMap<>();
    private final List<ReviewData> reviews = new ArrayList<>();
    private final Map<Long, ReportData> reports = new LinkedHashMap<>();
    private final Map<Long, List<NotificationData>> notifications = new HashMap<>();
    private final JdbcTemplate jdbcTemplate;

    public DataStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        seed();
    }

    public synchronized Map<String, Object> login(LoginRequest request) {
        UserData user = users.values().stream()
            .filter(item -> item.account.equalsIgnoreCase(request.account()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        if (!Objects.equals(user.password, request.password())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if ("DISABLED".equalsIgnoreCase(user.status)) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        user.lastLoginTime = LocalDateTime.now();
        persistUser(user);
        return Map.of("token", "demo-token-" + user.id, "currentUser", userMap(user, true));
    }

    public synchronized Map<String, Object> register(RegisterRequest request) {
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        boolean exists = users.values().stream().anyMatch(item -> item.account.equalsIgnoreCase(request.account()));
        if (exists) {
            throw new IllegalArgumentException("账号已存在");
        }
        UserData user = new UserData();
        user.id = userId.incrementAndGet();
        user.account = request.account();
        user.password = request.password();
        user.role = normalizeRole(request.role());
        user.name = request.name();
        user.gender = "未知";
        user.school = "国际交流学院";
        user.college = "语言与文化中心";
        user.grade = "2026";
        user.avatar = avatarUrl(request.account());
        user.status = "NORMAL";
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        user.profileCompleted = false;
        user.profile = new UserProfileData("中文", "English", "B1",
            new ArrayList<>(List.of("电影", "音乐")), new ArrayList<>(List.of("口语练习")),
            "周末晚上", "线上聊天", "口语练习", "刚加入平台，期待认识新的语言伙伴。");
        users.put(user.id, user);
        persistUser(user);
        notifyUser(user.id, "欢迎加入平台", "请先完善资料，以获取更准确的智能推荐。", "SYSTEM");
        return Map.of("token", "demo-token-" + user.id, "currentUser", userMap(user, true));
    }

    public synchronized Map<String, Object> checkAccountAvailability(String account) {
        String normalized = account == null ? "" : account.trim();
        boolean available = users.values().stream().noneMatch(item -> item.account.equalsIgnoreCase(normalized));
        return Map.of(
            "account", normalized,
            "available", available
        );
    }

    public synchronized void resetPassword(ResetPasswordRequest request) {
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        UserData user = users.values().stream()
            .filter(item -> item.account.equalsIgnoreCase(request.account()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        user.password = request.newPassword();
        user.updatedAt = LocalDateTime.now();
        persistUser(user);
    }

    public synchronized Map<String, Object> homeSummary(Long id) {
        UserData user = requireUser(id);
        return Map.of(
            "hero", Map.of("title", "连接语言伙伴，也连接文化理解", "subtitle", "语言互助、活动参与、社区分享与跨文化交流一站式完成。"),
            "announcements", List.of(
                Map.of("title", "四月跨文化交流月", "content", "本周新增双语电影夜、城市文化漫步等活动。"),
                Map.of("title", "推荐算法升级", "content", "兴趣标签和交流时间的权重已优化。")
            ),
            "hotTopics", List.of("校园生活", "口语练习", "文化交流", "城市探索", "学习打卡"),
            "recommendedMatches", listMatches(id, null, null, null, "score").stream().limit(3).toList(),
            "hotPosts", listPosts("hot").stream().limit(3).toList(),
            "upcomingActivities", listActivities(null, null, id).stream().limit(3).toList(),
            "currentUser", userMap(user, true)
        );
    }

    public synchronized Map<String, Object> getProfile(Long id) {
        UserData user = requireUser(id);
        Map<String, Object> data = new LinkedHashMap<>(userMap(user, true));
        data.put("notifications", notifications.getOrDefault(id, List.of()).stream().map(this::notificationMap).toList());
        return data;
    }

    public synchronized Map<String, Object> updateProfile(Long id, UpdateProfileRequest request) {
        UserData user = requireUser(id);
        user.name = request.name();
        user.gender = safe(request.gender());
        user.school = safe(request.school());
        user.college = safe(request.college());
        user.grade = safe(request.grade());
        user.avatar = request.avatar() == null || request.avatar().isBlank() ? user.avatar : request.avatar();
        user.profile = new UserProfileData(
            request.nativeLanguage(),
            request.targetLanguage(),
            safe(request.languageLevel()),
            new ArrayList<>(request.interestTags()),
            new ArrayList<>(emptyIfNull(request.targetTags())),
            safe(request.availableTime()),
            safe(request.preferredMode()),
            safe(request.communicationGoal()),
            safe(request.introduction())
        );
        user.profileCompleted = true;
        user.updatedAt = LocalDateTime.now();
        persistUser(user);
        return getProfile(id);
    }

    public synchronized List<Map<String, Object>> listMatches(Long id, String language, String interest, String college, String sortBy) {
        UserData current = requireUser(id);
        Comparator<Map<String, Object>> comparator = Comparator.comparingDouble(item -> Double.parseDouble(String.valueOf(item.get("matchScore"))));
        if ("latest".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(item -> String.valueOf(item.get("createdAt")));
        }
        return users.values().stream()
            .filter(item -> !Objects.equals(item.id, id))
            .filter(item -> "USER".equalsIgnoreCase(item.role))
            .filter(item -> !"DISABLED".equalsIgnoreCase(item.status))
            .map(candidate -> toMatchMap(current, candidate))
            .filter(item -> language == null || containsAny(String.valueOf(item.get("nativeLanguage")), String.valueOf(item.get("targetLanguage")), language))
            .filter(item -> interest == null || ((List<?>) item.get("interestTags")).stream().map(String::valueOf).anyMatch(tag -> tag.contains(interest)))
            .filter(item -> college == null || String.valueOf(item.get("college")).contains(college))
            .sorted(comparator.reversed())
            .toList();
    }

    public synchronized Map<String, Object> getUserDetail(Long viewerId, Long targetUserId) {
        UserData viewer = requireUser(viewerId);
        UserData target = requireUser(targetUserId);
        MatchScore score = calculateScore(viewer, target);
        Map<String, Object> data = new LinkedHashMap<>(userMap(target, false));
        data.put("matchScore", score.score);
        data.put("matchReasons", score.reasons);
        data.put("publicReviews", reviews.stream()
            .filter(review -> Objects.equals(review.toUserId, targetUserId))
            .sorted(Comparator.comparing((ReviewData review) -> review.createdAt).reversed())
            .map(this::reviewMap)
            .limit(3)
            .toList());
        return data;
    }

    public synchronized Map<String, Object> connectUsers(Long userIdValue, Long targetUserId) {
        UserData source = requireUser(userIdValue);
        UserData target = requireUser(targetUserId);
        MatchData match = ensureMatch(source, target);
        match.status = "CONNECTED";
        match.updatedAt = LocalDateTime.now();
        ChatSessionData session = ensureSession(userIdValue, targetUserId);
        notifyUser(targetUserId, "新的联系申请", source.name + " 已向你发起交流联系。", "MATCH");
        return Map.of("session", sessionMap(session, userIdValue), "match", matchMap(match, target));
    }

    public synchronized Map<String, Object> feedbackMatch(Long userIdValue, Long targetUserId, String action) {
        MatchData match = ensureMatch(requireUser(userIdValue), requireUser(targetUserId));
        match.status = action.toUpperCase(Locale.ROOT);
        match.updatedAt = LocalDateTime.now();
        return matchMap(match, requireUser(targetUserId));
    }

    public synchronized List<Map<String, Object>> listSessions(Long userIdValue) {
        requireUser(userIdValue);
        return sessions.values().stream()
            .filter(session -> Objects.equals(session.userId, userIdValue) || Objects.equals(session.peerUserId, userIdValue))
            .sorted(Comparator.comparing((ChatSessionData session) -> session.lastMessageTime).reversed())
            .map(session -> sessionMap(session, userIdValue))
            .toList();
    }

    public synchronized Map<String, Object> getSessionMessages(Long sessionIdValue, Long userIdValue) {
        ChatSessionData session = requireSession(sessionIdValue);
        if (!Objects.equals(session.userId, userIdValue) && !Objects.equals(session.peerUserId, userIdValue)) {
            throw new IllegalArgumentException("无权查看该会话");
        }
        session.messages.stream().filter(msg -> !Objects.equals(msg.senderId, userIdValue)).forEach(msg -> msg.read = true);
        return Map.of("session", sessionMap(session, userIdValue), "messages", session.messages.stream().map(this::messageMap).toList());
    }

    public synchronized Map<String, Object> sendMessage(Long sessionIdValue, SendMessageRequest request) {
        ChatSessionData session = requireSession(sessionIdValue);
        if (!Objects.equals(session.userId, request.senderId()) && !Objects.equals(session.peerUserId, request.senderId())) {
            throw new IllegalArgumentException("无权在该会话中发言");
        }
        ChatMessageData message = new ChatMessageData();
        message.id = messageId.incrementAndGet();
        message.sessionId = sessionIdValue;
        message.senderId = request.senderId();
        message.messageType = request.messageType();
        message.content = request.content();
        message.fileUrl = request.fileUrl();
        message.read = false;
        message.revoked = false;
        message.sendTime = LocalDateTime.now();
        session.messages.add(message);
        session.lastMessageTime = message.sendTime;
        notifyUser(peerId(session, request.senderId()), "新消息提醒", requireUser(request.senderId()).name + " 向你发送了一条消息。", "CHAT");
        return messageMap(message);
    }

    public synchronized List<Map<String, Object>> listPosts(String sortBy) {
        Comparator<PostData> comparator = "hot".equalsIgnoreCase(sortBy)
            ? Comparator.comparingInt((PostData post) -> post.likeUserIds.size() * 2 + post.comments.size() * 3).reversed()
            : Comparator.comparing((PostData post) -> post.createdAt).reversed();
        return posts.values().stream().sorted(comparator).map(this::postMap).toList();
    }

    public synchronized Map<String, Object> createPost(CreatePostRequest request) {
        requireUser(request.userId());
        PostData post = new PostData();
        post.id = postId.incrementAndGet();
        post.userId = request.userId();
        post.content = request.content();
        post.images = new ArrayList<>(emptyIfNull(request.images()));
        post.topics = new ArrayList<>(emptyIfNull(request.topics()));
        post.status = "NORMAL";
        post.createdAt = LocalDateTime.now();
        post.updatedAt = LocalDateTime.now();
        posts.put(post.id, post);
        return postMap(post);
    }

    public synchronized Map<String, Object> actOnPost(Long postIdValue, PostActionRequest request) {
        PostData post = requirePost(postIdValue);
        requireUser(request.userId());
        String action = request.actionType().toUpperCase(Locale.ROOT);
        if ("LIKE".equals(action)) {
            toggle(post.likeUserIds, request.userId());
        } else if ("FAVORITE".equals(action)) {
            toggle(post.favoriteUserIds, request.userId());
        } else if ("SHARE".equals(action)) {
            post.shareCount++;
        }
        post.updatedAt = LocalDateTime.now();
        return postMap(post);
    }

    public synchronized Map<String, Object> commentPost(Long postIdValue, CreateCommentRequest request) {
        PostData post = requirePost(postIdValue);
        requireUser(request.userId());
        PostCommentData comment = new PostCommentData();
        comment.id = commentId.incrementAndGet();
        comment.postId = postIdValue;
        comment.userId = request.userId();
        comment.content = request.content();
        comment.createdAt = LocalDateTime.now();
        post.comments.add(comment);
        return postMap(post);
    }

    public synchronized Map<String, Object> search(String keyword) {
        String q = Optional.ofNullable(keyword).orElse("").trim();
        if (q.isEmpty()) {
            return Map.of("users", List.of(), "posts", List.of(), "topics", List.of());
        }
        List<String> topics = posts.values().stream().flatMap(post -> post.topics.stream()).distinct().filter(item -> item.contains(q)).toList();
        return Map.of(
            "users", users.values().stream().filter(user -> user.name.contains(q) || user.account.contains(q)).map(user -> userMap(user, false)).limit(6).toList(),
            "posts", posts.values().stream().filter(post -> post.content.contains(q) || post.topics.stream().anyMatch(topic -> topic.contains(q))).map(this::postMap).limit(6).toList(),
            "topics", topics
        );
    }

    public synchronized List<Map<String, Object>> listActivities(String activityType, String status, Long userIdValue) {
        requireUser(userIdValue);
        return activities.values().stream()
            .filter(item -> activityType == null || item.activityType.equalsIgnoreCase(activityType))
            .filter(item -> status == null || item.status.equalsIgnoreCase(status))
            .sorted(Comparator.comparing((ActivityData item) -> item.startTime))
            .map(item -> activityMap(item, userIdValue))
            .toList();
    }

    public synchronized Map<String, Object> getActivityDetail(Long activityIdValue, Long userIdValue) {
        ActivityData activity = requireActivity(activityIdValue);
        Map<String, Object> data = new LinkedHashMap<>(activityMap(activity, userIdValue));
        data.put("participants", activity.signups.stream().map(this::signupMap).toList());
        data.put("reviews", activity.reviews.stream().map(this::activityReviewMap).toList());
        return data;
    }

    public synchronized Map<String, Object> signupActivity(Long activityIdValue, ActivitySignupRequest request) {
        ActivityData activity = requireActivity(activityIdValue);
        requireUser(request.userId());
        long successCount = activity.signups.stream().filter(signup -> "SUCCESS".equalsIgnoreCase(signup.signupStatus)).count();
        if (activity.limitCount != null && successCount >= activity.limitCount) {
            throw new IllegalArgumentException("活动报名人数已满");
        }
        boolean exists = activity.signups.stream()
            .anyMatch(signup -> Objects.equals(signup.userId, request.userId()) && "SUCCESS".equalsIgnoreCase(signup.signupStatus));
        if (exists) {
            throw new IllegalArgumentException("你已报名该活动");
        }
        ActivitySignupData signup = new ActivitySignupData();
        signup.id = reviewId.incrementAndGet();
        signup.activityId = activityIdValue;
        signup.userId = request.userId();
        signup.signupType = request.signupType();
        signup.signupStatus = "SUCCESS";
        signup.signupTime = LocalDateTime.now();
        activity.signups.add(signup);
        notifyUser(request.userId(), "活动报名成功", "你已成功报名《" + activity.title + "》。", "ACTIVITY");
        return activityMap(activity, request.userId());
    }

    public synchronized Map<String, Object> cancelSignup(Long activityIdValue, Long userIdValue) {
        ActivityData activity = requireActivity(activityIdValue);
        activity.signups.stream()
            .filter(signup -> Objects.equals(signup.userId, userIdValue) && "SUCCESS".equalsIgnoreCase(signup.signupStatus))
            .findFirst()
            .ifPresent(signup -> signup.signupStatus = "CANCELLED");
        return activityMap(activity, userIdValue);
    }

    public synchronized Map<String, Object> reviewActivity(Long activityIdValue, ActivityReviewRequest request) {
        ActivityData activity = requireActivity(activityIdValue);
        requireUser(request.userId());
        ActivityReviewData review = new ActivityReviewData();
        review.id = reviewId.incrementAndGet();
        review.activityId = activityIdValue;
        review.userId = request.userId();
        review.score = request.score();
        review.content = request.content() == null || request.content().isBlank() ? "活动体验不错，期待下次继续参加。" : request.content();
        review.createdAt = LocalDateTime.now();
        activity.reviews.add(review);
        return activityMap(activity, request.userId());
    }

    public synchronized Map<String, Object> myOverview(Long userIdValue) {
        long favoriteCount = posts.values().stream().filter(post -> post.favoriteUserIds.contains(userIdValue)).count();
        long activityCount = activities.values().stream().flatMap(item -> item.signups.stream())
            .filter(signup -> Objects.equals(signup.userId, userIdValue) && "SUCCESS".equalsIgnoreCase(signup.signupStatus)).count();
        return Map.of(
            "profile", getProfile(userIdValue),
            "stats", Map.of(
                "matchCount", matches.stream().filter(match -> Objects.equals(match.userId, userIdValue) && "CONNECTED".equalsIgnoreCase(match.status)).count(),
                "activityCount", activityCount,
                "favoriteCount", favoriteCount,
                "reviewScore", averageScore(userIdValue)
            ),
            "recentMatches", listMatches(userIdValue, null, null, null, "score").stream().limit(4).toList(),
            "recentSessions", listSessions(userIdValue).stream().limit(4).toList(),
            "notifications", notifications.getOrDefault(userIdValue, List.of()).stream().map(this::notificationMap).toList()
        );
    }

    public synchronized Map<String, Object> adminDashboard() {
        return Map.of(
            "stats", Map.of(
                "userCount", users.values().stream().filter(user -> "USER".equalsIgnoreCase(user.role)).count(),
                "activityCount", activities.size(),
                "matchCount", matches.size(),
                "postCount", posts.size(),
                "reportCount", reports.size()
            ),
            "recentReports", reports.values().stream().sorted(Comparator.comparing((ReportData item) -> item.createdAt).reversed()).map(this::reportMap).limit(5).toList(),
            "recentActivities", activities.values().stream().sorted(Comparator.comparing((ActivityData item) -> item.startTime)).map(item -> activityMap(item, item.createdBy)).limit(5).toList()
        );
    }

    public synchronized List<Map<String, Object>> adminUsers(String status) {
        return users.values().stream()
            .filter(user -> "USER".equalsIgnoreCase(user.role))
            .filter(user -> status == null || user.status.equalsIgnoreCase(status))
            .sorted(Comparator.comparing((UserData user) -> user.createdAt).reversed())
            .map(user -> userMap(user, true))
            .toList();
    }

    public synchronized Map<String, Object> updateUserStatus(Long userIdValue, String status) {
        UserData user = requireUser(userIdValue);
        user.status = status.toUpperCase(Locale.ROOT);
        user.updatedAt = LocalDateTime.now();
        persistUser(user);
        return userMap(user, true);
    }

    public synchronized List<Map<String, Object>> adminActivities() {
        return activities.values().stream()
            .sorted(Comparator.comparing((ActivityData item) -> item.startTime))
            .map(item -> activityMap(item, item.createdBy))
            .toList();
    }

    public synchronized Map<String, Object> saveActivity(Long activityIdValue, SaveActivityRequest request) {
        ActivityData activity = activityIdValue == null ? new ActivityData() : requireActivity(activityIdValue);
        if (activityIdValue == null) {
            activity.id = activityId.incrementAndGet();
            activity.signups = new ArrayList<>();
            activity.reviews = new ArrayList<>();
        }
        activity.title = request.title();
        activity.content = request.content();
        activity.activityType = request.activityType();
        activity.location = request.location();
        activity.liveUrl = request.liveUrl();
        activity.replayUrl = request.replayUrl();
        activity.startTime = request.startTime();
        activity.endTime = request.endTime();
        activity.limitCount = request.limitCount();
        activity.status = request.status();
        activity.createdBy = request.createdBy();
        activities.put(activity.id, activity);
        return activityMap(activity, activity.createdBy);
    }

    public synchronized List<Map<String, Object>> adminReports(String status) {
        return reports.values().stream()
            .filter(report -> status == null || report.status.equalsIgnoreCase(status))
            .sorted(Comparator.comparing((ReportData item) -> item.createdAt).reversed())
            .map(this::reportMap)
            .toList();
    }

    public synchronized Map<String, Object> processReport(Long reportIdValue, String status) {
        ReportData report = requireReport(reportIdValue);
        report.status = status.toUpperCase(Locale.ROOT);
        return reportMap(report);
    }

    private void initializeUsers() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
        if (count != null && count > 0) {
            loadUsersFromDatabase();
            return;
        }
    }

    private void loadUsersFromDatabase() {
        users.clear();
        jdbcTemplate.query("""
            SELECT u.id, u.account, u.password, u.role, u.name, u.gender, u.school, u.college, u.grade,
                   u.avatar, u.status, u.last_login_time, u.created_at, u.updated_at,
                   p.native_language, p.target_language, p.language_level, p.interest_tags, p.target_tags,
                   p.available_time, p.preferred_mode, p.communication_goal, p.introduction
            FROM user u
            LEFT JOIN user_profile p ON p.user_id = u.id
            ORDER BY u.id
            """, rs -> {
            UserData user = new UserData();
            user.id = rs.getLong("id");
            user.account = rs.getString("account");
            user.password = rs.getString("password");
            user.role = rs.getString("role");
            user.name = rs.getString("name");
            user.gender = rs.getString("gender");
            user.school = rs.getString("school");
            user.college = rs.getString("college");
            user.grade = rs.getString("grade");
            user.avatar = rs.getString("avatar");
            user.status = rs.getString("status");
            user.lastLoginTime = toLocalDateTime(rs.getTimestamp("last_login_time"));
            user.createdAt = toLocalDateTime(rs.getTimestamp("created_at"));
            user.updatedAt = toLocalDateTime(rs.getTimestamp("updated_at"));
            user.profile = new UserProfileData(
                defaultIfBlank(rs.getString("native_language"), "中文"),
                defaultIfBlank(rs.getString("target_language"), "English"),
                defaultIfBlank(rs.getString("language_level"), "B1"),
                parseTags(rs.getString("interest_tags")),
                parseTags(rs.getString("target_tags")),
                defaultIfBlank(rs.getString("available_time"), "周末晚上"),
                defaultIfBlank(rs.getString("preferred_mode"), "线上聊天"),
                defaultIfBlank(rs.getString("communication_goal"), "口语练习"),
                defaultIfBlank(rs.getString("introduction"), "")
            );
            user.profileCompleted = rs.getString("native_language") != null;
            users.put(user.id, user);
        });
        long maxId = users.keySet().stream().mapToLong(Long::longValue).max().orElse(1000L);
        userId.set(maxId);
    }

    private void persistUser(UserData user) {
        jdbcTemplate.update("""
            INSERT INTO user (id, account, password, role, name, gender, school, college, grade, avatar, status, last_login_time, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              account = VALUES(account),
              password = VALUES(password),
              role = VALUES(role),
              name = VALUES(name),
              gender = VALUES(gender),
              school = VALUES(school),
              college = VALUES(college),
              grade = VALUES(grade),
              avatar = VALUES(avatar),
              status = VALUES(status),
              last_login_time = VALUES(last_login_time),
              created_at = VALUES(created_at),
              updated_at = VALUES(updated_at)
            """,
            user.id, user.account, user.password, user.role, user.name, user.gender, user.school, user.college, user.grade,
            user.avatar, user.status, toTimestamp(user.lastLoginTime), toTimestamp(user.createdAt), toTimestamp(user.updatedAt)
        );
        jdbcTemplate.update("""
            INSERT INTO user_profile (user_id, native_language, target_language, language_level, interest_tags, target_tags, available_time, preferred_mode, communication_goal, introduction)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              native_language = VALUES(native_language),
              target_language = VALUES(target_language),
              language_level = VALUES(language_level),
              interest_tags = VALUES(interest_tags),
              target_tags = VALUES(target_tags),
              available_time = VALUES(available_time),
              preferred_mode = VALUES(preferred_mode),
              communication_goal = VALUES(communication_goal),
              introduction = VALUES(introduction)
            """,
            user.id,
            user.profile.nativeLanguage,
            user.profile.targetLanguage,
            user.profile.languageLevel,
            String.join(",", user.profile.interestTags),
            String.join(",", user.profile.targetTags),
            user.profile.availableTime,
            user.profile.preferredMode,
            user.profile.communicationGoal,
            user.profile.introduction
        );
    }

    private void seed() {
        initializeUsers();
        UserData admin = createUser("admin", "123456", "ADMIN", "平台管理员", "女", "国际交流学院", "运营中心", "教师", "中文", "English", "C1",
            List.of("运营", "活动策划"), List.of("平台治理"), "工作日上午", "后台管理", "平台运营", "负责平台运行与审核。");
        UserData lily = createUser("lily.chen", "123456", "USER", "陈莉", "女", "华东外国语大学", "英语学院", "2023", "中文", "English", "B2",
            List.of("电影", "旅行", "摄影", "咖啡"), List.of("口语练习", "文化交流"), "周末晚上", "线上聊天", "口语练习", "喜欢和不同文化背景的朋友交流。");
        UserData alex = createUser("alex.smith", "123456", "USER", "Alex Smith", "男", "East China University", "Chinese Program", "2024", "English", "中文", "HSK4",
            List.of("电影", "城市探索", "足球", "摄影"), List.of("口语练习", "校园生活"), "周末晚上", "线上聊天", "口语练习", "希望提升中文表达，也想认识本地朋友。");
        UserData maria = createUser("maria.lopez", "123456", "USER", "Maria Lopez", "女", "International College", "Business School", "2025", "Spanish", "中文", "HSK5",
            List.of("音乐", "旅行", "美食", "舞蹈"), List.of("文化交流", "结交朋友"), "工作日晚上", "语音通话", "文化交流", "热爱音乐和中国美食。");
        UserData zhou = createUser("zhou.ming", "123456", "USER", "周鸣", "男", "华东外国语大学", "国际文化学院", "2022", "中文", "Spanish", "A2",
            List.of("音乐", "阅读", "美食", "篮球"), List.of("考试备考", "文化交流"), "工作日晚上", "语音通话", "文化交流", "想学习西语，也愿意介绍中国校园文化。");
        UserData nora = createUser("nora.khan", "123456", "USER", "Nora Khan", "女", "International College", "Design School", "2023", "English", "中文", "HSK3",
            List.of("设计", "电影", "阅读", "摄影"), List.of("结交朋友", "文化交流"), "周末下午", "线下活动", "结交朋友", "正在适应校园生活。");

        ensureMatch(lily, alex);
        ensureMatch(zhou, maria);
        ensureMatch(lily, nora);

        ChatSessionData chat = ensureSession(lily.id, alex.id);
        seedMessage(chat, lily.id, "TEXT", "Hi Alex, 很高兴认识你！", null, true, LocalDateTime.now().minusHours(6));
        seedMessage(chat, alex.id, "TEXT", "Nice to meet you too! We can practice together.", null, true, LocalDateTime.now().minusHours(5));
        seedMessage(chat, lily.id, "IMAGE", "上周活动的照片发你看看。", "https://images.unsplash.com/photo-1522202176988-66273c2fd55f", false, LocalDateTime.now().minusHours(2));

        seedPost(lily.id, "第一次参加校园语言角，认识了很多有趣的新朋友。#校园生活 #口语练习",
            List.of("https://images.unsplash.com/photo-1522202176988-66273c2fd55f"), List.of("校园生活", "口语练习"));
        seedPost(alex.id, "今天终于能用中文完整介绍自己了，进步一点点也很开心。#学习打卡", List.of(), List.of("学习打卡"));
        seedPost(maria.id, "有没有人想一起去周末的国际美食节？#城市探索 #文化交流",
            List.of("https://images.unsplash.com/photo-1414235077428-338989a2e8c0"), List.of("城市探索", "文化交流"));

        ActivityData a1 = seedActivity("双语电影夜", "一起观看中英双语电影，并进行观后交流。", "ONLINE", null,
            "https://meeting.example.com/movie-night", null, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(2), 40, "PUBLISHED", admin.id);
        ActivityData a2 = seedActivity("城市文化漫步", "线下参观城市地标，进行混合分组交流。", "OFFLINE", "图书馆南广场",
            null, null, LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(4).plusHours(3), 30, "PUBLISHED", admin.id);
        seedActivity("HSK 口语冲刺工作坊", "面向留学生的中文口语提升小组。", "ONLINE", null,
            "https://meeting.example.com/hsk", "https://replay.example.com/hsk", LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(6).plusHours(2), 25, "PUBLISHED", admin.id);
        seedSignup(a1, alex.id, "RESERVE", "SUCCESS");
        seedSignup(a1, lily.id, "SIGNUP", "SUCCESS");
        seedSignup(a2, nora.id, "SIGNUP", "SUCCESS");
        seedActivityReview(a1, alex.id, 5, "活动很流畅，互动环节设计得很好。");

        seedReview(lily.id, alex.id, 5, "沟通很耐心，也会主动纠正表达。", "MATCH");
        seedReview(alex.id, lily.id, 5, "非常友好，聊天氛围轻松。", "MATCH");
        seedReview(zhou.id, maria.id, 4, "交流很自然，推荐继续配对。", "MATCH");

        seedReport(nora.id, 5001L, "POST", "内容存在广告嫌疑", "PENDING");
        seedReport(lily.id, alex.id, "USER", "测试用举报记录", "RESOLVED");

        notifyUser(lily.id, "推荐上新", "系统为你更新了 3 位高分语言伙伴。", "MATCH");
        notifyUser(alex.id, "活动提醒", "你报名的《双语电影夜》将在两天后开始。", "ACTIVITY");
    }

    private UserData createUser(String account, String password, String role, String name, String gender, String school, String college, String grade,
                                String nativeLanguage, String targetLanguage, String languageLevel, List<String> interestTags, List<String> targetTags,
                                String availableTime, String preferredMode, String communicationGoal, String introduction) {
        UserData existing = users.values().stream()
            .filter(item -> item.account.equalsIgnoreCase(account))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            return existing;
        }

        UserData user = new UserData();
        user.id = userId.incrementAndGet();
        user.account = account;
        user.password = password;
        user.role = role;
        user.name = name;
        user.gender = gender;
        user.school = school;
        user.college = college;
        user.grade = grade;
        user.avatar = avatarUrl(account);
        user.status = "NORMAL";
        user.createdAt = LocalDateTime.now().minusDays((long) (Math.random() * 30));
        user.updatedAt = user.createdAt;
        user.lastLoginTime = LocalDateTime.now().minusHours((long) (Math.random() * 24));
        user.profileCompleted = true;
        user.profile = new UserProfileData(nativeLanguage, targetLanguage, languageLevel, new ArrayList<>(interestTags), new ArrayList<>(targetTags),
            availableTime, preferredMode, communicationGoal, introduction);
        users.put(user.id, user);
        persistUser(user);
        return user;
    }

    private PostData seedPost(Long userIdValue, String content, List<String> images, List<String> topics) {
        PostData post = new PostData();
        post.id = postId.incrementAndGet();
        post.userId = userIdValue;
        post.content = content;
        post.images = new ArrayList<>(images);
        post.topics = new ArrayList<>(topics);
        post.status = "NORMAL";
        post.createdAt = LocalDateTime.now().minusHours((long) (Math.random() * 48));
        post.updatedAt = post.createdAt;
        post.likeUserIds.addAll(users.keySet().stream().filter(id -> !Objects.equals(id, userIdValue)).limit(2).collect(Collectors.toSet()));
        posts.put(post.id, post);
        return post;
    }

    private ActivityData seedActivity(String title, String content, String activityType, String location, String liveUrl, String replayUrl,
                                      LocalDateTime startTime, LocalDateTime endTime, Integer limitCount, String status, Long createdBy) {
        ActivityData activity = new ActivityData();
        activity.id = activityId.incrementAndGet();
        activity.title = title;
        activity.content = content;
        activity.activityType = activityType;
        activity.location = location;
        activity.liveUrl = liveUrl;
        activity.replayUrl = replayUrl;
        activity.startTime = startTime;
        activity.endTime = endTime;
        activity.limitCount = limitCount;
        activity.status = status;
        activity.createdBy = createdBy;
        activity.signups = new ArrayList<>();
        activity.reviews = new ArrayList<>();
        activities.put(activity.id, activity);
        return activity;
    }

    private void seedSignup(ActivityData activity, Long userIdValue, String signupType, String signupStatus) {
        ActivitySignupData signup = new ActivitySignupData();
        signup.id = reviewId.incrementAndGet();
        signup.activityId = activity.id;
        signup.userId = userIdValue;
        signup.signupType = signupType;
        signup.signupStatus = signupStatus;
        signup.signupTime = LocalDateTime.now().minusDays(1);
        activity.signups.add(signup);
    }

    private void seedActivityReview(ActivityData activity, Long userIdValue, int score, String content) {
        ActivityReviewData review = new ActivityReviewData();
        review.id = reviewId.incrementAndGet();
        review.activityId = activity.id;
        review.userId = userIdValue;
        review.score = score;
        review.content = content;
        review.createdAt = LocalDateTime.now().minusHours(10);
        activity.reviews.add(review);
    }

    private void seedReview(Long fromUserId, Long toUserId, int score, String content, String type) {
        ReviewData review = new ReviewData();
        review.id = reviewId.incrementAndGet();
        review.fromUserId = fromUserId;
        review.toUserId = toUserId;
        review.score = score;
        review.content = content;
        review.type = type;
        review.createdAt = LocalDateTime.now().minusDays(2);
        reviews.add(review);
    }

    private void seedReport(Long reportUserId, Long targetId, String targetType, String reason, String status) {
        ReportData report = new ReportData();
        report.id = reportId.incrementAndGet();
        report.reportUserId = reportUserId;
        report.targetId = targetId;
        report.targetType = targetType;
        report.reason = reason;
        report.status = status;
        report.createdAt = LocalDateTime.now().minusHours(8);
        reports.put(report.id, report);
    }

    private void seedMessage(ChatSessionData session, Long senderIdValue, String type, String content, String fileUrl, boolean read, LocalDateTime sendTime) {
        ChatMessageData message = new ChatMessageData();
        message.id = messageId.incrementAndGet();
        message.sessionId = session.id;
        message.senderId = senderIdValue;
        message.messageType = type;
        message.content = content;
        message.fileUrl = fileUrl;
        message.read = read;
        message.revoked = false;
        message.sendTime = sendTime;
        session.messages.add(message);
        session.lastMessageTime = sendTime;
    }

    private MatchData ensureMatch(UserData source, UserData target) {
        return matches.stream()
            .filter(item -> Objects.equals(item.userId, source.id) && Objects.equals(item.targetUserId, target.id))
            .findFirst()
            .orElseGet(() -> {
                MatchScore score = calculateScore(source, target);
                MatchData match = new MatchData();
                match.id = matchId.incrementAndGet();
                match.userId = source.id;
                match.targetUserId = target.id;
                match.score = score.score;
                match.reasons = score.reasons;
                match.status = "RECOMMENDED";
                match.source = "SMART_ENGINE";
                match.createdAt = LocalDateTime.now();
                match.updatedAt = LocalDateTime.now();
                matches.add(match);
                return match;
            });
    }

    private ChatSessionData ensureSession(Long leftUserId, Long rightUserId) {
        return sessions.values().stream()
            .filter(session -> samePair(session.userId, session.peerUserId, leftUserId, rightUserId))
            .findFirst()
            .orElseGet(() -> {
                ChatSessionData session = new ChatSessionData();
                session.id = sessionId.incrementAndGet();
                session.userId = leftUserId;
                session.peerUserId = rightUserId;
                session.status = "ACTIVE";
                session.createdAt = LocalDateTime.now();
                session.updatedAt = LocalDateTime.now();
                session.lastMessageTime = LocalDateTime.now();
                session.messages = new ArrayList<>();
                sessions.put(session.id, session);
                return session;
            });
    }

    private MatchScore calculateScore(UserData current, UserData candidate) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (equalsIgnoreCase(current.profile.nativeLanguage, candidate.profile.targetLanguage)
            && equalsIgnoreCase(current.profile.targetLanguage, candidate.profile.nativeLanguage)) {
            score += 45;
            reasons.add("语言目标高度互补，适合双向练习");
        } else if (equalsIgnoreCase(current.profile.targetLanguage, candidate.profile.nativeLanguage)
            || equalsIgnoreCase(current.profile.nativeLanguage, candidate.profile.targetLanguage)) {
            score += 25;
            reasons.add("存在单向语言互补优势");
        }
        Set<String> common = new HashSet<>(current.profile.interestTags);
        common.retainAll(candidate.profile.interestTags);
        if (!common.isEmpty()) {
            score += Math.min(20, common.size() * 6);
            reasons.add("共同兴趣包含：" + String.join("、", common));
        }
        if (containsAny(current.profile.communicationGoal, candidate.profile.communicationGoal, current.profile.communicationGoal)) {
            score += 15;
            reasons.add("交流目标接近，适合长期互动");
        }
        if (containsAny(current.profile.availableTime, candidate.profile.availableTime, current.profile.availableTime)) {
            score += 10;
            reasons.add("空闲时间段有明显重合");
        }
        if (containsAny(current.profile.preferredMode, candidate.profile.preferredMode, current.profile.preferredMode)) {
            score += 5;
            reasons.add("偏好的交流方式一致");
        }
        score += (int) Math.min(5, averageScore(candidate.id));
        if (reasons.isEmpty()) {
            reasons.add("基础画像相近，建议先从社区互动开始了解彼此");
        }
        return new MatchScore(Math.min(score, 100), reasons);
    }

    private Map<String, Object> toMatchMap(UserData current, UserData candidate) {
        MatchData match = ensureMatch(current, candidate);
        MatchScore score = calculateScore(current, candidate);
        match.score = score.score;
        match.reasons = score.reasons;
        match.updatedAt = LocalDateTime.now();
        return matchMap(match, candidate);
    }

    private Map<String, Object> matchMap(MatchData match, UserData candidate) {
        Map<String, Object> map = new LinkedHashMap<>(userMap(candidate, false));
        map.put("matchId", match.id);
        map.put("matchScore", match.score);
        map.put("matchStatus", match.status);
        map.put("matchReasons", match.reasons);
        map.put("source", match.source);
        map.put("createdAt", match.createdAt.toString());
        return map;
    }

    private Map<String, Object> userMap(UserData user, boolean includePrivate) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.id);
        map.put("account", user.account);
        map.put("role", user.role);
        map.put("status", user.status);
        map.put("name", user.name);
        map.put("gender", user.gender);
        map.put("school", user.school);
        map.put("college", user.college);
        map.put("grade", user.grade);
        map.put("avatar", user.avatar);
        map.put("nativeLanguage", user.profile.nativeLanguage);
        map.put("targetLanguage", user.profile.targetLanguage);
        map.put("languageLevel", user.profile.languageLevel);
        map.put("interestTags", user.profile.interestTags);
        map.put("targetTags", user.profile.targetTags);
        map.put("availableTime", user.profile.availableTime);
        map.put("preferredMode", user.profile.preferredMode);
        map.put("communicationGoal", user.profile.communicationGoal);
        map.put("introduction", user.profile.introduction);
        map.put("averageScore", averageScore(user.id));
        map.put("profileCompleted", user.profileCompleted);
        if (includePrivate) {
            map.put("lastLoginTime", user.lastLoginTime);
            map.put("createdAt", user.createdAt);
            map.put("updatedAt", user.updatedAt);
        }
        return map;
    }

    private Map<String, Object> sessionMap(ChatSessionData session, Long viewerId) {
        UserData peer = requireUser(peerId(session, viewerId));
        long unread = session.messages.stream().filter(msg -> !Objects.equals(msg.senderId, viewerId) && !msg.read).count();
        String lastMessage = session.messages.isEmpty() ? "" : session.messages.get(session.messages.size() - 1).content;
        return Map.of("id", session.id, "status", session.status, "peer", userMap(peer, false), "unreadCount", unread, "lastMessage", lastMessage, "lastMessageTime", session.lastMessageTime.toString());
    }

    private Map<String, Object> messageMap(ChatMessageData message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.id);
        map.put("sessionId", message.sessionId);
        map.put("senderId", message.senderId);
        map.put("senderName", requireUser(message.senderId).name);
        map.put("messageType", message.messageType);
        map.put("content", message.content);
        map.put("fileUrl", message.fileUrl);
        map.put("readStatus", message.read);
        map.put("revokeStatus", message.revoked);
        map.put("sendTime", message.sendTime.toString());
        return map;
    }

    private Map<String, Object> postMap(PostData post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.id);
        map.put("author", userMap(requireUser(post.userId), false));
        map.put("content", post.content);
        map.put("images", post.images);
        map.put("topics", post.topics);
        map.put("likeCount", post.likeUserIds.size());
        map.put("favoriteCount", post.favoriteUserIds.size());
        map.put("commentCount", post.comments.size());
        map.put("shareCount", post.shareCount);
        map.put("createdAt", post.createdAt.toString());
        map.put("comments", post.comments.stream().map(this::commentMap).toList());
        return map;
    }

    private Map<String, Object> commentMap(PostCommentData comment) {
        return Map.of("id", comment.id, "user", userMap(requireUser(comment.userId), false), "content", comment.content, "createdAt", comment.createdAt.toString());
    }

    private Map<String, Object> activityMap(ActivityData activity, Long userIdValue) {
        long signupCount = activity.signups.stream().filter(signup -> "SUCCESS".equalsIgnoreCase(signup.signupStatus)).count();
        boolean joined = activity.signups.stream().anyMatch(signup -> Objects.equals(signup.userId, userIdValue) && "SUCCESS".equalsIgnoreCase(signup.signupStatus));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", activity.id);
        map.put("title", activity.title);
        map.put("content", activity.content);
        map.put("activityType", activity.activityType);
        map.put("location", activity.location);
        map.put("liveUrl", activity.liveUrl);
        map.put("replayUrl", activity.replayUrl);
        map.put("startTime", activity.startTime.toString());
        map.put("endTime", activity.endTime.toString());
        map.put("displayTime", activity.startTime.format(DISPLAY_TIME) + " - " + activity.endTime.format(DISPLAY_TIME));
        map.put("limitCount", activity.limitCount);
        map.put("status", activity.status);
        map.put("signupCount", signupCount);
        map.put("joined", joined);
        map.put("reviewCount", activity.reviews.size());
        map.put("createdBy", activity.createdBy);
        return map;
    }

    private Map<String, Object> signupMap(ActivitySignupData signup) {
        return Map.of("id", signup.id, "user", userMap(requireUser(signup.userId), false), "signupType", signup.signupType, "signupStatus", signup.signupStatus, "signupTime", signup.signupTime.toString());
    }

    private Map<String, Object> activityReviewMap(ActivityReviewData review) {
        return Map.of("id", review.id, "user", userMap(requireUser(review.userId), false), "score", review.score, "content", review.content, "createdAt", review.createdAt.toString());
    }

    private Map<String, Object> reviewMap(ReviewData review) {
        return Map.of("id", review.id, "fromUser", userMap(requireUser(review.fromUserId), false), "score", review.score, "content", review.content, "type", review.type, "createdAt", review.createdAt.toString());
    }

    private Map<String, Object> reportMap(ReportData report) {
        return Map.of("id", report.id, "reportUser", userMap(requireUser(report.reportUserId), false), "targetId", report.targetId, "targetType", report.targetType, "reason", report.reason, "status", report.status, "createdAt", report.createdAt.toString());
    }

    private Map<String, Object> notificationMap(NotificationData notification) {
        return Map.of("id", notification.id, "title", notification.title, "content", notification.content, "type", notification.type, "readStatus", notification.read, "createdAt", notification.createdAt.toString());
    }

    private void notifyUser(Long userIdValue, String title, String content, String type) {
        NotificationData notice = new NotificationData();
        notice.id = noticeId.incrementAndGet();
        notice.userId = userIdValue;
        notice.title = title;
        notice.content = content;
        notice.type = type;
        notice.read = false;
        notice.createdAt = LocalDateTime.now();
        notifications.computeIfAbsent(userIdValue, ignored -> new ArrayList<>()).add(notice);
    }

    private UserData requireUser(Long id) {
        UserData user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private ChatSessionData requireSession(Long id) {
        ChatSessionData session = sessions.get(id);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        return session;
    }

    private PostData requirePost(Long id) {
        PostData post = posts.get(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        return post;
    }

    private ActivityData requireActivity(Long id) {
        ActivityData activity = activities.get(id);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    private ReportData requireReport(Long id) {
        ReportData report = reports.get(id);
        if (report == null) {
            throw new IllegalArgumentException("举报不存在");
        }
        return report;
    }

    private double averageScore(Long userIdValue) {
        return Math.round(reviews.stream().filter(review -> Objects.equals(review.toUserId, userIdValue)).mapToInt(review -> review.score).average().orElse(4.6) * 10) / 10.0;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(value.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
    }

    private String avatarUrl(String seed) {
        return "https://api.dicebear.com/7.x/thumbs/svg?seed=" + seed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean containsAny(String left, String right, String keyword) {
        return Optional.ofNullable(left).orElse("").contains(keyword) || Optional.ofNullable(right).orElse("").contains(keyword);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return Optional.ofNullable(left).orElse("").equalsIgnoreCase(Optional.ofNullable(right).orElse(""));
    }

    private List<String> emptyIfNull(List<String> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).filter(item -> !item.isBlank()).toList();
    }

    private void toggle(Set<Long> set, Long value) {
        if (!set.add(value)) {
            set.remove(value);
        }
    }

    private boolean samePair(Long leftA, Long leftB, Long rightA, Long rightB) {
        return (Objects.equals(leftA, rightA) && Objects.equals(leftB, rightB)) || (Objects.equals(leftA, rightB) && Objects.equals(leftB, rightA));
    }

    private Long peerId(ChatSessionData session, Long userIdValue) {
        return Objects.equals(session.userId, userIdValue) ? session.peerUserId : session.userId;
    }

    private static class MatchScore {
        private final double score;
        private final List<String> reasons;

        private MatchScore(double score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }
    }

    private static class UserData {
        Long id;
        String account;
        String password;
        String role;
        String name;
        String gender;
        String school;
        String college;
        String grade;
        String avatar;
        String status;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
        LocalDateTime lastLoginTime;
        boolean profileCompleted;
        UserProfileData profile;
    }

    private static class UserProfileData {
        String nativeLanguage;
        String targetLanguage;
        String languageLevel;
        List<String> interestTags;
        List<String> targetTags;
        String availableTime;
        String preferredMode;
        String communicationGoal;
        String introduction;

        UserProfileData(String nativeLanguage, String targetLanguage, String languageLevel, List<String> interestTags,
                        List<String> targetTags, String availableTime, String preferredMode, String communicationGoal, String introduction) {
            this.nativeLanguage = nativeLanguage;
            this.targetLanguage = targetLanguage;
            this.languageLevel = languageLevel;
            this.interestTags = interestTags;
            this.targetTags = targetTags;
            this.availableTime = availableTime;
            this.preferredMode = preferredMode;
            this.communicationGoal = communicationGoal;
            this.introduction = introduction;
        }
    }

    private static class MatchData {
        Long id;
        Long userId;
        Long targetUserId;
        double score;
        List<String> reasons;
        String status;
        String source;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
    }

    private static class ChatSessionData {
        Long id;
        Long userId;
        Long peerUserId;
        String status;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
        LocalDateTime lastMessageTime;
        List<ChatMessageData> messages;
    }

    private static class ChatMessageData {
        Long id;
        Long sessionId;
        Long senderId;
        String messageType;
        String content;
        String fileUrl;
        boolean read;
        boolean revoked;
        LocalDateTime sendTime;
    }

    private static class PostData {
        Long id;
        Long userId;
        String content;
        List<String> images = new ArrayList<>();
        List<String> topics = new ArrayList<>();
        String status;
        int shareCount;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
        Set<Long> likeUserIds = new HashSet<>();
        Set<Long> favoriteUserIds = new HashSet<>();
        List<PostCommentData> comments = new ArrayList<>();
    }

    private static class PostCommentData {
        Long id;
        Long postId;
        Long userId;
        String content;
        LocalDateTime createdAt;
    }

    private static class ActivityData {
        Long id;
        String title;
        String content;
        String activityType;
        String location;
        String liveUrl;
        String replayUrl;
        LocalDateTime startTime;
        LocalDateTime endTime;
        Integer limitCount;
        String status;
        Long createdBy;
        List<ActivitySignupData> signups;
        List<ActivityReviewData> reviews;
    }

    private static class ActivitySignupData {
        Long id;
        Long activityId;
        Long userId;
        String signupType;
        String signupStatus;
        LocalDateTime signupTime;
    }

    private static class ActivityReviewData {
        Long id;
        Long activityId;
        Long userId;
        Integer score;
        String content;
        LocalDateTime createdAt;
    }

    private static class ReviewData {
        Long id;
        Long fromUserId;
        Long toUserId;
        Integer score;
        String content;
        String type;
        LocalDateTime createdAt;
    }

    private static class ReportData {
        Long id;
        Long reportUserId;
        Long targetId;
        String targetType;
        String reason;
        String status;
        LocalDateTime createdAt;
    }

    private static class NotificationData {
        Long id;
        Long userId;
        String title;
        String content;
        String type;
        boolean read;
        LocalDateTime createdAt;
    }
}
