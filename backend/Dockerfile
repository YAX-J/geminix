# ============================================================
# 多阶段构建：Maven 编译 + JRE 运行
# ============================================================

# ---- 阶段 1：构建 ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先拷贝 pom.xml 并下载依赖（利用 Docker 层缓存：源码变更不重下依赖）
COPY pom.xml .
# 国内服务器走阿里云 Maven 镜像加速
COPY settings.xml /root/.m2/settings.xml
RUN mvn -B -ntp dependency:go-offline

# 拷贝源码并打包
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests \
    && cp target/worklog-backend-*.jar /build/app.jar

# ---- 阶段 2：运行时 ----
FROM eclipse-temurin:21-jre-alpine

# 设置时区为东八区（@Scheduled cron "0 0 18 * * ?" 才能按北京时间 18:00 触发）
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone \
    && apk del tzdata

WORKDIR /opt/app

# 从构建阶段拷贝 fat jar
COPY --from=builder /build/app.jar /opt/app/app.jar

# 非 root 运行
RUN addgroup -S worklog && adduser -S worklog -G worklog
USER worklog

# JVM 参数（堆内存可按需调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8089

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8089/actuator/health 2>/dev/null \
   || wget -qO- http://127.0.0.1:8089/api/auth/me 2>/dev/null \
   || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/app/app.jar"]