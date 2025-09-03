####
# Multi-stage Dockerfile for building Quarkus application with GraalVM
# This builds the JAR inside Docker container and creates a runtime image
# Uses mount caches for faster builds
####

# Build stage - 使用GraalVM进行构建
FROM ghcr.io/graalvm/graalvm-community:21 AS builder

# 安装Maven
RUN microdnf update -y && \
    microdnf install -y wget tar gzip && \
    microdnf clean all

# 下载并安装Maven（使用缓存加速）
RUN --mount=type=cache,target=/tmp/maven-cache \
    if [ ! -f /tmp/maven-cache/apache-maven-3.9.6-bin.tar.gz ]; then \
        wget -q https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -O /tmp/maven-cache/apache-maven-3.9.6-bin.tar.gz; \
    fi && \
    tar -xzf /tmp/maven-cache/apache-maven-3.9.6-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# 下载依赖（利用Docker缓存和Maven本地仓库缓存）
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（使用Maven缓存）
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# Runtime stage - 使用GraalVM运行时
FROM ghcr.io/graalvm/graalvm-community:21

# 安装语言运行时
RUN microdnf clean all && microdnf --refresh && \
    microdnf update -y && \
    microdnf install -y python3 python3-pip curl && \
    microdnf clean all && \
    rm -rf /var/cache/yum

# 设置工作目录
WORKDIR /app

# 复制构建好的JAR文件
COPY --from=builder /app/target/quarkus-app/ ./

# 创建非root用户
RUN groupadd -r appuser && useradd -r -g appuser -m appuser && \
    chown -R appuser:appuser /app
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/script/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
