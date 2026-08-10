-- 03 问题表
SET NAMES utf8mb4;
USE worklog;

DROP TABLE IF EXISTS issue;
CREATE TABLE issue (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
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
  KEY idx_issue_favorite (favorite)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='问题表';
