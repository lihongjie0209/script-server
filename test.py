#!/usr/bin/env python3
"""
GraalVM 脚本执行服务 - 构建和测试脚本
用于自动化构建Docker镜像、启动服务并进行功能测试
"""

import os
import sys
import time
import json
import subprocess
import requests
import websocket
import threading
from pathlib import Path

class ScriptServerTester:
    def __init__(self):
        self.base_url = "http://localhost:8080"
        self.ws_url = "ws://localhost:8080/ws/script"
        self.container_name = "script-server-test"
        self.image_name = "script-server:latest"
        self.ws_messages = []
        self.ws_connected = False
        
    def log(self, message, level="INFO"):
        """日志输出"""
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] [{level}] {message}")
        
    def run_command(self, command, cwd=None, capture_output=True):
        """执行系统命令"""
        self.log(f"执行命令: {command}")
        try:
            result = subprocess.run(
                command,
                shell=True,
                cwd=cwd,
                capture_output=capture_output,
                text=True,
                timeout=300  # 5分钟超时
            )
            if result.returncode != 0:
                self.log(f"命令执行失败: {result.stderr}", "ERROR")
                return False, result.stderr
            return True, result.stdout
        except subprocess.TimeoutExpired:
            self.log("命令执行超时", "ERROR")
            return False, "Timeout"
        except Exception as e:
            self.log(f"命令执行异常: {str(e)}", "ERROR")
            return False, str(e)
    
    def build_docker_image(self):
        """构建Docker镜像"""
        self.log("🐳 开始构建Docker镜像...")
        
        # 检查Dockerfile是否存在
        dockerfile_path = Path("Dockerfile")
        if not dockerfile_path.exists():
            self.log("Dockerfile不存在", "ERROR")
            return False
            
        # 设置环境变量启用BuildKit
        env = os.environ.copy()
        env['DOCKER_BUILDKIT'] = '1'
        
        # 构建镜像（使用BuildKit和缓存）
        command = f"docker build --progress=plain --build-arg BUILDKIT_INLINE_CACHE=1 -t {self.image_name} ."
        
        self.log(f"执行命令: {command}")
        try:
            result = subprocess.run(
                command,
                shell=True,
                env=env,
                capture_output=False,
                text=True,
                timeout=600  # 10分钟超时
            )
            if result.returncode != 0:
                self.log("Docker镜像构建失败", "ERROR")
                return False
        except subprocess.TimeoutExpired:
            self.log("Docker构建超时", "ERROR")
            return False
        except Exception as e:
            self.log(f"Docker构建异常: {str(e)}", "ERROR")
            return False
            
        self.log("✅ Docker镜像构建成功")
        return True
    
    def start_container(self):
        """启动Docker容器"""
        self.log("🚀 启动Docker容器...")
        
        # 停止并删除现有容器
        self.run_command(f"docker stop {self.container_name}")
        self.run_command(f"docker rm {self.container_name}")
        
        # 启动新容器
        success, output = self.run_command(
            f"docker run -d --name {self.container_name} -p 8080:8080 {self.image_name}"
        )
        
        if not success:
            self.log("容器启动失败", "ERROR")
            return False
            
        self.log("✅ 容器启动成功")
        return True
    
    def wait_for_service(self, max_wait=60):
        """等待服务启动"""
        self.log("⏳ 等待服务启动...")
        
        start_time = time.time()
        while time.time() - start_time < max_wait:
            try:
                response = requests.get(f"{self.base_url}/api/script/health", timeout=5)
                if response.status_code == 200:
                    self.log("✅ 服务已启动")
                    return True
            except requests.exceptions.RequestException:
                pass
            
            time.sleep(2)
        
        self.log("❌ 服务启动超时", "ERROR")
        return False
    
    def test_rest_api(self):
        """测试REST API"""
        self.log("🧪 开始测试REST API...")
        
        tests = [
            {
                "name": "健康检查",
                "method": "GET",
                "url": "/api/script/health",
                "expected_status": 200
            },
            {
                "name": "获取支持的语言",
                "method": "GET", 
                "url": "/api/script/languages",
                "expected_status": 200
            },
            {
                "name": "获取沙盒权限配置",
                "method": "GET",
                "url": "/api/script/permissions/sandbox", 
                "expected_status": 200
            },
            {
                "name": "简单JavaScript执行",
                "method": "POST",
                "url": "/api/script/execute",
                "data": {
                    "script": "1 + 2 + 3",
                    "language": "js",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "JavaScript函数调用",
                "method": "POST", 
                "url": "/api/script/execute",
                "data": {
                    "script": "function add(a, b) { return a + b; }",
                    "language": "js",
                    "entryFunction": "add",
                    "args": [10, 20],
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "JavaScript控制台输出",
                "method": "POST",
                "url": "/api/script/execute", 
                "data": {
                    "script": "console.log('Hello World'); 'result'",
                    "language": "js",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "JavaScript setTimeout测试",
                "method": "POST",
                "url": "/api/script/execute",
                "data": {
                    "script": "console.log('Before setTimeout'); setTimeout(() => { console.log('Inside setTimeout'); }, 100); console.log('After setTimeout'); 'timeout_test_started'",
                    "language": "js",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": True,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "GraalVM setTimeout 详细测试",
                "method": "GET",
                "url": "/api/test/setTimeout",
                "expected_status": 200
            },
            {
                "name": "简单Python执行",
                "method": "POST",
                "url": "/api/script/execute",
                "data": {
                    "script": "1 + 2 + 3",
                    "language": "python",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "Python函数调用",
                "method": "POST", 
                "url": "/api/script/execute",
                "data": {
                    "script": "def add(a, b):\n    return a + b",
                    "language": "python",
                    "entryFunction": "add",
                    "args": [15, 25],
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "Python print输出",
                "method": "POST",
                "url": "/api/script/execute", 
                "data": {
                    "script": "print('Hello from Python')\nresult = 'python_success'\nresult",
                    "language": "python",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "Python列表操作",
                "method": "POST",
                "url": "/api/script/execute", 
                "data": {
                    "script": "numbers = [1, 2, 3, 4, 5]\nsum(numbers)",
                    "language": "python",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "Python sleep间隔输出",
                "method": "POST",
                "url": "/api/script/execute", 
                "data": {
                    "script": "import time\ndef slow_task():\n    print('任务开始')\n    for i in range(1, 4):\n        print(f'步骤 {i}: 处理中...')\n        time.sleep(0.5)\n        print(f'步骤 {i}: 完成')\n    print('任务结束')\n    return 'slow_task_completed'",
                    "language": "python",
                    "entryFunction": "slow_task",
                    "permissions": {
                        "allowIO": False,
                        "allowNetwork": False,
                        "allowHostAccess": False,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            },
            {
                "name": "Python HTTP请求测试",
                "method": "POST",
                "url": "/api/script/execute",
                "data": {
                    "script": "import urllib.request\nimport json\n\ndef test_http():\n    try:\n        # 测试简单的 GET 请求\n        response = urllib.request.urlopen('https://httpbin.org/get')\n        data = response.read().decode('utf-8')\n        result = json.loads(data)\n        return f\"HTTP请求成功，URL: {result.get('url', 'N/A')}\"\n    except Exception as e:\n        return f\"HTTP请求失败: {str(e)}\"\n\ntest_http()",
                    "language": "python",
                    "permissions": {
                        "allowIO": True,
                        "allowNetwork": True,
                        "allowHostAccess": True,
                        "allowFileAccess": False,
                        "allowCreateThread": False,
                        "allowEnvironmentAccess": False,
                        "maxExecutionTime": 30000,
                        "maxMemoryUsage": 134217728
                    }
                },
                "expected_status": 200,
                "check_result": True
            }
        ]
        
        passed = 0
        failed = 0
        
        for test in tests:
            try:
                self.log(f"测试: {test['name']}")
                
                if test["method"] == "GET":
                    response = requests.get(f"{self.base_url}{test['url']}", timeout=10)
                else:
                    response = requests.post(
                        f"{self.base_url}{test['url']}", 
                        json=test.get("data"),
                        timeout=10
                    )
                
                # 检查状态码
                if response.status_code != test["expected_status"]:
                    self.log(f"  ❌ 状态码错误: 期望{test['expected_status']}, 实际{response.status_code}", "ERROR")
                    failed += 1
                    continue
                
                # 检查结果
                if test.get("check_result"):
                    result = response.json()
                    if not result.get("success"):
                        self.log(f"  ❌ 脚本执行失败: {result.get('error')}", "ERROR")
                        failed += 1
                        continue
                    
                    self.log(f"  ✅ 执行结果: {result.get('result')}")
                else:
                    self.log(f"  ✅ 响应: {response.text[:100]}...")
                
                passed += 1
                
            except Exception as e:
                self.log(f"  ❌ 测试异常: {str(e)}", "ERROR")
                failed += 1
        
        self.log(f"📊 REST API测试完成: 通过{passed}, 失败{failed}")
        return failed == 0
    
    def on_ws_message(self, ws, message):
        """WebSocket消息处理"""
        self.ws_messages.append(json.loads(message))
        self.log(f"WebSocket消息: {message}")
    
    def on_ws_error(self, ws, error):
        """WebSocket错误处理"""
        self.log(f"WebSocket错误: {error}", "ERROR")
    
    def on_ws_close(self, ws, close_status_code, close_msg):
        """WebSocket关闭处理"""
        self.ws_connected = False
        self.log("WebSocket连接已关闭")
    
    def on_ws_open(self, ws):
        """WebSocket连接打开"""
        self.ws_connected = True
        self.log("WebSocket连接已建立")
    
    def test_websocket(self):
        """测试WebSocket功能"""
        self.log("🧪 开始测试WebSocket...")
        
        try:
            # 创建WebSocket连接
            ws = websocket.WebSocketApp(
                self.ws_url,
                on_message=self.on_ws_message,
                on_error=self.on_ws_error,
                on_close=self.on_ws_close,
                on_open=self.on_ws_open
            )
            
            # 在新线程中运行WebSocket
            def run_ws():
                ws.run_forever()
            
            ws_thread = threading.Thread(target=run_ws)
            ws_thread.daemon = True
            ws_thread.start()
            
            # 等待连接建立
            timeout = 10
            while not self.ws_connected and timeout > 0:
                time.sleep(0.5)
                timeout -= 0.5
            
            if not self.ws_connected:
                self.log("WebSocket连接失败", "ERROR")
                return False
            
            # 发送JavaScript测试消息
            test_script_js = {
                "script": "console.log('WebSocket JS Test'); 'js_success'",
                "language": "js",
                "permissions": {
                    "allowIO": False,
                    "allowNetwork": False,
                    "allowHostAccess": False,
                    "allowFileAccess": False,
                    "allowCreateThread": False,
                    "allowEnvironmentAccess": False,
                    "maxExecutionTime": 30000,
                    "maxMemoryUsage": 134217728
                }
            }
            
            ws.send(json.dumps(test_script_js))
            
            # 等待响应
            time.sleep(2)
            
            # 发送Python测试消息
            test_script_py = {
                "script": "print('WebSocket Python Test')\n'python_success'",
                "language": "python",
                "permissions": {
                    "allowIO": False,
                    "allowNetwork": False,
                    "allowHostAccess": False,
                    "allowFileAccess": False,
                    "allowCreateThread": False,
                    "allowEnvironmentAccess": False,
                    "maxExecutionTime": 30000,
                    "maxMemoryUsage": 134217728
                }
            }
            
            ws.send(json.dumps(test_script_py))
            
            # 等待响应
            time.sleep(2)
            
            # 关闭连接
            ws.close()
            
            # 检查消息
            if len(self.ws_messages) > 0:
                self.log("✅ WebSocket测试成功")
                return True
            else:
                self.log("❌ 未收到WebSocket消息", "ERROR")
                return False
                
        except Exception as e:
            self.log(f"WebSocket测试异常: {str(e)}", "ERROR")
            return False
    
    def test_web_interface(self):
        """测试Web界面"""
        self.log("🧪 测试Web界面...")
        
        try:
            response = requests.get(f"{self.base_url}/", timeout=10)
            if response.status_code == 200 and "GraalVM" in response.text:
                self.log("✅ Web界面访问成功")
                return True
            else:
                self.log(f"❌ Web界面访问失败: {response.status_code}", "ERROR")
                return False
        except Exception as e:
            self.log(f"Web界面测试异常: {str(e)}", "ERROR")
            return False
    
    def show_logs(self):
        """显示容器日志"""
        self.log("📋 获取容器日志...")
        success, output = self.run_command(f"docker logs {self.container_name}")
        if success:
            print("=== 容器日志 ===")
            print(output)
            print("=== 日志结束 ===")
    
    def cleanup(self):
        """清理资源"""
        self.log("🧹 清理资源...")
        self.run_command(f"docker stop {self.container_name}")
        self.run_command(f"docker rm {self.container_name}")
    
    def run_all_tests(self):
        """运行所有测试"""
        self.log("🚀 开始完整测试流程...")
        
        # 构建镜像
        if not self.build_docker_image():
            return False
        
        # 启动容器
        if not self.start_container():
            return False
        
        try:
            # 等待服务启动
            if not self.wait_for_service():
                self.show_logs()
                return False
            
            # 测试Web界面
            web_ok = self.test_web_interface()
            
            # 测试REST API
            api_ok = self.test_rest_api()
            
            # 测试WebSocket
            ws_ok = self.test_websocket()
            
            # 显示测试结果
            self.log("📊 测试结果汇总:")
            self.log(f"  Web界面: {'✅ 通过' if web_ok else '❌ 失败'}")
            self.log(f"  REST API: {'✅ 通过' if api_ok else '❌ 失败'}")
            self.log(f"  WebSocket: {'✅ 通过' if ws_ok else '❌ 失败'}")
            
            if web_ok and api_ok and ws_ok:
                self.log("🎉 所有测试通过！服务正常运行")
                self.log(f"🌐 访问地址: {self.base_url}")
                return True
            else:
                self.log("❌ 部分测试失败", "ERROR")
                self.show_logs()
                return False
                
        finally:
            # 默认保持容器运行以便手动测试
            keep_running = 'y'
            if keep_running != 'y':
                self.cleanup()
            else:
                self.log(f"🌐 容器继续运行，访问地址: {self.base_url}")
                self.log(f"📋 查看日志: docker logs {self.container_name}")
                self.log(f"🛑 停止容器: docker stop {self.container_name}")
                self.log(f"🗑️  删除容器: docker rm {self.container_name}")

def main():
    """主函数"""
    print("=" * 60)
    print("GraalVM 脚本执行服务 - 自动化测试")
    print("=" * 60)
    
    tester = ScriptServerTester()
    
    try:
        success = tester.run_all_tests()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n测试被用户中断")
        tester.cleanup()
        sys.exit(1)
    except Exception as e:
        print(f"测试过程中发生异常: {str(e)}")
        tester.cleanup()
        sys.exit(1)

if __name__ == "__main__":
    main()
