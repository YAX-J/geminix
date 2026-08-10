// 演示数据：后端就绪后由 /api 接口替代（见 src/api 与 stores 中 mock 开关）

// 日报：只含工作内容，与问题解耦
export const demoReports = [
  {
    id: 1,
    date: '2026-08-10',
    week: '星期一',
    isToday: true,
    time: '09:30 - 18:20',
    title: '订单模块性能优化 & 缓存改造',
    tags: ['后端', 'Redis'],
    tasks: [
      '完成订单列表接口压测，QPS 由 800 提升至 2400',
      '订单详情接入 Redis 缓存，命中率 92%',
      '修复库存扣减并发场景下的超卖问题'
    ]
  },
  {
    id: 2,
    date: '2026-08-07',
    week: '星期五',
    time: '10:00 - 19:00',
    title: '报表导出功能联调',
    tags: ['前端', '数据库'],
    tasks: ['完成月度报表导出接口联调', '优化导出文件生成流程，支持后台异步任务']
  },
  {
    id: 3,
    date: '2026-08-06',
    week: '星期四',
    time: '09:00 - 18:00',
    title: '登录模块接入 JWT + 网关鉴权',
    tags: ['后端'],
    tasks: ['完成 JWT 签发与刷新流程', '网关统一接入 Token 校验过滤器']
  },
  {
    id: 4,
    date: '2026-08-05',
    week: '星期三',
    time: '09:30 - 18:30',
    title: '前端组件库搭建与页面开发',
    tags: ['前端'],
    tasks: ['完成基础组件库目录搭建（Button / Table / Form）', '开发日报列表页与详情抽屉']
  },
  {
    id: 5,
    date: '2026-08-04',
    week: '星期二',
    time: '09:00 - 17:40',
    title: '测试环境部署与 CI 流程',
    tags: ['运维'],
    tasks: ['编写 Dockerfile 并接入 Jenkins 流水线', '完成测试环境一键部署']
  },
  {
    id: 6,
    date: '2026-08-03',
    week: '星期一',
    time: '09:00 - 18:00',
    title: '项目初始化与技术选型',
    tags: ['后端', '前端', '数据库'],
    tasks: [
      '完成 Spring Boot + MySQL + Redis 基础工程搭建',
      '确定 Vue3 + Vite 前端工程结构',
      '设计日报、问题、标签核心表结构'
    ]
  }
]

// 问题：独立实体，status = open(待解决) | done(已解决)，reportDate 为可选的日报关联
// solutions：多方案数组（content 内容 / best 是否最佳）；favorite：收藏置顶
export const demoIssues = [
  {
    id: 1,
    title: 'Redis 缓存击穿导致慢查询',
    desc: '热点订单数据缓存击穿，瞬时高并发请求直接打到 MySQL，出现多条慢查询',
    solution: '本地缓存 + Redis 分布式锁双重兜底，缓存设置逻辑过期并异步刷新',
    solutions: [
      { content: '本地缓存 + Redis 分布式锁双重兜底，缓存设置逻辑过期并异步刷新', best: true },
      { content: '热点 key 预生成 + 互斥重建，击穿窗口内只允许一个线程回源', best: false }
    ],
    favorite: true,
    tag: 'Redis',
    status: 'done',
    createdAt: '2026-08-10',
    reportDate: '2026-08-10'
  },
  {
    id: 2,
    title: '报表查询 N+1 导致接口超时',
    desc: '报表明细查询在循环中逐条查库，SQL 执行 200+ 次',
    solution: '关联查询 + 批量 IN 替代循环单查，SQL 次数降到 3 次',
    solutions: [{ content: '关联查询 + 批量 IN 替代循环单查，SQL 次数降到 3 次', best: true }],
    favorite: false,
    tag: '数据库',
    status: 'done',
    createdAt: '2026-08-07',
    reportDate: '2026-08-07'
  },
  {
    id: 3,
    title: '大列表渲染卡顿掉帧',
    desc: '5 万行报表明细渲染导致滚动掉帧',
    solution: '虚拟滚动 + 分页加载，仅渲染可视区域，首屏 2.3s → 0.4s',
    tag: '前端',
    status: 'done',
    favorite: false,
    createdAt: '2026-08-07',
    reportDate: '2026-08-07'
  },
  {
    id: 4,
    title: '网关 Token 校验重复查 Redis',
    desc: '每个请求重复解析 Token，多次查 Redis，接口延迟增加约 40ms',
    solution: '请求链路内 Token 只解析一次 + 黑名单机制，耗时降低 60%',
    tag: '后端',
    status: 'done',
    favorite: false,
    createdAt: '2026-08-06',
    reportDate: '2026-08-06'
  },
  {
    id: 5,
    title: '跨域请求被浏览器拦截',
    desc: '本地开发环境跨域请求被拦截，接口无法联调',
    solution: '网关 CORS 白名单 + Vite/Nginx 代理转发',
    tag: '前端',
    status: 'done',
    favorite: false,
    createdAt: '2026-08-05',
    reportDate: '2026-08-05'
  },
  {
    id: 6,
    title: 'Docker 容器内存溢出',
    desc: '测试环境容器运行一段时间后被 OOM Kill',
    solution: 'JVM 堆参数与容器限额对齐 + 健康检查自动重启',
    tag: '运维',
    status: 'done',
    favorite: false,
    createdAt: '2026-08-04',
    reportDate: '2026-08-04'
  },
  {
    id: 7,
    title: '定时任务偶发重复执行',
    desc: '分布式环境下定时任务被多个节点同时触发，导致数据重复写入（独立记录，暂无关联日报）',
    solution: '',
    tag: '后端',
    status: 'open',
    favorite: false,
    createdAt: '2026-08-10',
    reportDate: ''
  },
  {
    id: 8,
    title: '图片上传偶现 413 错误',
    desc: '上传大图时网关返回 413，怀疑 Nginx client_max_body_size 限制（独立记录，暂无关联日报）',
    solution: '',
    tag: '运维',
    status: 'open',
    favorite: false,
    createdAt: '2026-08-07',
    reportDate: ''
  }
]

export const tagClassMap = {
  后端: 'tag-backend',
  前端: 'tag-frontend',
  数据库: 'tag-db',
  Redis: 'tag-redis',
  运维: 'tag-ops'
}

export const weekCN = ['日', '一', '二', '三', '四', '五', '六']
