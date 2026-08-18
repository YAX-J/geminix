-- ============================================================
-- 工作日志台 数据库初始化脚本
-- 执行方式: mysql -uroot -p < init.sql 或由后端首次启动自动执行
-- 注意: 文件含中文默认值，必须保证连接为 utf8mb4（下方 SET NAMES 已兜底）
-- ============================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS worklog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE worklog;

-- ---------- 日报表（只存工作内容，与问题解耦） ----------
DROP TABLE IF EXISTS report;
CREATE TABLE report (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL COMMENT '所属用户（数据隔离）',
  report_date DATE         NOT NULL COMMENT '工作日期',
  weekday     VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '星期（冗余存储）',
  time_range  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '时间段，如 09:30 - 18:20',
  title       VARCHAR(128) NOT NULL COMMENT '日报标题',
  tasks       JSON         NOT NULL COMMENT '工作内容列表（JSON 数组）',
  tags        JSON         NOT NULL COMMENT '标签列表（JSON 数组）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_report_date (report_date),
  KEY idx_report_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='日报表';

-- ---------- 问题表（独立实体，可单独记录，不依赖日报） ----------
DROP TABLE IF EXISTS issue;
CREATE TABLE issue (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL COMMENT '所属用户（数据隔离）',
  title       VARCHAR(128) NOT NULL COMMENT '问题标题',
  description TEXT         NOT NULL COMMENT '问题描述',
  solution    TEXT         NULL COMMENT '最佳解决方案摘要（为空表示未解决，兼容旧数据）',
  tag         VARCHAR(32)  NOT NULL DEFAULT '后端' COMMENT '问题类型标签',
  status      VARCHAR(16)  NOT NULL DEFAULT 'open' COMMENT '状态: open 待解决 / done 已解决',
  report_date DATE         NULL COMMENT '关联日报日期（可空=独立问题）',
  solutions   JSON         NULL COMMENT '多个解决方案（JSON 数组，元素 {content,best}）',
  favorite    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否收藏（常见问题置顶）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_issue_status (status),
  KEY idx_issue_report_date (report_date),
  KEY idx_issue_favorite (favorite),
  KEY idx_issue_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='问题表';

-- ---------- 用户表 ----------
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
  nickname    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
  role        VARCHAR(16)  NOT NULL DEFAULT 'AUTHOR' COMMENT '角色: ADMIN/AUTHOR/READER',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- ---------- 标签表（独立管理，不再硬编码） ----------
DROP TABLE IF EXISTS tag;
CREATE TABLE tag (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(32)  NOT NULL COMMENT '标签名称（唯一）',
  color       VARCHAR(32)  NULL COMMENT '展示色（CSS 类名或色值，可空）',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='标签表';

-- 默认标签（后端/前端/数据库/Redis/运维）
INSERT INTO tag (name, color, sort) VALUES
('后端', 'tag-backend', 1),
('前端', 'tag-frontend', 2),
('数据库', 'tag-db', 3),
('Redis', 'tag-redis', 4),
('运维', 'tag-ops', 5);

-- （演示数据已清空：多用户改造后报表/问题由用户自行创建，admin 账号由后端 InitAdminRunner 首次启动创建）
