# GraalVM 脚本执行服务

基于 GraalVM 和 Quarkus 实现的多语言脚本执行服务，支持 JavaScript、Python、Ruby 等多种脚本语言的安全执行。

## 功能特性

- 🚀 **多语言支持**: 支持 JavaScript、Python、Ruby 等脚本语言
- 🔒 **安全沙盒**: 可配置的脚本执行权限和资源限制
- 🎯 **函数调用**: 支持指定入口函数名称和参数传递
- 📡 **实时通信**: WebSocket 支持实时返回脚本执行日志和结果
- 🐳 **Docker 支持**: 完整的 Docker 镜像支持，包含 GraalVM 和脚本运行时
- 🌐 **Web 界面**: 提供友好的前端页面用于脚本测试和调试
- ⚡ **高性能**: 基于 Quarkus 和 GraalVM Native Image 的高性能实现
- 🔄 **反向代理支持**: 完整支持反向代理部署，自动处理URL路径问题

## 快速开始

### 开发环境要求

- Java 21+
- Maven 3.8+
- Docker (可选)
- GraalVM 21+ (用于 Native 构建)
- Python 3.7+ (用于测试脚本)

### 本地运行

### 使用测试脚本（推荐）

```bash
# 克隆项目
git clone <repository-url>
cd script-server

# 安装Python依赖
pip install -r requirements.txt

# 运行完整构建和测试
python test.py

# 或使用快速构建脚本
python build_fast.py
```

### 开发模式

```bash
./mvnw quarkus:dev
```

### 访问应用

- Web 界面: http://localhost:8080
- API 文档: http://localhost:8080/q/swagger-ui
- 健康检查: http://localhost:8080/api/script/health

### 构建和部署

#### 使用构建脚本

**Windows:**
```bash
build.bat
```

**Linux/macOS:**
```bash
chmod +x build.sh
./build.sh
```

#### 手动构建

1. **构建 JVM 版本**
```bash
./mvnw clean package
```

2. **构建 Native 版本**
```bash
./mvnw package -Dnative -DskipTests
```

3. **构建 Docker 镜像**
```bash
# JVM 镜像
docker build -f src/main/docker/Dockerfile.jvm -t script-server:jvm .

# Native 镜像
docker build -f src/main/docker/Dockerfile.native -t script-server:native .

# 完整 GraalVM 镜像
docker build -f src/main/docker/Dockerfile.graalvm -t script-server:graalvm .
```

### Docker 运行

```bash
# 运行完整 GraalVM 镜像（推荐）
docker run -p 8080:8080 script-server:graalvm

# 或运行 Native 镜像
docker run -p 8080:8080 script-server:native

# 或运行 JVM 镜像
docker run -p 8080:8080 script-server:jvm
```

## API 使用说明

### REST API

#### 执行脚本
```bash
POST /api/script/execute
Content-Type: application/json

{
  "script": "console.log('Hello World'); 1 + 2",
  "language": "js",
  "entryFunction": "",
  "args": [],
  "permissions": {
    "allowIO": false,
    "allowNetwork": false,
    "allowHostAccess": false,
    "allowFileAccess": false,
    "allowCreateThread": false,
    "allowEnvironmentAccess": false,
    "maxExecutionTime": 30000,
    "maxMemoryUsage": 134217728
  }
}
```

#### 获取支持的语言
```bash
GET /api/script/languages
```

#### 获取默认权限配置
```bash
GET /api/script/permissions/sandbox
GET /api/script/permissions/permissive
```

### WebSocket API

连接到 `ws://localhost:8080/ws/script` 发送相同格式的 JSON 消息，可获得实时执行反馈。

### 权限配置说明

| 权限 | 说明 |
|------|------|
| `allowIO` | 是否允许IO操作 |
| `allowNetwork` | 是否允许网络访问 |
| `allowHostAccess` | 是否允许主机访问 |
| `allowFileAccess` | 是否允许文件访问 |
| `allowCreateThread` | 是否允许创建线程 |
| `allowEnvironmentAccess` | 是否允许环境变量访问 |
| `maxExecutionTime` | 最大执行时间（毫秒） |
| `maxMemoryUsage` | 最大内存使用量（字节） |

## 脚本示例

### JavaScript 示例

**简单计算:**
```javascript
console.log("开始计算...");
let result = 1 + 2 + 3;
console.log("结果:", result);
result;
```

**函数调用:**
```javascript
function calculate(a, b, operation) {
    console.log(`执行 ${a} ${operation} ${b}`);
    switch(operation) {
        case "+": return a + b;
        case "-": return a - b;
        case "*": return a * b;
        case "/": return a / b;
        default: return "不支持的操作";
    }
}

// 调用方式：设置 entryFunction="calculate", args=[10, 5, "+"]
```

### Python 示例

```python
print("Hello from Python!")
result = sum([1, 2, 3, 4, 5])
print(f"求和结果: {result}")
result
```

### Ruby 示例

```ruby
puts "Hello from Ruby!"
result = (1..5).sum
puts "求和结果: #{result}"
result
```

## 🌐 反向代理支持

应用完全支持在反向代理后部署：

- **自动路径检测**: 处理子路径部署情况
- **头部转发**: 支持 `X-Forwarded-Host`、`X-Forwarded-Proto` 和 `X-Forwarded-Prefix` 头部
- **WebSocket 兼容**: WebSocket 连接在代理后正常工作
- **相对重定向**: 所有重定向使用相对路径避免代理问题

### 反向代理配置

```properties
# 启用代理支持
quarkus.http.proxy.proxy-address-forwarding=true
quarkus.http.proxy.allow-forwarded=true
quarkus.http.proxy.enable-forwarded-host=true
quarkus.http.proxy.enable-forwarded-prefix=true
quarkus.http.proxy.trusted-proxies=*
```

### 常见反向代理配置示例

#### Nginx 配置
```nginx
location /script-server/ {
    proxy_pass http://localhost:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Prefix /script-server;
    proxy_set_header X-Original-Host $host;  # 自定义原始主机头
    
    # WebSocket 支持
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

#### Cloud Run / Google Cloud Platform
```nginx
# Google Cloud Run会自动设置X-Forwarded-*头部
# 额外设置X-Original-Host用于域名识别
proxy_set_header X-Original-Host script-server-501458390533.asia-east2.run.app;
```

#### Apache 配置
```apache
ProxyPreserveHost On
ProxyPass /script-server/ http://localhost:8080/
ProxyPassReverse /script-server/ http://localhost:8080/

# 设置代理头部
Header always set X-Forwarded-Prefix "/script-server"
Header always set X-Original-Host "your-domain.com"
```

## 🧪 测试

项目包含完整的测试覆盖：

- **47个单元测试**: 完整覆盖核心功能
- **安全测试**: 权限和沙盒验证
- **集成测试**: REST API 和 WebSocket 测试
- **自动化测试**: 基于Python的端到端测试

```bash
# 运行单元测试
./mvnw test

# 运行完整集成测试
python test.py
```

## 📊 性能表现

- **快速启动**: Native 构建毫秒级启动
- **低内存占用**: GraalVM 优化的内存使用
- **并发执行**: 支持多个脚本并发执行
- **资源限制**: 可配置的内存和执行时间限制

## 🔒 安全考虑

- 默认使用严格的沙盒模式，禁用所有危险操作
- 支持细粒度的权限控制
- 设置执行时间和内存限制
- 建议在生产环境中使用容器隔离

## 🛠️ 技术栈

- **框架**: Quarkus 3.26.1
- **运行时**: GraalVM Community 21
- **语言支持**: JavaScript (GraalJS 24.1.0)、Python (GraalPy)、Ruby
- **构建工具**: Maven + Docker BuildKit
- **测试框架**: JUnit 5 + Python 自动化
- **通信协议**: REST API + WebSocket

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 开发指南

- 使用 `python test.py` 进行所有测试
- 新功能必须包含测试用例
- 遵循 Docker 优先的开发方式
- 保持安全最佳实践

## 📄 许可证

本项目采用 MIT 许可证。查看 [LICENSE](LICENSE) 文件了解更多信息。

## 📞 联系方式

- 项目主页: [GitHub Repository]
- 问题反馈: [GitHub Issues]
- 邮箱: your-email@example.com

---

## 相关文档

- [English Documentation](README.md)
- [使用指南](USAGE.md)
- [测试报告](TEST_REPORT.md)
