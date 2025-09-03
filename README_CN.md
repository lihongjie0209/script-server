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

## 快速开始

### 开发环境要求

- Java 21+
- Maven 3.8+
- Docker (可选)
- GraalVM 21+ (用于 Native 构建)

### 本地运行

1. **克隆项目**
```bash
git clone <repository-url>
cd script-server
```

2. **开发模式运行**
```bash
./mvnw quarkus:dev
```

3. **访问应用**
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

## 安全考虑

- 默认使用严格的沙盒模式，禁用所有危险操作
- 支持细粒度的权限控制
- 设置执行时间和内存限制
- 建议在生产环境中使用容器隔离

## 技术栈

- **Quarkus**: 云原生 Java 框架
- **GraalVM**: 高性能多语言虚拟机
- **WebSocket**: 实时通信支持
- **Docker**: 容器化部署
- **RESTEasy**: REST API 实现

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证。查看 [LICENSE](LICENSE) 文件了解更多信息。

## 联系方式

- 项目主页: [GitHub Repository]
- 问题反馈: [GitHub Issues]
- 邮箱: your-email@example.com
