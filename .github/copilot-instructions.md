# GraalVM 脚本执行服务 - 开发指南

## 项目概述
这是一个基于 GraalVM 的多语言脚本执行服务，支持 JavaScript 和 Python 脚本的安全执行，提供 REST API 和 WebSocket 实时通信。

## 开发规范

### 1. 构建、运行和测试流程
**重要：所有的构建、运行和测试必须使用以下命令：**

```bash
python test.py
```

- `test.py` 脚本已经封装了完整的 Docker 构建、运行和测试流程
- 包括自动化的 REST API 测试、WebSocket 测试和 Web 界面测试
- 支持自动化的健康检查和错误诊断
- 默认保持容器运行以便手动测试

### 2. 测试维护规范
**所有测试必须维护在 `test.py` 中，不允许在其他地方创建测试：**

- 新功能的测试用例必须添加到 `test.py` 的测试列表中
- API 端点测试必须包含在 `test_rest_api()` 方法中
- WebSocket 功能测试必须在 `test_websocket()` 方法中实现
- 测试用例应包含详细的错误检查和结果验证

### 3. 开发环境规范
**禁止在本地直接打包和运行程序：**

- ❌ 不要使用 `mvnw clean package`
- ❌ 不要使用 `mvnw quarkus:dev`
- ❌ 不要在本地运行 JAR 文件
- ✅ 必须使用 `python test.py` 进行 Docker 化的测试

### 4. Docker 优先原则
项目采用 Docker 优先的开发方式：
- 所有功能验证必须在 Docker 容器中进行
- 使用多阶段构建确保环境一致性
- 构建过程包含完整的依赖管理和优化
- 生产环境与开发环境保持一致

## 技术栈
- **框架**: Quarkus 3.26.1
- **运行时**: GraalVM Community 21
- **语言支持**: JavaScript (GraalJS), Python (GraalPy)
- **构建工具**: Maven + Docker
- **测试工具**: Python 自动化测试脚本

## 快速开始

1. **运行完整测试**:
   ```bash
   python test.py
   ```

2. **查看测试结果**:
   - 测试脚本会自动构建 Docker 镜像
   - 启动容器并运行服务
   - 执行完整的功能测试
   - 默认保持容器运行以便手动验证

3. **手动测试访问**:
   - Web 界面: http://localhost:8080
   - REST API: http://localhost:8080/api/script/*
   - WebSocket: ws://localhost:8080/ws/script

## 注意事项
- 代码修改后必须重新运行 `python test.py` 验证
- 测试失败时会显示详细的错误信息和容器日志
- 容器默认保持运行状态，可手动停止或重新测试