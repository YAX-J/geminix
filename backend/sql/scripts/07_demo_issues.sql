-- 07 问题演示数据
SET NAMES utf8mb4;
USE worklog;

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
