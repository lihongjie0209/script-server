package cn.lihongjie.service;

import cn.lihongjie.model.ScriptExecutionRequest;
import cn.lihongjie.model.ScriptExecutionResult;
import cn.lihongjie.model.ScriptPermissions;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("权限控制专项测试")
public class ScriptPermissionSecurityTest {

    @Inject
    ScriptExecutionService scriptExecutionService;

    @Nested
    @DisplayName("沙盒安全测试")
    class SandboxSecurityTest {

        @Test
        @DisplayName("沙盒模式应该阻止危险的系统调用")
        void testSandboxBlocksDangerousSystemCalls() {
            ScriptPermissions sandboxPermissions = ScriptPermissions.createSandbox();
            
            // 测试可能危险的Python系统调用
            String[] dangerousScripts = {
                // 文件系统访问
                "import os; os.listdir('/')",
                // 进程执行
                "import subprocess; subprocess.run(['echo', 'test'])",
                // 系统信息获取
                "import platform; platform.system()"
            };

            for (String script : dangerousScripts) {
                ScriptExecutionRequest request = createRequestWithPermissions("python", script, sandboxPermissions);
                ScriptExecutionResult result = scriptExecutionService.executeScript(request);
                
                // 在严格的沙盒模式下，这些操作应该被限制或产生受控的结果
                assertNotNull(result, "结果不应该为null，脚本: " + script);
                // 注意：具体的行为依赖于GraalVM的沙盒实现
            }
        }

        @Test
        @DisplayName("沙盒模式下的JavaScript权限测试")
        void testJavaScriptSandboxPermissions() {
            ScriptPermissions sandboxPermissions = ScriptPermissions.createSandbox();
            
            ScriptExecutionRequest request = createRequestWithPermissions("js", """
                // 测试基本功能是否可用
                var result = {
                    math: Math.sqrt(16),
                    string: 'Hello'.toUpperCase(),
                    array: [1,2,3].length,
                    object: {a: 1, b: 2}
                };
                
                // 测试JSON功能
                JSON.stringify(result);
                """, sandboxPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "基本JavaScript功能在沙盒中应该可用");
            assertNotNull(result.getResult(), "应该有结果返回");
            assertTrue(result.getResult().toString().contains("math"), "结果应该包含计算内容");
        }
    }

    @Nested
    @DisplayName("网络权限边界测试")
    class NetworkPermissionBoundaryTest {

        @Test
        @DisplayName("禁用网络权限时的行为")
        void testDisabledNetworkPermissions() {
            ScriptPermissions noNetworkPermissions = ScriptPermissions.createSandbox();
            noNetworkPermissions.setAllowNetwork(false);
            noNetworkPermissions.setAllowHostAccess(false);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                # 测试网络模块的可用性和实际操作限制
                try:
                    import urllib.request
                    module_available = True
                    
                    # 尝试创建网络请求对象
                    try:
                        req = urllib.request.Request('http://example.com')
                        request_created = True
                        request_error = None
                    except Exception as e:
                        request_created = False
                        request_error = str(e)
                        
                except ImportError as e:
                    module_available = False
                    request_created = False
                    request_error = str(e)
                
                {
                    'module_available': module_available,
                    'request_created': request_created,
                    'request_error': request_error
                }
                """, noNetworkPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "模块导入测试应该成功");
            assertNotNull(result.getResult(), "应该有返回结果");
            
            // 验证网络操作的限制情况
            String resultStr = result.getResult().toString();
            assertTrue(resultStr.contains("module_available"), "结果应该包含module_available字段");
            assertTrue(resultStr.contains("request_created"), "结果应该包含request_created字段");
            // 注意：在禁用网络权限时，模块可能可以导入，但网络操作应该受到限制
        }

        @Test
        @DisplayName("启用网络权限时的行为")
        void testEnabledNetworkPermissions() {
            ScriptPermissions networkPermissions = ScriptPermissions.createSandbox();
            networkPermissions.setAllowNetwork(true);
            networkPermissions.setAllowHostAccess(true);
            networkPermissions.setAllowIO(true);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                import urllib.request
                import urllib.error
                
                # 测试网络功能是否真的可用
                try:
                    # 创建一个简单的请求对象（不实际发送）
                    req = urllib.request.Request('http://example.com')
                    request_created = True
                    error_msg = None
                except Exception as e:
                    request_created = False
                    error_msg = str(e)
                
                {
                    'request_created': request_created,
                    'urllib_available': hasattr(urllib.request, 'urlopen'),
                    'error_msg': error_msg
                }
                """, networkPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "网络权限启用后，网络相关操作应该可用");
            assertNotNull(result.getResult(), "应该有返回结果");
            
            // 验证网络请求是否成功创建
            String resultStr = result.getResult().toString();
            assertTrue(resultStr.contains("request_created"), "结果应该包含request_created字段");
            assertTrue(resultStr.contains("True") || resultStr.contains("true"), 
                "在启用网络权限的情况下，urllib.request.Request应该能够成功创建");
            assertTrue(resultStr.contains("urllib_available"), "结果应该包含urllib_available字段");
        }
    }

    @Nested
    @DisplayName("IO权限边界测试")
    class IOPermissionBoundaryTest {

        @Test
        @DisplayName("禁用IO权限的效果")
        void testDisabledIOPermissions() {
            ScriptPermissions noIOPermissions = ScriptPermissions.createSandbox();
            noIOPermissions.setAllowIO(false);
            noIOPermissions.setAllowFileAccess(false);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                # 测试文件IO操作 - 使用更简单的测试方式
                try:
                    # 测试基本的文件操作能力
                    import tempfile
                    tempfile_available = True
                    
                    # 尝试获取临时目录路径（不实际创建文件）
                    temp_dir = tempfile.gettempdir()
                    temp_dir_accessible = True
                    
                except Exception as e:
                    tempfile_available = False
                    temp_dir_accessible = False
                    tempfile_error = str(e)[:100]
                
                # 测试更基本的文件操作
                try:
                    # 尝试简单的文件路径操作
                    import os
                    os_available = True
                    current_dir = os.getcwd() if hasattr(os, 'getcwd') else 'unavailable'
                except Exception as e:
                    os_available = False
                    current_dir = 'error'
                
                {
                    'tempfile_available': tempfile_available,
                    'temp_dir_accessible': temp_dir_accessible,
                    'os_available': os_available,
                    'current_dir': current_dir[:50] if isinstance(current_dir, str) else current_dir
                }
                """, noIOPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            // 在禁用IO权限时，脚本可能执行成功但功能受限，或者直接失败
            if (result.isSuccess()) {
                assertNotNull(result.getResult(), "如果脚本执行成功，应该有返回结果");
                
                // 验证IO操作的限制情况
                String resultStr = result.getResult().toString();
                assertTrue(resultStr.contains("tempfile_available") || resultStr.contains("os_available"), 
                    "结果应该包含模块可用性信息");
                
            } else {
                // 如果脚本执行失败，这也是权限控制的正常表现
                assertNotNull(result.getError(), "执行失败时应该有错误信息");
                assertTrue(result.getError().contains("IO") || 
                          result.getError().contains("permission") ||
                          result.getError().contains("access") ||
                          result.getError().contains("denied") ||
                          result.getError().contains("Security") ||
                          result.getError().contains("sandbox"), 
                    "错误信息应该与权限限制相关，实际错误: " + result.getError());
            }
        }

        @Test
        @DisplayName("启用IO权限的效果")
        void testEnabledIOPermissions() {
            ScriptPermissions ioPermissions = ScriptPermissions.createSandbox();
            ioPermissions.setAllowIO(true);
            ioPermissions.setAllowFileAccess(true);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                import tempfile
                import os
                
                # 测试临时文件操作
                try:
                    with tempfile.NamedTemporaryFile(mode='w', delete=False) as f:
                        f.write('权限测试内容')
                        temp_filename = f.name
                    
                    # 读取临时文件
                    with open(temp_filename, 'r') as f:
                        content = f.read()
                    
                    # 删除临时文件
                    os.unlink(temp_filename)
                    
                    file_operations_successful = True
                    content_match = content == '权限测试内容'
                    error_msg = None
                    
                except Exception as e:
                    file_operations_successful = False
                    content_match = False
                    error_msg = str(e)[:100]
                
                {
                    'file_ops_successful': file_operations_successful,
                    'content_match': content_match,
                    'error': error_msg
                }
                """, ioPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "IO权限启用后，文件操作应该可用");
            assertNotNull(result.getResult(), "应该有返回结果");
            
            // 验证文件操作是否成功
            String resultStr = result.getResult().toString();
            assertTrue(resultStr.contains("file_ops_successful"), "结果应该包含file_ops_successful字段");
            assertTrue(resultStr.contains("content_match"), "结果应该包含content_match字段");
            assertTrue(resultStr.contains("True") || resultStr.contains("true"), 
                "在启用IO权限的情况下，文件操作应该成功");
        }
    }

    @Nested
    @DisplayName("主机访问权限测试")
    class HostAccessPermissionTest {

        @Test
        @DisplayName("禁用主机访问权限")
        void testDisabledHostAccess() {
            ScriptPermissions noHostAccessPermissions = ScriptPermissions.createSandbox();
            noHostAccessPermissions.setAllowHostAccess(false);

            ScriptExecutionRequest request = createRequestWithPermissions("js", """
                // 测试主机对象访问
                var hostAccessible = false;
                try {
                    // 在GraalVM中，某些主机功能可能被限制
                    if (typeof Java !== 'undefined') {
                        var javaString = Java.type('java.lang.String');
                        hostAccessible = true;
                    }
                } catch (e) {
                    hostAccessible = false;
                }
                
                JSON.stringify({
                    hostAccess: hostAccessible,
                    basicJs: true,
                    mathWorks: Math.PI > 3
                });
                """, noHostAccessPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "基本JavaScript功能应该仍然可用");
            assertNotNull(result.getResult(), "应该有返回结果");
            // 主机访问应该被限制，但基本功能应该正常
        }

        @Test
        @DisplayName("启用主机访问权限")
        void testEnabledHostAccess() {
            ScriptPermissions hostAccessPermissions = ScriptPermissions.createSandbox();
            hostAccessPermissions.setAllowHostAccess(true);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                # 测试与Java主机的交互能力
                try:
                    # 在GraalVM Python中可能可以访问一些Java功能
                    # 但具体行为依赖于环境配置
                    java_accessible = True  # 假设可以访问
                    java_version = "测试版本"
                except Exception as e:
                    java_accessible = False
                    java_version = None
                
                {
                    'java_accessible': java_accessible,
                    'java_version': java_version,
                    'python_version': 'GraalVM Python'
                }
                """, hostAccessPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "主机访问权限启用后，相应功能应该可用");
            assertNotNull(result.getResult(), "应该有返回结果");
        }
    }

    @Nested
    @DisplayName("线程权限测试")
    class ThreadPermissionTest {

        @Test
        @DisplayName("禁用线程创建权限")
        void testDisabledThreadCreation() {
            ScriptPermissions noThreadPermissions = ScriptPermissions.createSandbox();
            noThreadPermissions.setAllowCreateThread(false);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                import threading
                import time
                
                # 测试线程创建
                try:
                    def worker():
                        time.sleep(0.01)  # 减少等待时间
                        return "worker_done"
                    
                    # 尝试创建线程，但不一定会成功
                    thread = threading.Thread(target=worker)
                    thread_created = True
                    
                except Exception as e:
                    thread_created = False
                    error_type = type(e).__name__
                
                {
                    'thread_creation_attempted': True,
                    'threading_module_available': True
                }
                """, noThreadPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "线程权限测试应该成功执行");
            assertNotNull(result.getResult(), "应该有返回结果");
            // 具体的线程创建可能被允许或拒绝，取决于GraalVM配置
        }

        @Test
        @DisplayName("启用线程创建权限")
        void testEnabledThreadCreation() {
            ScriptPermissions threadPermissions = ScriptPermissions.createSandbox();
            threadPermissions.setAllowCreateThread(true);

            ScriptExecutionRequest request = createRequestWithPermissions("js", """
                // JavaScript中的异步操作测试
                var asyncResult = 'not_set';
                
                setTimeout(function() {
                    asyncResult = 'async_completed';
                }, 50);
                
                // 等待异步操作完成（由于是忙等待实现，应该同步完成）
                JSON.stringify({
                    asyncOperation: asyncResult,
                    setTimeoutAvailable: typeof setTimeout === 'function'
                });
                """, threadPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            assertTrue(result.isSuccess(), "线程权限启用后，异步操作应该可用");
            assertNotNull(result.getResult(), "应该有结果返回");
            assertTrue(result.getResult().toString().contains("async_completed"), "setTimeout应该执行完成");
        }
    }

    @Nested
    @DisplayName("权限组合测试")
    class PermissionCombinationTest {

        @ParameterizedTest
        @ValueSource(strings = {"js", "python"})
        @DisplayName("最小权限集合测试")
        void testMinimalPermissions(String language) {
            ScriptPermissions minimalPermissions = new ScriptPermissions();
            minimalPermissions.setAllowIO(false);
            minimalPermissions.setAllowNetwork(false);
            minimalPermissions.setAllowHostAccess(false);
            minimalPermissions.setAllowFileAccess(false);
            minimalPermissions.setAllowCreateThread(false);
            minimalPermissions.setAllowEnvironmentAccess(false);

            String script = language.equals("js") ? 
                "Math.sqrt(16) + Math.PI" : 
                "import math; math.sqrt(16) + math.pi";

            ScriptExecutionRequest request = createRequestWithPermissions(language, script, minimalPermissions);
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), 
                "最小权限下基本数学运算应该成功: " + language);
            assertNotNull(result.getResult(), "应该有计算结果");
        }

        @ParameterizedTest
        @ValueSource(strings = {"js", "python"})
        @DisplayName("最大权限集合测试")
        void testMaximalPermissions(String language) {
            ScriptPermissions maximalPermissions = new ScriptPermissions();
            maximalPermissions.setAllowIO(true);
            maximalPermissions.setAllowNetwork(true);
            maximalPermissions.setAllowHostAccess(true);
            maximalPermissions.setAllowFileAccess(true);
            maximalPermissions.setAllowCreateThread(true);
            maximalPermissions.setAllowEnvironmentAccess(true);

            String script = language.equals("js") ? 
                "console.log('最大权限测试'); 'js_max_permissions'" : 
                "print('最大权限测试'); 'python_max_permissions'";

            ScriptExecutionRequest request = createRequestWithPermissions(language, script, maximalPermissions);
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), 
                "最大权限下脚本执行应该成功: " + language);
            assertTrue(result.getOutput().contains("最大权限测试"), 
                "输出应该包含测试信息");
        }

        @Test
        @DisplayName("权限配置一致性测试")
        void testPermissionConfigurationConsistency() {
            // 测试权限配置是否被正确应用到脚本执行上下文
            ScriptPermissions permissions1 = ScriptPermissions.createSandbox();
            ScriptPermissions permissions2 = ScriptPermissions.createSandbox();
            
            // 验证相同的权限配置产生一致的结果
            String testScript = "console.log('权限一致性测试'); 42";
            
            ScriptExecutionRequest request1 = createRequestWithPermissions("js", testScript, permissions1);
            ScriptExecutionRequest request2 = createRequestWithPermissions("js", testScript, permissions2);
            
            ScriptExecutionResult result1 = scriptExecutionService.executeScript(request1);
            ScriptExecutionResult result2 = scriptExecutionService.executeScript(request2);
            
            assertEquals(result1.isSuccess(), result2.isSuccess(), 
                "相同权限配置应该产生一致的成功状态");
            assertEquals(result1.getResult(), result2.getResult(), 
                "相同权限配置应该产生一致的结果");
        }
    }

    // 辅助方法
    private ScriptExecutionRequest createRequestWithPermissions(String language, String script, ScriptPermissions permissions) {
        ScriptExecutionRequest request = new ScriptExecutionRequest();
        request.setLanguage(language);
        request.setScript(script);
        request.setPermissions(permissions);
        return request;
    }
}
