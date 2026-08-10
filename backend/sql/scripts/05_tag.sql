-- 05 标签表 + 默认标签
SET NAMES utf8mb4;
USE worklog;

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

INSERT INTO tag (name, color, sort) VALUES
('后端', 'tag-backend', 1),
('前端', 'tag-frontend', 2),
('数据库', 'tag-db', 3),
('Redis', 'tag-redis', 4),
('运维', 'tag-ops', 5);
