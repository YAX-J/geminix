-- 06 日报演示数据
SET NAMES utf8mb4;
USE worklog;

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
