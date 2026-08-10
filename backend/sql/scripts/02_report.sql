-- 02 日报表
SET NAMES utf8mb4;
USE worklog;

DROP TABLE IF EXISTS report;
CREATE TABLE report (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  report_date DATE         NOT NULL COMMENT '工作日期',
  weekday     VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '星期（冗余存储）',
  time_range  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '时间段，如 09:30 - 18:20',
  title       VARCHAR(128) NOT NULL COMMENT '日报标题',
  tasks       JSON         NOT NULL COMMENT '工作内容列表（JSON 数组）',
  tags        JSON         NOT NULL COMMENT '标签列表（JSON 数组）',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_report_date (report_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='日报表';
