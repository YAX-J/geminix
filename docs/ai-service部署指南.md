# ai-service 部署指南（P1 向量检索服务）

> 服务：FastAPI + sentence-transformers(bge-small-zh-v1.5) + Milvus + MySQL
> 功能：把 worklog 库的日报/问题向量化写入 Milvus（集合 `geminix_docs`，512 维 COSINE+IVF_FLAT），提供语义检索接口
> 日期：2026-08-20

## 一、服务结构

```
ai-service/
├── app/
│   ├── main.py           # FastAPI 入口（health / sync / search）
│   ├── config.py         # .env 配置读取
│   ├── embedder.py       # bge-small-zh-v1.5 向量化（查询带检索指令前缀）
│   ├── db.py             # MySQL 读 report/issue
│   ├── milvus_client.py  # 集合管理（geminix_docs）+ 检索
│   ├── sync.py           # 同步管道：MySQL → 分块 → embedding → Milvus 全量重建
│   └── schemas.py
├── requirements.txt
├── .env.example          # 复制为 .env（真实密码不入库）
└── .env                  # 本地/服务器实际配置（gitignored）
```

## 二、接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/health` | 健康检查 |
| POST | `/api/ai/sync` | 全量重建向量库（drop + 重灌，数据量小所以最简） |
| GET | `/api/ai/search?q=关键词&k=5` | 语义检索，返回命中片段 + 来源（type/ref_id/ref_date/title/project） |

## 三、本地开发验证（Windows）

```bash
cd ai-service
py -m venv .venv                     # 或 python3 -m venv
.venv/Scripts/pip install -r requirements.txt
copy .env.example .env               # 改真实 MySQL/Milvus 地址
.venv/Scripts/python -m uvicorn app.main:app --host 0.0.0.0 --port 8010
# 验证：
curl http://127.0.0.1:8010/api/ai/health
curl -X POST http://127.0.0.1:8010/api/ai/sync
curl "http://127.0.0.1:8010/api/ai/search?q=缓存击穿"
```

## 四、服务器部署（Ubuntu + Python 3.10，CPU）

```bash
# 1. 上传代码（rsync 或 git pull），进入目录
cd /opt/geminix/ai-service

# 2. 虚拟环境 + 依赖（国内镜像）
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 3. 下载 embedding 模型（modelscope，国内快）
.venv/bin/pip install modelscope -i https://pypi.tuna.tsinghua.edu.cn/simple
.venv/bin/modelscope download --model BAAI/bge-small-zh-v1.5 --local_dir ./models/bge-small-zh-v1.5

# 4. 配置 .env（密码含 @ 等特殊字符需 URL 编码，如 Worklog@2026 → Worklog%402026）
cat > .env <<'EOF'
MYSQL_URL=mysql+pymysql://worklog:<密码URL编码>@127.0.0.1:3306/worklog?charset=utf8mb4
MILVUS_HOST=127.0.0.1
MILVUS_PORT=19530
MILVUS_COLLECTION=geminix_docs
EMBED_MODEL=/opt/geminix/ai-service/models/bge-small-zh-v1.5
EMBED_DIM=512
CHUNK_SIZE=500
EOF

# 5. 启动（systemd 更稳，这里先 nohup 验证）
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8010 > /var/log/geminix-ai.log 2>&1 &

# 6. 验证
curl http://127.0.0.1:8010/api/ai/health
curl -X POST http://127.0.0.1:8010/api/ai/sync
curl "http://127.0.0.1:8010/api/ai/search?q=简历亮点"
```

> 服务器说明：Milvus 19530 已在宿主机映射；MySQL 3306 已在宿主机映射（worklog 账号）。
> 内存 7G、无 GPU：bge-small-zh-v1.5 约 100MB 模型，CPU 推理单条 <100ms，数据量小无压力。

## 六、已验证（2026-08-20，本地连远程库实测）

- 同步：`sync_all()` → 583 chunks 全部入库（Milvus 集合 `geminix_docs`，MilvusClient API）
- 检索：`GET /api/ai/search?q=缓存击穿` → 命中 2026-07-24「JD 生成性能优化」日报，带 score/来源/片段
- 已知坑：①pymilvus 3.x 的 ORM Collection API 已弃用且在 Milvus v3.0 上静默失败，必须用 `MilvusClient`（uri 需 `tcp://` 或 `http://` 前缀）；②MySQL 密码含 `@` 需 URL 编码 `%40`；③`tasks` JSON 列 pymysql 返回字符串，需 `json.loads` 解析

## 五、与主站集成（P1.2 预留）

- 后端 `SearchController` 增加语义搜索：转发 `/api/ai/search`，与 SQL LIKE 结果混排
- 前端搜索框下拉展示语义命中（带来源类型徽标），点击跳转日报/问题
- 日报/问题增删改后触发 `POST /api/ai/sync`（或定时 5 分钟增量同步）
