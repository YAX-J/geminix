# 工作日志台（geminix）Docker 部署指南

> 适用：服务器已装 Docker / Docker Compose
> 假设 MySQL 8 与 Redis 已作为 **Docker 容器**运行（不是宿主机进程），worklog 容器通过容器名互通

---

## 〇、架构

```
浏览器 ──> 宿主机 :80  ──> [Nginx 容器] /usr/share/nginx/html
                          /api/* 反代 ──> [Spring Boot 容器] :8089
                                              │
                                              ├─ 容器 mysql (:3306)
                                              └─ 容器 redis (:6379)

所有容器（mysql/redis/backend/frontend）共享 worklog-net 网络，容器名即主机名
```

文件清单（**前后端已拆为两个仓库，需并排放置**，如 `/data/geminix` 与 `/data/geminix-front`）：

```
/data/geminix                      # 后端仓库（本仓库）
├── docker-compose.yml         # 编排 backend + frontend（frontend 构建上下文 = ../geminix-front）
├── .env.example                # 环境变量模板
└── backend/
    ├── Dockerfile              # 多阶段：maven 编译（阿里云镜像）+ JRE 21 运行
    ├── settings.xml            # Maven 阿里云镜像配置
    └── .dockerignore

/data/geminix-front                 # 前端仓库（github.com/YAX-J/geminix-front）
├── Dockerfile                 # 多阶段：node 构建（npmmirror 镜像）+ nginx 静态托管
├── nginx.conf                 # 静态 + /api 反代
└── .dockerignore
```

> `worklog-net` 网络为 **external**（手工 `docker network create` 预创建，mysql/redis 手工加入），
> compose 不托管该网络；删除网络前需先 `docker compose down`。

---

## 一、宿主机准备（MySQL/Redis 容器放行）

worklog 后端通过 **容器名** `mysql` / `redis` 直连，所以 MySQL/Redis 容器需要：

### 1.1 进入 MySQL 容器授权

```bash
docker exec -it mysql mysql -uroot -p
# 密码 = 启动 MySQL 容器时设置的 MYSQL_ROOT_PASSWORD
```

```sql
-- 推荐：建专用账号（生产可用）
CREATE DATABASE IF NOT EXISTS worklog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'worklog'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON worklog.* TO 'worklog'@'%';
FLUSH PRIVILEGES;
EXIT;
```

> **root 授权快速方案**（本地/测试环境）：
> `GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION; FLUSH PRIVILEGES;`
> 但 root 开放网络权限是安全大忌，**生产环境务必用 worklog 专用账号**。

### 1.2 导入表结构（演示数据已清空）

`backend/sql/init.sql` 脚本顶部已 `SET NAMES utf8mb4` 兜底，**推荐用容器内 mysql 客户端执行**（避免宿主机 socket 缺失）：

```bash
# 方式 1：管道导入（推荐）
docker exec -i mysql mysql -uroot -p < backend/sql/init.sql

# 方式 2：若报单包超限（max_allowed_packet），使用拆分版脚本
# ⚠️ 注意：backend/sql/scripts/ 拆分脚本为旧版（2026-08-10），
#    不含多用户字段（user_id / role）且含演示数据，与 init.sql 已不同步。
#    多用户环境请勿直接执行，需先从 init.sql 重新生成拆分脚本后再用。
cd backend/sql/scripts
for f in 01_*.sql 02_*.sql 03_*.sql 04_*.sql 05_*.sql 06_*.sql 07_*.sql; do
  docker exec -i mysql mysql -uroot -p < "$f"
done
```

### 1.3 Redis 验证

```bash
docker exec -it redis redis-cli ping
# 期望：PONG
```

如需密码：编辑 redis 容器配置或重建时加 `REDIS_PASSWORD`，密码填入 `.env` 的 `REDIS_PASSWORD=`。

---

## 二、把现有 MySQL/Redis 容器加入 worklog 网络

worklog 的 backend/frontend 容器在 `worklog-net` 桥接网络里，MySQL/Redis 必须也加入才能用容器名互访。

```bash
# 创建网络（docker compose up 会自动创建；如已有 worklog-net 可跳过）
docker network create worklog-net

# 把已有 MySQL/Redis 容器接进来（重复执行无副作用）
docker network connect worklog-net mysql
docker network connect worklog-net redis

# 验证：worklog-net 里的容器
docker network inspect worklog-net --format '{{range .Containers}}{{.Name}} {{.IPv4Address}}{{"\n"}}{{end}}'
```

> 若你的 MySQL/Redis 容器实际叫别的名字（如 `mysql8`、`redis-server`），把 `mysql` / `redis` 换成 `docker ps` 里 NAMES 列的实际名字，**同时改 `.env` 里的 `MYSQL_CONTAINER_NAME` / `REDIS_CONTAINER_NAME`**。

---

## 三、构建与启动

```bash
# 1) 上传代码到服务器（或 git clone）
cd /opt/geminix

# 2) 复制环境变量模板并填入真实值
cp .env.example .env
vi .env            # 改 DB_PASSWORD / JWT_SECRET / REDIS_PASSWORD
# 检查 MYSQL_CONTAINER_NAME / REDIS_CONTAINER_NAME 与 docker ps 输出一致

# 3) 构建并启动
docker compose up -d --build
# 老版 docker 用：docker-compose up -d --build

# 4) 查看状态
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
```

启动成功后：

```bash
# 验证后端（应返回 401：未登录被拦截 = 后端正常）
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8089/api/auth/me

# 验证前端（应返回 200）
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1/

# 验证容器互联（在 backend 容器里解析 mysql/redis 主机名）
docker exec -it worklog-backend sh -c "getent hosts mysql redis"
# 期望返回 mysql 和 redis 的容器内网 IP
```

浏览器打开 `http://服务器IP`，登录页 `admin / admin123` 即可使用。

---

## 四、日常运维

```bash
# 停止
docker compose stop

# 启动
docker compose start

# 完全停止并删除容器（不影响 MySQL/Redis 数据）
docker compose down

# 重新构建（代码有更新）
git pull
docker compose up -d --build

# 查看后端实时日志
docker compose logs -f --tail=200 backend
```

---

## 五、关键环境变量（.env）

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_CONTAINER_NAME` | MySQL 容器名（与 `docker ps` NAMES 一致） | `mysql` |
| `REDIS_CONTAINER_NAME` | Redis 容器名 | `redis` |
| `DB_USERNAME` | MySQL 用户 | `worklog` |
| `DB_PASSWORD` | MySQL 密码 | 强密码 |
| `REDIS_PORT` | Redis 容器内端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码（无密码留空） | 留空或强密码 |
| `JWT_SECRET` | HS256 密钥（≥32 字节，**生产必改**） | `openssl rand -base64 48` |
| `BACKEND_PORT` | 后端暴露到宿主机的端口 | `8089` |
| `FRONTEND_PORT` | 前端暴露到宿主机的端口 | `80` |

---

## 六、与 MySQL/Redis 通信的原理

`docker-compose.yml` 中所有服务都用 `networks: [worklog-net]`，Docker 自动 DNS 把容器名解析为同网段 IP。

后端配置：
- `DB_URL=jdbc:mysql://${MYSQL_CONTAINER_NAME}:3306/worklog?...` → 解析为 MySQL 容器 IP
- `REDIS_HOST=${REDIS_CONTAINER_NAME}` → 解析为 Redis 容器 IP

不需要 `host.docker.internal`，因为所有容器在同一网段里。

---

## 七、HTTPS（推荐）

容器内是 HTTP，由宿主机 Nginx 或 Caddy 处理 HTTPS 最简单。示例（Caddy 自动签发）：

```bash
sudo apt install -y caddy
echo "your-domain.com {
    reverse_proxy 127.0.0.1:80
}" | sudo tee /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

---

## 八、常见问题

| 现象 | 排查 |
|------|------|
| 后端日志 `Communications link failure` 连不上 MySQL | ① `docker exec -it worklog-backend sh -c "getent hosts mysql"` 看 DNS 解析 ② `docker network inspect worklog-net` 确认 mysql 已加入 ③ 容器内 `nc -zv mysql 3306` 看连通 ④ MySQL 容器内 `SELECT user,host FROM mysql.user` 看账号授权 |
| 后端日志连不上 Redis | ① `getent hosts redis` ② `docker exec -it redis redis-cli ping` ③ 密码 |
| `curl /api/auth/me` 返回 502 | 前端容器内 `wget -qO- http://backend:8089/api/auth/me` 看后端是否可达 |
| 18:00 提醒不准（容器时区） | 镜像已 `ENV TZ=Asia/Shanghai` + Alpine 装 tzdata；如仍偏差，删容器重建 |
| `JWT` 提示 "登录已过期" | 浏览器清 localStorage 重新登录（**JWT_SECRET 变了**会导致老 token 失效） |
| 修改代码后无效果 | `docker compose up -d --build` 重新构建，**仅重启不重新 build 不会更新代码** |
| 镜像拉取慢 | 加 registry mirror 到 `/etc/docker/daemon.json`（如阿里云 `registry.cn-hangzhou.aliyuncs.com`） |
| `ERROR 1044 Access denied for user 'root'@'%'` | 在 `docker exec -it mysql mysql -uroot -p` 里执行授权 SQL（见 1.1） |
| `ERROR 2002 socket '/var/run/mysqld/mysqld.sock' (2)` | 宿主机没装 MySQL，MySQL 在容器里。`sudo mysql` 改成 `docker exec -it mysql mysql -uroot -p` |

---

## 九、从现有部署迁移到 Docker

1. MySQL/Redis 容器不动，worklog 只起 backend/frontend 两个新容器
2. 旧后端进程（systemd 托管或裸 java）停掉
3. 旧前端 dist 删除（如果在同一台机器，端口 80 冲突）
4. 执行第二节 `docker compose up -d --build`
5. 老 token 失效需要重新登录

---

## 十、与宿主机本地服务（MySQL/Redis）部署的差异

| 场景 | 连接方式 |
|------|----------|
| MySQL 在宿主机（`localhost:3306`） | `DB_URL=jdbc:mysql://host.docker.internal:3306/...`，`extra_hosts: [host.docker.internal:host-gateway]` |
| MySQL 是 Docker 容器 | `DB_URL=jdbc:mysql://<容器名>:3306/...`，容器加入 `worklog-net` 网络 |

本项目**当前默认后者**（容器互联），适合你的服务器现状。
