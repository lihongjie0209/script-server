# GraalVM Script Execution Service

A secure multi-language script execution service based on GraalVM and Quarkus framework, supporting JavaScript, Python, Ruby and other scripting languages with configurable sandbox permissions.

## 🚀 Features

- **Multi-language Support**: JavaScript, Python, Ruby and more scripting languages
- **Security Sandbox**: Configurable script execution permissions and resource limits
- **Function Invocation**: Support for entry function names and parameter passing
- **Real-time Communication**: WebSocket support for real-time script execution logs and results
- **Docker Ready**: Complete Docker image support with GraalVM and script runtimes
- **Web Interface**: User-friendly frontend for script testing and debugging
- **High Performance**: High-performance implementation based on Quarkus and GraalVM Native Image
- **Reverse Proxy Support**: Full support for reverse proxy deployments with proper URL handling

## 📋 Requirements

- Java 21+
- Maven 3.8+
- Docker (optional)
- GraalVM 21+ (for Native builds)
- Python 3.7+ (for testing scripts)

## 🏃‍♂️ Quick Start

### Using Test Script (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd script-server

# Install Python dependencies
pip install -r requirements.txt

# Run complete build and test
python test.py

# Or use fast build script
python build_fast.py
```

### Development Mode

```bash
./mvnw quarkus:dev
```

### Access the Application

- **Web Interface**: http://localhost:8080
- **API Documentation**: http://localhost:8080/q/swagger-ui
- **Health Check**: http://localhost:8080/api/script/health

## 🐳 Docker Deployment

### Quick Deploy

```bash
# Using Docker Compose (recommended for development)
docker-compose up --build

# Or build and run manually
DOCKER_BUILDKIT=1 docker build -t script-server:latest .
docker run -p 8080:8080 script-server:latest
```

### Available Docker Images

```bash
# Full GraalVM image (supports all languages)
docker build -f src/main/docker/Dockerfile.graalvm -t script-server:graalvm .

# Native image (fastest startup)
docker build -f src/main/docker/Dockerfile.native -t script-server:native .

# JVM image (development)
docker build -f src/main/docker/Dockerfile.jvm -t script-server:jvm .
```

## 📡 API Usage

### REST API

#### Execute Script
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

#### Get Supported Languages
```bash
GET /api/script/languages
```

#### Get Permission Presets
```bash
GET /api/script/permissions/sandbox
GET /api/script/permissions/permissive
```

### WebSocket API

Connect to `ws://localhost:8080/ws/script` and send JSON messages in the same format as REST API for real-time execution feedback.

## 🔒 Security & Permissions

| Permission | Description | Production Recommendation |
|------------|-------------|---------------------------|
| `allowIO` | Allow I/O operations | Disabled |
| `allowNetwork` | Allow network access | As needed |
| `allowHostAccess` | Allow host access | Use with caution |
| `allowFileAccess` | Allow file access | As needed |
| `allowCreateThread` | Allow thread creation | Usually disabled |
| `allowEnvironmentAccess` | Allow environment variable access | As needed |
| `maxExecutionTime` | Maximum execution time (ms) | ≤ 30000 |
| `maxMemoryUsage` | Maximum memory usage (bytes) | Based on server capacity |

## 📝 Script Examples

### JavaScript

```javascript
// Simple calculation
console.log("Starting calculation...");
let result = 1 + 2 + 3;
console.log("Result:", result);
result;

// Function call (set entryFunction="calculate", args=[10, 5, "+"])
function calculate(a, b, operation) {
    console.log(`Executing ${a} ${operation} ${b}`);
    switch(operation) {
        case "+": return a + b;
        case "-": return a - b;
        case "*": return a * b;
        case "/": return a / b;
        default: return "Unsupported operation";
    }
}
```

### Python

```python
print("Hello from Python!")
result = sum([1, 2, 3, 4, 5])
print(f"Sum result: {result}")
result

# Function call (set entryFunction="fibonacci", args=[10])
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)
```

## 🏗️ Build Options

### Standard Build
```bash
./mvnw clean package
```

### Native Build
```bash
./mvnw package -Dnative -DskipTests
```

### Docker Build with BuildKit
```bash
# Standard build
DOCKER_BUILDKIT=1 docker build --progress=plain -t script-server:latest .

# With cache
DOCKER_BUILDKIT=1 docker build --progress=plain --build-arg BUILDKIT_INLINE_CACHE=1 -t script-server:latest .

# Using buildx (recommended)
docker buildx build --progress=plain --load -t script-server:latest .
```

## 🌐 Reverse Proxy Support

The application fully supports deployment behind reverse proxies:

- **Automatic path detection**: Handles sub-path deployments
- **Header forwarding**: Respects `X-Forwarded-Host`, `X-Forwarded-Proto`, and `X-Forwarded-Prefix` headers
- **WebSocket compatibility**: WebSocket connections work correctly behind proxies
- **Relative redirects**: All redirects use relative paths to avoid proxy issues

### Configuration for Reverse Proxy

```properties
# Enable proxy support
quarkus.http.proxy.proxy-address-forwarding=true
quarkus.http.proxy.allow-forwarded=true
quarkus.http.proxy.enable-forwarded-host=true
quarkus.http.proxy.enable-forwarded-prefix=true
quarkus.http.proxy.trusted-proxies=*
```

### Proxy Configuration Examples

#### Nginx Configuration
```nginx
location /script-server/ {
    proxy_pass http://localhost:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Prefix /script-server;
    proxy_set_header X-Original-Host $host;  # Custom original host header
    
    # WebSocket support
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

#### Cloud Run / Google Cloud Platform
```nginx
# Google Cloud Run automatically sets X-Forwarded-* headers
# Additionally set X-Original-Host for domain identification
proxy_set_header X-Original-Host script-server-501458390533.asia-east2.run.app;
```

#### Apache Configuration
```apache
ProxyPreserveHost On
ProxyPass /script-server/ http://localhost:8080/
ProxyPassReverse /script-server/ http://localhost:8080/

# Set proxy headers
Header always set X-Forwarded-Prefix "/script-server"
Header always set X-Original-Host "your-domain.com"
```

## 🧪 Testing

The project includes comprehensive testing:

- **47 Unit Tests**: Complete coverage of core functionality
- **Security Tests**: Permission and sandbox validation
- **Integration Tests**: REST API and WebSocket testing
- **Automated Testing**: Python-based end-to-end testing

```bash
# Run unit tests
./mvnw test

# Run complete integration tests
python test.py
```

## 🛠️ Technology Stack

- **Framework**: Quarkus 3.26.1
- **Runtime**: GraalVM Community 21
- **Languages**: JavaScript (GraalJS 24.1.0), Python (GraalPy), Ruby
- **Build Tools**: Maven + Docker BuildKit
- **Testing**: JUnit 5 + Python automation
- **Communication**: REST API + WebSocket

## 📊 Performance

- **Fast Startup**: Native builds start in milliseconds
- **Low Memory**: Optimized memory usage with GraalVM
- **Concurrent Execution**: Support for multiple concurrent script executions
- **Resource Limits**: Configurable memory and execution time limits

## 🤝 Contributing

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

- Use `python test.py` for all testing
- All new features must include tests
- Follow the Docker-first development approach
- Maintain security best practices

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 📞 Contact

- Project Homepage: [GitHub Repository]
- Issues: [GitHub Issues]
- Email: your-email@example.com

---

## Related Documentation

- [中文文档](README_CN.md)
- [Usage Guide](USAGE.md)
- [Test Report](TEST_REPORT.md)
