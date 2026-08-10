-- ============================================================
-- 工作日志台 数据库初始化脚本
-- 执行方式: mysql -uroot -p < init.sql 或由后端首次启动自动执行
-- ============================================================
CREATE DATABASE IF NOT EXISTS worklog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE worklog;

-- ---------- 日报表（只存工作内容，与问题解耦） ----------
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

-- ---------- 问题表（独立实体，可单独记录，不依赖日报） ----------
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

-- ---------- 用户表 ----------
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
  nickname    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
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

-- ---------- 演示数据 ----------
INSERT INTO report (report_date, weekday, time_range, title, tasks, tags) VALUES
('2026-08-10', '星期一', '09:30 - 18:20', '订单模块性能优化 & 缓存改造',
 JSON_ARRAY('完成订单列表接口压测，QPS 由 800 提升至 2400', '订单详情接入 Redis 缓存，命中率 92%', '修复库存扣减并发场景下的超卖问题'),
 JSON_ARRAY('后端', 'Redis')),
('2026-08-07', '星期五', '10:00 - 19:00', '报表导出功能联调',
 JSON_ARRAY('完成月度报表导出接口联调', '优化导出文件生成流程，支持后台异步任务'),
 JSON_ARRAY('前端', '数据库')),
('2026-08-06', '星期四', '09:00 - 18:00', '登录模块接入 JWT + 网关鉴权',
 JSON_ARRAY('完成 JWT 签发与刷新流程', '网关统一接入 Token 校验过滤器'),
 JSON_ARRAY('后端')),
('2026-08-05', '星期三', '09:30 - 18:30', '前端组件库搭建与页面开发',
 JSON_ARRAY('完成基础组件库目录搭建（Button / Table / Form）', '开发日报列表页与详情抽屉'),
 JSON_ARRAY('前端')),
('2026-08-04', '星期二', '09:00 - 17:40', '测试环境部署与 CI 流程',
 JSON_ARRAY('编写 Dockerfile 并接入 Jenkins 流水线', '完成测试环境一键部署'),
 JSON_ARRAY('运维')),
('2026-08-03', '星期一', '09:00 - 18:00', '项目初始化与技术选型',
 JSON_ARRAY('完成 Spring Boot + MySQL + Redis 基础工程搭建', '确定 Vue3 + Vite 前端工程结构', '设计日报、问题、标签核心表结构'),
 JSON_ARRAY('后端', '前端', '数据库'));

INSERT INTO issue (title, description, solution, tag, status, report_date, solutions, favorite) VALUES
('Redis 缓存击穿导致慢查询', '热点订单数据缓存击穿，瞬时高并发请求直接打到 MySQL，出现多条慢查询', '本地缓存 + Redis 分布式锁双重兜底，缓存设置逻辑过期并异步刷新', 'Redis', 'done', '2026-08-10',
 JSON_ARRAY(JSON_OBJECT('content', '本地缓存 + Redis 分布式锁双重兜底，缓存设置逻辑过期并异步刷新', 'best', TRUE),
            JSON_OBJECT('content', '热点 key 预生成 + 互斥重建，击穿窗口内只允许一个线程回源', 'best', FALSE)),
 1),
('报表查询 N+1 导致接口超时', '报表明细查询在循环中逐条查库，SQL 执行 200+ 次', '关联查询 + 批量 IN 替代循环单查，SQL 次数降到 3 次', '数据库', 'done', '2026-08-07',
 JSON_ARRAY(JSON_OBJECT('content', '关联查询 + 批量 IN 替代循环单查，SQL 次数降到 3 次', 'best', TRUE)), 0),
('大列表渲染卡顿掉帧', '5 万行报表明细渲染导致滚动掉帧', '虚拟滚动 + 分页加载，仅渲染可视区域，首屏 2.3s → 0.4s', '前端', 'done', '2026-08-07', NULL, 0),
('网关 Token 校验重复查 Redis', '每个请求重复解析 Token，多次查 Redis，接口延迟增加约 40ms', '请求链路内 Token 只解析一次 + 黑名单机制，耗时降低 60%', '后端', 'done', '2026-08-06', NULL, 0),
('跨域请求被浏览器拦截', '本地开发环境跨域请求被拦截，接口无法联调', '网关 CORS 白名单 + Vite/Nginx 代理转发', '前端', 'done', '2026-08-05', NULL, 0),
('Docker 容器内存溢出', '测试环境容器运行一段时间后被 OOM Kill', 'JVM 堆参数与容器限额对齐 + 健康检查自动重启', '运维', 'done', '2026-08-04', NULL, 0),
('定时任务偶发重复执行', '分布式环境下定时任务被多个节点同时触发，导致数据重复写入', '', '后端', 'open', NULL, NULL, 0),
('图片上传偶现 413 错误', '上传大图时网关返回 413，怀疑 Nginx client_max_body_size 限制', '', '运维', 'open', NULL, NULL, 0);
