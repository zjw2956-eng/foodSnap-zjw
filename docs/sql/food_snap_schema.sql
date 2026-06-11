-- =====================================================
-- 美食分享社交平台 - 建表语句
-- 数据库：foodSnap，字符集：utf8mb4
-- =====================================================

SET FOREIGN_KEY_CHECKS=0;

-- ==================== 用户表补充 ====================
-- sys_user 表已存在（V1.0.1），以下为新增字段
ALTER TABLE `sys_user`
    ADD COLUMN `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
    ADD COLUMN `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL(Minio)',
    ADD COLUMN `location` varchar(100) DEFAULT NULL COMMENT '所在城市/区域',
    ADD COLUMN `latitude` decimal(10,6) DEFAULT NULL COMMENT '最近一次纬度',
    ADD COLUMN `longitude` decimal(10,6) DEFAULT NULL COMMENT '最近一次经度',
    ADD COLUMN `post_count` int(11) NOT NULL DEFAULT 0 COMMENT '发帖数',
    ADD COLUMN `liked_count` int(11) NOT NULL DEFAULT 0 COMMENT '获赞总数',
    ADD COLUMN `ranking_count` int(11) NOT NULL DEFAULT 0 COMMENT '上榜次数';

-- ==================== 美食帖子表 ====================
DROP TABLE IF EXISTS `food_post`;
CREATE TABLE `food_post` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_uuid` varchar(32) NOT NULL COMMENT '帖子唯一标识(UUID)',
  `user_id` int(11) NOT NULL COMMENT '发帖用户ID',
  `image_url` varchar(500) NOT NULL COMMENT '美食图片URL(Minio)',
  `food_name` varchar(100) DEFAULT NULL COMMENT 'AI识别的菜品名',
  `description` text COMMENT '文案描述(AI生成或用户手写)',
  `category` varchar(20) NOT NULL DEFAULT 'lunch' COMMENT '分类：breakfast/lunch/dinner/snack/night_snack',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '发帖纬度',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '发帖经度',
  `area_code` varchar(20) DEFAULT NULL COMMENT '区域编码(行政区划代码前6位)',
  `area_name` varchar(50) DEFAULT NULL COMMENT '区域名称',
  `like_count` int(11) NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int(11) NOT NULL DEFAULT 0 COMMENT '评论数',
  `collect_count` int(11) NOT NULL DEFAULT 0 COMMENT '收藏数',
  `score` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '综合评分(排行依据)',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1正常 2AI分析中 3已下架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_uuid` (`post_uuid`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_area_status_time` (`area_code`, `status`, `create_time`),
  KEY `idx_status_time` (`status`, `create_time`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美食帖子表';

-- ==================== 点赞记录表 ====================
DROP TABLE IF EXISTS `food_like`;
CREATE TABLE `food_like` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` int(11) NOT NULL COMMENT '帖子ID',
  `user_id` int(11) NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

-- ==================== 评论表(弹幕+传统) ====================
DROP TABLE IF EXISTS `food_comment`;
CREATE TABLE `food_comment` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `comment_uuid` varchar(32) NOT NULL COMMENT '评论唯一标识',
  `post_id` int(11) NOT NULL COMMENT '帖子ID',
  `user_id` int(11) NOT NULL COMMENT '评论用户ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `comment_type` varchar(10) NOT NULL DEFAULT 'danmaku' COMMENT '类型：danmaku弹幕 / normal普通评论',
  `danmaku_position` int(3) DEFAULT NULL COMMENT '弹幕纵向位置(百分比0-100)',
  `danmaku_color` varchar(10) DEFAULT '#ffffff' COMMENT '弹幕颜色',
  `reply_to` int(11) DEFAULT NULL COMMENT '回复的评论ID(普通评论使用)',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1正常 2已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_uuid` (`comment_uuid`),
  KEY `idx_post_time` (`post_id`, `create_time`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表(弹幕+传统)';

-- ==================== 收藏记录表 ====================
DROP TABLE IF EXISTS `food_collect`;
CREATE TABLE `food_collect` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` int(11) NOT NULL COMMENT '帖子ID',
  `user_id` int(11) NOT NULL COMMENT '收藏用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏记录表';

-- ==================== 每日榜单表 ====================
DROP TABLE IF EXISTS `food_ranking`;
CREATE TABLE `food_ranking` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ranking_date` date NOT NULL COMMENT '榜单日期',
  `ranking_type` varchar(10) NOT NULL COMMENT '类型：realtime实时热榜 / daily每日金榜',
  `post_id` int(11) NOT NULL COMMENT '帖子ID',
  `area_code` varchar(20) NOT NULL COMMENT '区域编码',
  `category` varchar(20) NOT NULL DEFAULT 'all' COMMENT '分类：breakfast/lunch/dinner/snack/night_snack/all',
  `score` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '综合评分',
  `rank_position` int(5) NOT NULL COMMENT '排名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_date_type_area_cat` (`ranking_date`, `ranking_type`, `area_code`, `category`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日榜单表';

-- ==================== AI 分析记录表 ====================
DROP TABLE IF EXISTS `food_ai_analysis`;
CREATE TABLE `food_ai_analysis` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` int(11) NOT NULL COMMENT '帖子ID',
  `food_name` varchar(100) DEFAULT NULL COMMENT 'AI识别的菜品名',
  `confidence` decimal(3,2) DEFAULT NULL COMMENT '识别置信度(0-1)',
  `ingredients` varchar(500) DEFAULT NULL COMMENT '推测的食材/配料',
  `calories_estimate` int(6) DEFAULT NULL COMMENT '热量估算(大卡)',
  `suggested_caption` text COMMENT 'AI生成的建议文案',
  `similar_recommendation` text COMMENT '同款美食建议(附近类似餐厅/外卖)',
  `analysis_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1待处理 2已完成 3失败',
  `error_message` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_status` (`analysis_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析记录表';

-- ==================== 敏感词表 ====================
DROP TABLE IF EXISTS `food_sensitive_word`;
CREATE TABLE `food_sensitive_word` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(50) NOT NULL COMMENT '敏感词',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

SET FOREIGN_KEY_CHECKS=1;
