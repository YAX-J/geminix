# 工作日志台（geminix）Docker 部署指南

> 适用：服务器已装 Docker / Docker Compose
> 假设 MySQL 8 与 Redis 已部署在 **宿主机**（容器外），后端容器通过 `host.docker.internal` 访问

---

## 〇、架构

```
浏览器 ──> 宿主机 :80  ──> [Nginx 容器] /usr/share/nginx/html
                          /api/* 反代 ──> [Spring Boot 容器] :8089
                                              │
                                              ├─ 宿主机 MySQL :3306
                                              └─ 宿主机 Redis :6379
```

文件清单：

```
D:\project\geminix
├── docker-compose.yml         # 编排
├── .env.example                # 环境变量模板
├── backend/
│   ├── Dockerfile              # 多阶段：maven 编译 + JRE 21 运行
│   └── .dockerignore
└── frontend/
    ├── Dockerfile              # 多阶段：node 构建 + Nginx 托管
    ├── nginx.conf              # 静态 + /api 反代
    └── .dockerignore
```

---

## 一、宿主机准备（MySQL/Redis 放行容器）

容器通过 `host.docker.internal` 解析为宿主机 IP，但**宿主机服务必须监听 0.0.0.0**（或对应容器网段），并且 **MySQL 用户允许容器主机访问**。

### 1.1 MySQL 8（你的服务器已有 mysql:8.0 镜像）

```bash
# 1) 让 MySQL 监听所有网络接口（默认通常 127.0.0.1）
sudo vi /etc/mysql/mysql.conf.d/mysqld.cnf
#   bind-address = 0.0.0.0
sudo systemctl restart mysql

# 2) 创建/授权用户：允许 worklog 用户从任意主机连接
sudo mysql -uroot -p
```

```sql
-- 如果用户已存在，仅刷新权限
CREATE USER IF NOT EXISTS 'worklog'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON worklog.* TO 'worklog'@'%';
FLUSH PRIVILEGES;

-- 导入表结构 + 演示数据（脚本顶部已 SET NAMES utf8mb4 兜底中文乱码）
USE worklog;
SOURCE /opt/geminix/backend/sql/init.sql;
```

### 1.2 Redis（你的服务器已有 redis:latest 镜像）

```bash
# 1) 编辑 redis.conf 让其监听所有接口（或指定容器网段 172.17.0.0/16）
sudo vi /etc/redis/redis.conf
#   bind 0.0.0.0        # 改为全网（生产建议加密码 + 防火墙限制）
#   protected-mode no   # 或保留 yes 但设置 requirepass
#   requirepass your_redis_password  # 启用密码（推荐）

sudo systemctl restart redis
```

如果启用密码，把 `.env` 中 `REDIS_PASSWORD=your_redis_password` 填上即可。

---

## 二、构建与启动

```bash
# 1) 上传代码到服务器（或 git clone）
cd /opt/geminix

# 2) 复制环境变量模板并填入真实值
cp .env.example .env
vi .env            # 修改 DB_PASSWORD / JWT_SECRET / REDIS_PASSWORD

# 3) 构建并启动
docker compose up -d --build
# 老版 docker 用：docker-compose up -d --build

# 4) 查看状态
docker compose ps
docker compose logs -f backend     # 实时查看后端日志
docker compose logs -f frontend    # 实时查看前端日志
```

启动成功后：

```bash
# 验证后端存活
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8089/api/auth/me
# 期望：401（无 token 被拦截 = 后端正常）

# 验证前端可访问
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1/
# 期望：200
```

浏览器打开 `http://服务器IP`，登录页 `admin / admin123` 即可使用。

---

## 三、日常运维

```bash
# 停止（保留数据卷与容器）
docker compose stop

# 启动
docker compose start

# 完全停止并删除容器（不影响代码与数据库）
docker compose down

# 重新构建（代码有更新时）
git pull
docker compose up -d --build

# 查看资源占用
docker stats worklog-backend worklog-frontend

# 进入后端容器调试
docker compose exec backend sh
#   > ls /opt/app
#   > env | grep -E 'DB_|REDIS_|JWT_'

# 查看后端实时日志
docker compose logs -f --tail=200 backend
```

---

## 四、关键环境变量（.env）

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_USERNAME` | MySQL 用户 | `worklog` |
| `DB_PASSWORD` | MySQL 密码 | 强密码 |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码（无密码留空） | 留空或强密码 |
| `JWT_SECRET` | HS256 密钥（≥32 字节，**生产必改**） | `openssl rand -base64 48` |
| `JWT_EXPIRE_HOURS` | token 有效期 | `24` |
| `BACKEND_PORT` | 后端暴露到宿主机的端口 | `8089` |
| `FRONTEND_PORT` | 前端暴露到宿主机的端口 | `80` |

`.env` 已被 `.gitignore` 忽略，**真实值不会进仓库**。`.env.example` 保留作模板参考。

---

## 五、与宿主机 MySQL/Redis 通信的原理

`docker-compose.yml` 中：

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

`host-gateway` 是 Docker 18.03+ 提供的特殊值，Compose 会自动解析为宿主机在 `bridge` 网络上的网关 IP（典型为 `172.17.0.1`）。后端配置 `DB_URL=jdbc:mysql://host.docker.internal:3306/...` 与 `REDIS_HOST=host.docker.internal` 即可访问宿主服务。

如果你的 Docker 版本较旧（< 18.03），把 `host.docker.internal` 替换为宿主机实际内网 IP（如 `192.168.1.100`），并确保防火墙放行。

---

## 六、HTTPS（推荐）

容器内是 HTTP，由宿主机 Nginx 或 Caddy 处理 HTTPS 最简单。示例（Caddy，自动签发）：

```bash
sudo apt install -y caddy
echo "your-domain.com {
    reverse_proxy 127.0.0.1:80
}" | sudo tee /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

或复用已有的宿主机 Nginx，新增 server block 监听 443 + certbot 签证书，`proxy_pass http://127.0.0.1:80` 即可。

---

## 七、常见问题

| 现象 | 排查 |
|------|------|
| `curl /api/auth/me` 返回 502 | 浏览器/外部访问时检查：① 前端容器是否启动 ② `docker compose logs frontend` 看 nginx 错误 ③ backend 健康检查是否通过 |
| 后端日志 `Communications link failure` 连不上 MySQL | ① MySQL `bind-address` 是否 `0.0.0.0` ② `worklog@%` 用户是否授权 ③ 宿主机 `ss -tlnp \| grep 3306` 确认 3306 监听全部接口 |
| 后端日志连不上 Redis | ① `redis-cli -h 127.0.0.1 ping` 本机可连 ② `bind 0.0.0.0` 或防火墙放行 ③ 密码填入 `REDIS_PASSWORD` |
| 18:00 提醒不准（容器时区） | 镜像已 `ENV TZ=Asia/Shanghai` + Alpine 装 tzdata；如仍偏差，删容器重建 |
| `JWT` 提示 "登录已过期" | 浏览器清 localStorage 重新登录（**JWT_SECRET 变了**会导致老 token 失效） |
| 修改代码后无效果 | `docker compose up -d --build` 重新构建，**仅重启不重新 build 不会更新代码** |
| 镜像拉取慢 | 国内服务器可加 registry mirror 到 `/etc/docker/daemon.json`（如阿里云 `registry.cn-hangzhou.aliyuncs.com`） |

---

## 八、从现有部署迁移到 Docker

1. 现有数据在宿主机 MySQL/Redis，**无需迁移**——容器直接连过去
2. 停掉旧后端进程：`systemctl stop worklog`（如有）
3. 启动 Docker：`docker compose up -d --build`
4. 前端访问从宿主机静态文件改为 Docker 容器（端口 80），Nginx/Caddy 反代配置更新即可
5. 旧前端的 `dist` 可保留作回滚备份
