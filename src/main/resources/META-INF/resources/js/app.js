let ws = null;

const examples = {
    'js-simple': {
        language: 'js',
        script: 'console.log("Hello from JavaScript!"); \nlet result = 1 + 2 + 3; \nconsole.log("计算结果:", result); \nresult;',
        entryFunction: '',
        args: ''
    },
    'js-function': {
        language: 'js',
        script: 'function calculate(a, b, operation) {\n    console.log(`执行 ${a} ${operation} ${b}`);\n    switch(operation) {\n        case "+": return a + b;\n        case "-": return a - b;\n        case "*": return a * b;\n        case "/": return a / b;\n        default: return "不支持的操作";\n    }\n}',
        entryFunction: 'calculate',
        args: '[10, 5, "+"]'
    },
    'js-async': {
        language: 'js',
        script: 'function demo() {\n    console.log("开始执行...");\n    for (let i = 1; i <= 5; i++) {\n        console.log(`步骤 ${i}: 正在处理...`);\n        // 使用简单的计算来模拟延时\n        let start = Date.now();\n        while (Date.now() - start < 500) {\n            // 忙等待500毫秒\n        }\n        console.log(`步骤 ${i}: 完成`);\n    }\n    console.log("所有步骤完成!");\n    return "延时执行完成";\n}',
        entryFunction: 'demo',
        args: ''
    },
    'js-recursive': {
        language: 'js',
        script: 'function processData(items, index) {\n    if (index >= items.length) {\n        console.log("处理完成!");\n        return "所有数据处理完成";\n    }\n    \n    console.log(`处理第 ${index + 1} 项: ${items[index]}`);\n    \n    // 模拟处理时间 - 执行一些计算\n    let sum = 0;\n    for (let i = 0; i < 1000000; i++) {\n        sum += Math.sqrt(i);\n    }\n    \n    console.log(`第 ${index + 1} 项处理完成`);\n    \n    // 递归处理下一项\n    return processData(items, index + 1);\n}\n\nfunction main() {\n    console.log("开始批量处理...");\n    const data = ["任务A", "任务B", "任务C", "任务D"];\n    return processData(data, 0);\n}',
        entryFunction: 'main',
        args: ''
    },
    'python-simple': {
        language: 'python',
        script: 'print("Hello from Python!")\nresult = sum([1, 2, 3, 4, 5])\nprint(f"求和结果: {result}")\nresult',
        entryFunction: '',
        args: ''
    },
    'python-sleep': {
        language: 'python',
        script: 'import time\n\ndef slow_task():\n    print("开始执行耗时任务...")\n    for i in range(1, 6):\n        print(f"步骤 {i}: 正在处理...")\n        time.sleep(1)  # 休眠1秒\n        print(f"步骤 {i}: 完成")\n    print("所有步骤完成!")\n    return "Python慢任务执行完成"',
        entryFunction: 'slow_task',
        args: ''
    },
    'python-http': {
        language: 'python',
        script: 'import urllib.request\nimport json\n\ndef test_http_simple():\n    print("=== Python HTTP 简单测试 ===")\n    \n    # 使用原生模块信息测试\n    print("\\n1. 测试 urllib.request 模块导入...")\n    try:\n        print("urllib.request 模块可用:", hasattr(urllib.request, "urlopen"))\n        print("Request 类可用:", hasattr(urllib.request, "Request"))\n    except Exception as e:\n        print("模块检查失败:", str(e))\n    \n    # 简单的 HTTP 测试（避免域名解析问题）\n    print("\\n2. 测试基本 HTTP 功能...")\n    try:\n        # 使用简单的 HTTP URL 避免 HTTPS 证书和域名解析问题\n        url = "http://httpbin.org/get"\n        print("正在请求:", url)\n        \n        response = urllib.request.urlopen(url, timeout=10)\n        print("HTTP 状态码:", response.getcode())\n        print("Content-Type:", response.getheader("content-type"))\n        \n        # 读取部分响应数据\n        data = response.read(500)  # 只读取前500字节\n        print("响应数据长度:", len(data), "字节")\n        print("响应开始:", str(data[:100], "utf-8", errors="ignore"))\n        \n        print("✅ HTTP 请求成功完成")\n        return "HTTP 请求测试通过"\n        \n    except Exception as e:\n        error_msg = str(e)\n        print("❌ HTTP 请求失败:", error_msg)\n        \n        # 分析错误类型\n        if "idna" in error_msg.lower():\n            print("💡 这是 IDNA 编码问题，GraalPy 可能不支持国际化域名")\n        elif "ssl" in error_msg.lower():\n            print("💡 这是 SSL 证书问题，可能需要额外的证书配置")\n        elif "timeout" in error_msg.lower():\n            print("💡 这是网络超时问题")\n        else:\n            print("💡 其他网络或配置问题")\n        \n        return "HTTP请求失败: " + error_msg',
        entryFunction: 'test_http_simple',
        args: '',
        permissions: {
            allowNetwork: true,
            allowIO: true
        }
    }
};

function loadExample(exampleKey) {
    const example = examples[exampleKey];
    if (example) {
        document.getElementById('language').value = example.language;
        document.getElementById('script').value = example.script;
        document.getElementById('entryFunction').value = example.entryFunction;
        document.getElementById('args').value = example.args;
        
        // 如果示例包含权限设置，应用这些权限
        if (example.permissions) {
            // 先重置所有权限
            document.getElementById('allowIO').checked = false;
            document.getElementById('allowNetwork').checked = false;
            document.getElementById('allowHostAccess').checked = false;
            document.getElementById('allowFileAccess').checked = false;
            document.getElementById('allowCreateThread').checked = false;
            document.getElementById('allowEnvironmentAccess').checked = false;
            
            // 应用示例的权限设置
            if (example.permissions.allowIO) {
                document.getElementById('allowIO').checked = true;
            }
            if (example.permissions.allowNetwork) {
                document.getElementById('allowNetwork').checked = true;
            }
            if (example.permissions.allowHostAccess) {
                document.getElementById('allowHostAccess').checked = true;
            }
            if (example.permissions.allowFileAccess) {
                document.getElementById('allowFileAccess').checked = true;
            }
            if (example.permissions.allowCreateThread) {
                document.getElementById('allowCreateThread').checked = true;
            }
            if (example.permissions.allowEnvironmentAccess) {
                document.getElementById('allowEnvironmentAccess').checked = true;
            }
        }
    }
}

function connectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        return;
    }
    
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/script`;
    
    ws = new WebSocket(wsUrl);
    
    ws.onopen = function() {
        updateStatus('连接成功', 'connected');
    };
    
    ws.onmessage = function(event) {
        const message = JSON.parse(event.data);
        appendOutput(`[${message.type.toUpperCase()}] ${message.message}`);
        if (message.data) {
            appendOutput(JSON.stringify(message.data, null, 2));
        }
    };
    
    ws.onclose = function() {
        updateStatus('连接已断开', 'disconnected');
    };
    
    ws.onerror = function(error) {
        updateStatus('连接错误', 'disconnected');
        appendOutput('WebSocket错误: ' + error);
    };
}

function disconnectWebSocket() {
    if (ws) {
        ws.close();
        ws = null;
    }
}

function updateStatus(message, className) {
    const statusEl = document.getElementById('wsStatus');
    statusEl.textContent = 'WebSocket: ' + message;
    statusEl.className = 'status ' + className;
}

function executeScript() {
    const request = buildRequest();
    
    if (ws && ws.readyState === WebSocket.OPEN) {
        // 使用WebSocket执行
        clearOutput();
        appendOutput('通过WebSocket执行脚本...');
        ws.send(JSON.stringify(request));
    } else {
        // 使用HTTP API执行
        executeViaHTTP(request);
    }
}

function executeViaHTTP(request) {
    clearOutput();
    appendOutput('通过HTTP API执行脚本...');
    
    fetch('/api/script/execute', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
    })
    .then(response => response.json())
    .then(result => {
        appendOutput('执行完成:');
        appendOutput(JSON.stringify(result, null, 2));
    })
    .catch(error => {
        appendOutput('执行错误: ' + error);
    });
}

function buildRequest() {
    const argsText = document.getElementById('args').value.trim();
    let args = null;
    if (argsText) {
        try {
            args = JSON.parse(argsText);
        } catch (e) {
            alert('参数格式错误，请使用有效的JSON格式');
            return null;
        }
    }
    
    return {
        script: document.getElementById('script').value,
        language: document.getElementById('language').value,
        entryFunction: document.getElementById('entryFunction').value,
        args: args,
        permissions: {
            allowIO: document.getElementById('allowIO').checked,
            allowNetwork: document.getElementById('allowNetwork').checked,
            allowHostAccess: document.getElementById('allowHostAccess').checked,
            allowFileAccess: document.getElementById('allowFileAccess').checked,
            allowCreateThread: document.getElementById('allowCreateThread').checked,
            allowEnvironmentAccess: document.getElementById('allowEnvironmentAccess').checked,
            maxExecutionTime: parseInt(document.getElementById('maxExecutionTime').value) || 30000,
            maxMemoryUsage: 128 * 1024 * 1024
        }
    };
}

function appendOutput(text) {
    const outputEl = document.getElementById('output');
    outputEl.textContent += new Date().toLocaleTimeString() + ' - ' + text + '\n';
    outputEl.scrollTop = outputEl.scrollHeight;
}

function clearOutput() {
    document.getElementById('output').textContent = '';
}

// 从服务器获取支持的语言列表
async function loadSupportedLanguages() {
    try {
        const response = await fetch('/api/script/languages');
        const languages = await response.json();
        
        const languageSelect = document.getElementById('language');
        // 清空现有选项
        languageSelect.innerHTML = '';
        
        // 添加获取到的语言选项
        languages.forEach(lang => {
            const option = document.createElement('option');
            option.value = lang;
            option.textContent = getLanguageDisplayName(lang);
            languageSelect.appendChild(option);
        });
        
        // 设置默认选择第一个语言
        if (languages.length > 0) {
            languageSelect.value = languages[0];
        }
    } catch (error) {
        console.error('获取支持的语言失败:', error);
        // 如果获取失败，使用默认语言
        appendOutput('警告: 无法获取支持的语言列表，使用默认配置');
    }
}

// 获取语言的显示名称
function getLanguageDisplayName(language) {
    const displayNames = {
        'js': 'JavaScript',
        'javascript': 'JavaScript',
        'python': 'Python',
        'py': 'Python',
        'ruby': 'Ruby',
        'rb': 'Ruby',
        'r': 'R'
    };
    return displayNames[language] || language.toUpperCase();
}

// 页面加载时自动加载示例和语言列表
window.onload = async function() {
    await loadSupportedLanguages();
    loadExample('js-simple');
};
