CREATE DATABASE IF NOT EXISTS language_exchange_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE language_exchange_platform;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `account` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
  `name` VARCHAR(50) NOT NULL COMMENT '昵称或姓名',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `school` VARCHAR(100) DEFAULT NULL COMMENT '学校名称',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '院系名称',
  `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '账号状态',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `user_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `native_language` VARCHAR(50) NOT NULL COMMENT '母语',
  `target_language` VARCHAR(50) NOT NULL COMMENT '目标学习语言',
  `language_level` VARCHAR(20) DEFAULT NULL COMMENT '语言等级',
  `interest_tags` VARCHAR(255) DEFAULT NULL COMMENT '兴趣标签',
  `target_tags` VARCHAR(255) DEFAULT NULL COMMENT '目标标签',
  `available_time` VARCHAR(100) DEFAULT NULL COMMENT '空闲时间偏好',
  `preferred_mode` VARCHAR(50) DEFAULT NULL COMMENT '偏好交流方式',
  `communication_goal` VARCHAR(100) DEFAULT NULL COMMENT '交流目标',
  `introduction` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_user_id` (`user_id`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

CREATE TABLE IF NOT EXISTS `match_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '发起匹配用户ID',
  `matched_user_id` BIGINT NOT NULL COMMENT '被匹配用户ID',
  `match_score` DECIMAL(5,2) DEFAULT NULL COMMENT '匹配分值',
  `match_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '匹配状态',
  `source` VARCHAR(20) DEFAULT NULL COMMENT '匹配来源',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_match_user_id` (`user_id`),
  KEY `idx_match_matched_user_id` (`matched_user_id`),
  CONSTRAINT `fk_match_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_match_matched_user` FOREIGN KEY (`matched_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='匹配记录表';

CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户A',
  `peer_user_id` BIGINT NOT NULL COMMENT '用户B',
  `session_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_user_id` (`user_id`),
  KEY `idx_session_peer_user_id` (`peer_user_id`),
  CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_session_peer_user` FOREIGN KEY (`peer_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息主键ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `message_type` VARCHAR(20) NOT NULL COMMENT '消息类型',
  `content` TEXT DEFAULT NULL COMMENT '消息内容',
  `file_url` VARCHAR(255) DEFAULT NULL COMMENT '文件地址',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  `revoke_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否撤回',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_session_id` (`session_id`),
  KEY `idx_message_sender_id` (`sender_id`),
  CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS `call_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通话记录主键ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `caller_id` BIGINT NOT NULL COMMENT '发起者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `call_type` VARCHAR(20) NOT NULL COMMENT '通话类型',
  `call_status` VARCHAR(20) NOT NULL COMMENT '通话状态',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration` INT DEFAULT NULL COMMENT '通话时长秒数',
  PRIMARY KEY (`id`),
  KEY `idx_call_session_id` (`session_id`),
  KEY `idx_call_caller_id` (`caller_id`),
  KEY `idx_call_receiver_id` (`receiver_id`),
  CONSTRAINT `fk_call_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`),
  CONSTRAINT `fk_call_caller` FOREIGN KEY (`caller_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_call_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话记录表';

CREATE TABLE IF NOT EXISTS `post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子主键ID',
  `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
  `content` TEXT NOT NULL COMMENT '帖子正文',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '可见范围',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
  `share_count` INT NOT NULL DEFAULT 0 COMMENT '分享数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '帖子状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_user_id` (`user_id`),
  KEY `idx_post_created_at` (`created_at`),
  CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

CREATE TABLE IF NOT EXISTS `post_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `image_url` VARCHAR(255) NOT NULL COMMENT '图片地址',
  `sort_no` INT DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`id`),
  KEY `idx_post_image_post_id` (`post_id`),
  CONSTRAINT `fk_post_image_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子图片表';

CREATE TABLE IF NOT EXISTS `topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '话题主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '话题名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '话题描述',
  `heat` INT NOT NULL DEFAULT 0 COMMENT '热度值',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '话题状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_topic_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题表';

CREATE TABLE IF NOT EXISTS `post_topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `topic_id` BIGINT NOT NULL COMMENT '话题ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_topic` (`post_id`, `topic_id`),
  KEY `idx_post_topic_topic_id` (`topic_id`),
  CONSTRAINT `fk_post_topic_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
  CONSTRAINT `fk_post_topic_topic` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子话题关联表';

CREATE TABLE IF NOT EXISTS `post_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论主键ID',
  `post_id` BIGINT NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '评论状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_post_id` (`post_id`),
  KEY `idx_comment_user_id` (`user_id`),
  KEY `idx_comment_parent_id` (`parent_id`),
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论表';

CREATE TABLE IF NOT EXISTS `post_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `action_type` VARCHAR(20) NOT NULL COMMENT '行为类型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_action_post_id` (`post_id`),
  KEY `idx_post_action_user_id` (`user_id`),
  CONSTRAINT `fk_post_action_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`),
  CONSTRAINT `fk_post_action_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子互动表';

CREATE TABLE IF NOT EXISTS `activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动标题',
  `content` TEXT NOT NULL COMMENT '活动内容',
  `activity_type` VARCHAR(20) NOT NULL COMMENT '活动类型',
  `location` VARCHAR(255) DEFAULT NULL COMMENT '活动地点',
  `live_url` VARCHAR(255) DEFAULT NULL COMMENT '直播地址',
  `replay_url` VARCHAR(255) DEFAULT NULL COMMENT '回放地址',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `limit_count` INT DEFAULT NULL COMMENT '人数上限',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态',
  `created_by` BIGINT NOT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  KEY `idx_activity_created_by` (`created_by`),
  KEY `idx_activity_start_time` (`start_time`),
  CONSTRAINT `fk_activity_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

CREATE TABLE IF NOT EXISTS `activity_signup` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `signup_type` VARCHAR(20) NOT NULL COMMENT '预约或正式报名',
  `signup_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `signup_status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '报名状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_signup` (`activity_id`, `user_id`, `signup_type`),
  KEY `idx_activity_signup_user_id` (`user_id`),
  CONSTRAINT `fk_signup_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
  CONSTRAINT `fk_signup_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名表';

CREATE TABLE IF NOT EXISTS `activity_checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `checkin_time` DATETIME DEFAULT NULL COMMENT '签到时间',
  `checkin_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '签到状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_checkin` (`activity_id`, `user_id`),
  KEY `idx_activity_checkin_user_id` (`user_id`),
  CONSTRAINT `fk_checkin_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动签到表';

CREATE TABLE IF NOT EXISTS `activity_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '评价用户ID',
  `score` INT NOT NULL COMMENT '评分',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_review_activity_id` (`activity_id`),
  KEY `idx_activity_review_user_id` (`user_id`),
  CONSTRAINT `fk_activity_review_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
  CONSTRAINT `fk_activity_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动评价表';

CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `from_user_id` BIGINT NOT NULL COMMENT '评价发起人',
  `to_user_id` BIGINT NOT NULL COMMENT '被评价用户',
  `score` INT NOT NULL COMMENT '评分',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `type` VARCHAR(20) DEFAULT NULL COMMENT '评价类型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_from_user_id` (`from_user_id`),
  KEY `idx_review_to_user_id` (`to_user_id`),
  CONSTRAINT `fk_review_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_review_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户评价表';

CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_user_id` BIGINT NOT NULL COMMENT '举报人ID',
  `target_id` BIGINT NOT NULL COMMENT '举报目标ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型',
  `reason` VARCHAR(255) NOT NULL COMMENT '举报原因',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_user_id` (`report_user_id`),
  KEY `idx_report_target` (`target_id`, `target_type`),
  CONSTRAINT `fk_report_user` FOREIGN KEY (`report_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '接收通知用户ID',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
  `type` VARCHAR(20) NOT NULL COMMENT '通知类型',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_id` (`user_id`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
