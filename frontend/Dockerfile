# ============================================================
# 多阶段构建：Node 构建 + Nginx 托管
# ============================================================

# ---- 阶段 1：构建 ----
FROM node:20-alpine AS builder

WORKDIR /build

# 先拷贝 package.json 装依赖（利用缓存：源码变更不重装）
COPY package.json package-lock.json ./
# 国内服务器可加 --registry=https://registry.npmmirror.com
RUN npm ci --no-audit --no-fund

# 拷贝源码并构建
COPY . .
ARG VITE_API_BASE=/api
ENV VITE_API_BASE=${VITE_API_BASE}
RUN npm run build

# ---- 阶段 2：运行时 ----
FROM nginx:1.27-alpine

# 删除默认配置，替换为自定义
RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf

# 拷贝构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]