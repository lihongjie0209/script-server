# 使用说明

## 环境要求

- Python 3.7+
- Docker with BuildKit support
- pip

## 快速开始

### 1. 安装Python依赖

```bash
pip install -r requirements.txt
```

### 2. 快速构建（推荐）

```bash
# 使用优化构建脚本
python build_fast.py

# 或者使用传统方式
python test.py
```

### 3. 使用Docker Compose（开发环境）

```bash
# 启动服务
docker-compose up --build

# 后台运行
docker-compose up -d --build

# 停止服务
docker-compose down
```

## 构建优化

项目使用了Docker BuildKit的缓存挂载功能来加速构建：

### BuildKit缓存功能
- `--mount=type=cache,target=/root/.m2` - Maven本地仓库缓存
- `--mount=type=cache,target=/var/cache/microdnf` - 系统包管理器缓存
- `--mount=type=cache,target=/var/lib/rpm` - RPM包缓存

### 手动构建选项

```bash
# 1. 启用BuildKit标准构建
DOCKER_BUILDKIT=1 docker build --progress=plain -t script-server:latest .

# 2. 使用内联缓存
DOCKER_BUILDKIT=1 docker build --progress=plain --build-arg BUILDKIT_INLINE_CACHE=1 -t script-server:latest .

# 3. 使用buildx（推荐）
docker buildx create --name mybuilder --use
docker buildx build --progress=plain --load -t script-server:latest .

# 4. 清除缓存重新构建
docker build --progress=plain --no-cache -t script-server:latest .
```

## 依赖优化

项目使用了正确的GraalVM Polyglot依赖配置：

```xml
<!-- GraalVM Polyglot API 核心依赖 -->
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>23.1.0</version>
</dependency>

<!-- GraalVM JavaScript引擎 -->
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>js</artifactId>
    <version>23.1.0</version>
    <type>pom</type>
</dependency>

<!-- GraalVM Python引擎 -->
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>python</artifactId>
    <version>23.1.0</version>
    <type>pom</type>
</dependency>
```

## 访问服务

- Web界面: http://localhost:8080
- REST API: http://localhost:8080/api/script/
- WebSocket: ws://localhost:8080/ws/script

## API示例

### REST API调用

```bash
curl -X POST http://localhost:8080/api/script/execute \
  -H "Content-Type: application/json" \
  -d '{
    "script": "console.log(\"Hello World\"); 1 + 2",
    "language": "js",
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
  }'
```

### WebSocket示例（JavaScript）

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/script');

ws.onopen = function() {
    ws.send(JSON.stringify({
        script: 'console.log("Hello WebSocket"); 42',
        language: 'js',
        permissions: {
            allowIO: false,
            allowNetwork: false,
            allowHostAccess: false,
            allowFileAccess: false,
            allowCreateThread: false,
            allowEnvironmentAccess: false,
            maxExecutionTime: 30000,
            maxMemoryUsage: 134217728
        }
    }));
};

ws.onmessage = function(event) {
    console.log('收到消息:', JSON.parse(event.data));
};
```

## 脚本示例

### JavaScript

```javascript
// 简单计算
let a = 10;
let b = 20;
console.log(`${a} + ${b} = ${a + b}`);
a + b;

// 函数调用（设置entryFunction为"main"）
function main(name, age) {
    console.log(`Hello ${name}, you are ${age} years old`);
    return `Welcome ${name}!`;
}
```

### Python

```python
# Hello World
print("Hello from Python!")
result = sum(range(1, 11))
print(f"1到10的和是: {result}")
result

# 函数调用（设置entryFunction为"calculate"）
def calculate(x, y, op):
    if op == "add":
        return x + y
    elif op == "multiply":
        return x * y
    else:
        return "Unknown operation"
```

## 权限配置

| 权限 | 说明 | 建议 |
|------|------|------|
| allowIO | 允许IO操作 | 生产环境建议关闭 |
| allowNetwork | 允许网络访问 | 根据需要开启 |
| allowHostAccess | 允许主机访问 | 谨慎开启 |
| allowFileAccess | 允许文件访问 | 根据需要开启 |
| allowCreateThread | 允许创建线程 | 通常关闭 |
| allowEnvironmentAccess | 允许环境变量访问 | 根据需要开启 |
| maxExecutionTime | 最大执行时间（毫秒） | 建议30秒以内 |
| maxMemoryUsage | 最大内存使用（字节） | 根据服务器配置 |

## 故障排除

### 容器启动失败
```bash
# 查看详细日志
docker logs script-server

# 检查端口占用
netstat -an | grep 8080
```

### 脚本执行失败
- 检查脚本语法
- 确认权限配置
- 查看错误日志

### WebSocket连接失败
- 确认服务正常运行
- 检查防火墙设置
- 验证URL格式
