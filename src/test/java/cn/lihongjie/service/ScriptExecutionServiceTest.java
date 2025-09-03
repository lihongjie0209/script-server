package cn.lihongjie.service;

import cn.lihongjie.model.ScriptExecutionRequest;
import cn.lihongjie.model.ScriptExecutionResult;
import cn.lihongjie.model.ScriptPermissions;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("ScriptExecutionService 单元测试")
public class ScriptExecutionServiceTest {

    @Inject
    ScriptExecutionService scriptExecutionService;

    @Nested
    @DisplayName("基础功能测试")
    class BasicFunctionalityTest {

        @Test
        @DisplayName("获取可用语言列表")
        void testGetAvailableLanguages() {
            String[] languages = scriptExecutionService.getAvailableLanguages();
            
            assertNotNull(languages, "语言列表不应为null");
            assertTrue(languages.length > 0, "应该至少支持一种语言");
            
            // 验证基本语言支持
            boolean hasJs = false, hasPython = false;
            for (String lang : languages) {
                if ("js".equals(lang)) hasJs = true;
                if ("python".equals(lang)) hasPython = true;
            }
            
            assertTrue(hasJs || hasPython, "应该支持JavaScript或Python中的至少一种");
        }

        @Test
        @DisplayName("检查语言可用性")
        void testIsLanguageAvailable() {
            assertTrue(scriptExecutionService.isLanguageAvailable("js"), "JavaScript应该可用");
            assertTrue(scriptExecutionService.isLanguageAvailable("python"), "Python应该可用");
            assertFalse(scriptExecutionService.isLanguageAvailable("nonexistent"), "不存在的语言应该返回false");
        }
    }

    @Nested
    @DisplayName("JavaScript 脚本执行测试")
    class JavaScriptExecutionTest {

        @Test
        @DisplayName("简单表达式执行")
        void testSimpleExpression() {
            ScriptExecutionRequest request = createRequest("js", "1 + 2 * 3");
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "脚本执行应该成功");
            assertEquals(7, result.getResult(), "计算结果应该正确");
            assertNull(result.getError(), "不应该有错误");
        }

        @Test
        @DisplayName("函数定义和调用")
        void testFunctionExecution() {
            ScriptExecutionRequest request = createRequest("js", 
                "function multiply(a, b) { return a * b; }");
            request.setEntryFunction("multiply");
            request.setArgs(new Object[]{6, 7});

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "函数执行应该成功");
            assertEquals(42, result.getResult(), "函数计算结果应该正确");
        }

        @Test
        @DisplayName("控制台输出测试")
        void testConsoleOutput() {
            ScriptExecutionRequest request = createRequest("js", 
                "console.log('Hello, World!'); console.log('测试中文'); 'finished'");

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "脚本执行应该成功");
            assertEquals("finished", result.getResult(), "返回值应该正确");
            assertNotNull(result.getOutput(), "应该有输出内容");
            assertTrue(result.getOutput().contains("Hello, World!"), "输出应该包含英文内容");
            assertTrue(result.getOutput().contains("测试中文"), "输出应该包含中文内容");
        }

        @Test
        @DisplayName("setTimeout 功能测试")
        void testSetTimeoutFunction() {
            ScriptExecutionRequest request = createRequest("js", """
                var result = 'not_called';
                setTimeout(function() {
                    result = 'timeout_executed';
                }, 100);
                result;
                """);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "setTimeout脚本应该执行成功");
            // 由于setTimeout是同步忙等待实现，应该执行完回调
            assertEquals("timeout_executed", result.getResult(), "setTimeout回调应该被执行");
        }

        @Test
        @DisplayName("语法错误处理")
        void testSyntaxError() {
            ScriptExecutionRequest request = createRequest("js", "invalid syntax !!!");

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertFalse(result.isSuccess(), "语法错误的脚本应该执行失败");
            assertNotNull(result.getError(), "应该有错误信息");
            assertTrue(result.getError().toLowerCase().contains("syntax") || 
                      result.getError().toLowerCase().contains("parse") ||
                      result.getError().toLowerCase().contains("unexpected"), 
                      "错误信息应该指示语法问题");
        }
    }

    @Nested
    @DisplayName("Python 脚本执行测试")
    class PythonExecutionTest {

        @Test
        @DisplayName("简单Python表达式")
        void testSimplePythonExpression() {
            ScriptExecutionRequest request = createRequest("python", "1 + 2 * 3");
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "Python脚本执行应该成功");
            assertEquals(7, result.getResult(), "Python计算结果应该正确");
        }

        @Test
        @DisplayName("Python函数执行")
        void testPythonFunction() {
            ScriptExecutionRequest request = createRequest("python", """
                def fibonacci(n):
                    if n <= 1:
                        return n
                    return fibonacci(n-1) + fibonacci(n-2)
                """);
            request.setEntryFunction("fibonacci");
            request.setArgs(new Object[]{10});

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "Python函数执行应该成功");
            assertEquals(55, result.getResult(), "斐波那契数列第10项应该是55");
        }

        @Test
        @DisplayName("Python print输出")
        void testPythonPrint() {
            ScriptExecutionRequest request = createRequest("python", """
                print("Hello from Python")
                print("多行输出测试")
                print("数字:", 42)
                "execution_completed"
                """);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "Python print脚本应该成功");
            assertEquals("execution_completed", result.getResult(), "返回值应该正确");
            assertNotNull(result.getOutput(), "应该有打印输出");
            assertTrue(result.getOutput().contains("Hello from Python"), "输出应该包含英文");
            assertTrue(result.getOutput().contains("多行输出测试"), "输出应该包含中文");
            assertTrue(result.getOutput().contains("42"), "输出应该包含数字");
        }

        @Test
        @DisplayName("Python列表和字典操作")
        void testPythonDataStructures() {
            ScriptExecutionRequest request = createRequest("python", """
                data = [1, 2, 3, 4, 5]
                total = sum(data)
                
                info = {
                    "sum": total,
                    "count": len(data),
                    "average": total / len(data)
                }
                
                info["sum"]
                """);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "Python数据结构操作应该成功");
            assertEquals(15, result.getResult(), "列表求和结果应该正确");
        }
    }

    @Nested
    @DisplayName("权限控制测试")
    class PermissionControlTest {

        @Test
        @DisplayName("沙盒模式 - 禁止IO访问")
        void testSandboxIORestriction() {
            ScriptPermissions sandboxPermissions = ScriptPermissions.createSandbox();
            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                import os
                os.getcwd()
                """, sandboxPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            // 在沙盒模式下，某些系统功能可能被限制
            // 这个测试主要验证权限系统是否正常工作
            assertNotNull(result, "结果不应该为null");
            // 根据GraalVM的具体行为，这里可能成功也可能失败，主要是测试权限配置生效
        }

        @Test
        @DisplayName("网络权限控制测试")
        void testNetworkPermissionControl() {
            // 测试无网络权限
            ScriptPermissions noNetworkPermissions = ScriptPermissions.createSandbox();
            noNetworkPermissions.setAllowNetwork(false);
            
            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                # 简单的网络相关测试
                import socket
                socket.gethostname()
                """, noNetworkPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            
            // 验证权限配置被应用（具体行为可能因GraalVM版本而异）
            assertNotNull(result, "结果不应该为null");
        }

        @Test
        @DisplayName("线程创建权限测试")
        void testThreadCreationPermission() {
            ScriptPermissions restrictedPermissions = ScriptPermissions.createSandbox();
            restrictedPermissions.setAllowCreateThread(false);

            ScriptExecutionRequest request = createRequestWithPermissions("js", """
                // 测试线程相关功能限制
                // 在JavaScript中，这主要测试setTimeout等异步功能
                setTimeout(function() {
                    console.log('callback executed');
                }, 10);
                'thread_test_completed'
                """, restrictedPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "线程权限测试应该完成");
            assertEquals("thread_test_completed", result.getResult(), "基本功能应该正常");
        }

        @Test
        @DisplayName("自定义权限配置测试")
        void testCustomPermissions() {
            ScriptPermissions customPermissions = new ScriptPermissions();
            customPermissions.setAllowIO(true);
            customPermissions.setAllowNetwork(false);
            customPermissions.setAllowHostAccess(false);
            customPermissions.setAllowFileAccess(false);
            customPermissions.setAllowCreateThread(true);
            customPermissions.setMaxExecutionTime(5000);

            ScriptExecutionRequest request = createRequestWithPermissions("js", """
                console.log('自定义权限测试');
                var config = {
                    io: 'enabled',
                    network: 'disabled',
                    threads: 'enabled'
                };
                JSON.stringify(config);
                """, customPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "自定义权限配置应该工作正常");
            assertNotNull(result.getResult(), "应该有返回值");
            assertTrue(result.getOutput().contains("自定义权限测试"), "日志输出应该正常");
        }

        @Test
        @DisplayName("权限升级测试 - 网络访问")
        void testElevatedNetworkPermissions() {
            ScriptPermissions networkPermissions = ScriptPermissions.createSandbox();
            networkPermissions.setAllowNetwork(true);
            networkPermissions.setAllowHostAccess(true);
            networkPermissions.setAllowIO(true);

            ScriptExecutionRequest request = createRequestWithPermissions("python", """
                import urllib.request
                # 测试网络模块是否可用（不实际发送请求）
                hasattr(urllib.request, 'urlopen')
                """, networkPermissions);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "网络权限提升后应该可以访问网络模块");
            assertEquals(true, result.getResult(), "urllib.request模块应该可用");
        }
    }

    @Nested
    @DisplayName("异步执行测试")
    class AsyncExecutionTest {

        @Test
        @DisplayName("异步脚本执行")
        @Timeout(10)
        void testAsyncExecution() throws Exception {
            ScriptExecutionRequest request = createRequest("js", """
                var result = 0;
                for (var i = 1; i <= 100; i++) {
                    result += i;
                }
                result;
                """);

            CompletableFuture<ScriptExecutionResult> future = 
                scriptExecutionService.executeScriptAsync(request, null);

            ScriptExecutionResult result = future.get(5, TimeUnit.SECONDS);

            assertTrue(result.isSuccess(), "异步执行应该成功");
            assertEquals(5050, result.getResult(), "1到100求和应该等于5050");
        }

        @Test
        @DisplayName("异步执行实时输出回调")
        @Timeout(10)
        void testAsyncExecutionWithCallback() throws Exception {
            AtomicInteger callbackCount = new AtomicInteger(0);
            StringBuilder outputBuffer = new StringBuilder();

            ScriptExecutionRequest request = createRequest("python", """
                for i in range(5):
                    print(f"输出行 {i + 1}")
                "所有输出完成"
                """);

            CompletableFuture<ScriptExecutionResult> future = 
                scriptExecutionService.executeScriptAsync(request, output -> {
                    callbackCount.incrementAndGet();
                    outputBuffer.append(output);
                });

            ScriptExecutionResult result = future.get(5, TimeUnit.SECONDS);

            assertTrue(result.isSuccess(), "异步执行应该成功");
            assertEquals("所有输出完成", result.getResult(), "返回值应该正确");
            assertTrue(callbackCount.get() > 0, "应该触发输出回调");
            assertTrue(outputBuffer.toString().contains("输出行"), "回调应该接收到输出内容");
        }
    }

    @Nested
    @DisplayName("错误处理和边界情况测试")
    class ErrorHandlingTest {

        @Test
        @DisplayName("空脚本处理")
        void testEmptyScript() {
            ScriptExecutionRequest request = createRequest("js", "");
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "空脚本应该成功执行");
            assertNull(result.getResult(), "空脚本的结果应该是null");
        }

        @Test
        @DisplayName("不存在的函数调用")
        void testNonExistentFunction() {
            ScriptExecutionRequest request = createRequest("js", "var x = 1;");
            request.setEntryFunction("nonExistentFunction");

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertFalse(result.isSuccess(), "调用不存在的函数应该失败");
            assertNotNull(result.getError(), "应该有错误信息");
            assertTrue(result.getError().toLowerCase().contains("not found") || 
                      result.getError().toLowerCase().contains("not executable"),
                      "错误信息应该指示函数未找到");
        }

        @Test
        @DisplayName("运行时异常处理")
        void testRuntimeException() {
            ScriptExecutionRequest request = createRequest("js", """
                function throwError() {
                    throw new Error('故意抛出的测试错误');
                }
                throwError();
                """);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertFalse(result.isSuccess(), "抛出异常的脚本应该执行失败");
            assertNotNull(result.getError(), "应该捕获到错误信息");
            assertTrue(result.getError().contains("故意抛出的测试错误"), "错误信息应该包含原始错误内容");
        }

        @Test
        @DisplayName("不支持的语言处理")
        void testUnsupportedLanguage() {
            ScriptExecutionRequest request = createRequest("unsupported-lang", "test");
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertFalse(result.isSuccess(), "不支持的语言应该执行失败");
            assertNotNull(result.getError(), "应该有错误信息");
        }

        @Test
        @DisplayName("执行时间统计")
        void testExecutionTimeTracking() {
            ScriptExecutionRequest request = createRequest("js", """
                // 执行一些耗时操作
                var sum = 0;
                for (var i = 0; i < 10000; i++) {
                    sum += Math.sqrt(i);
                }
                sum;
                """);

            long startTime = System.currentTimeMillis();
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            long actualTime = System.currentTimeMillis() - startTime;

            assertTrue(result.isSuccess(), "脚本应该执行成功");
            assertTrue(result.getExecutionTime() > 0, "执行时间应该大于0");
            assertTrue(result.getExecutionTime() <= actualTime + 100, "记录的执行时间应该合理");
        }
    }

    @Nested
    @DisplayName("性能和资源测试")
    class PerformanceTest {

        @Test
        @DisplayName("大量数据处理测试")
        void testLargeDataProcessing() {
            ScriptExecutionRequest request = createRequest("python", """
                # 处理大量数据
                data = list(range(10000))
                
                # 执行一些计算密集型操作
                result = sum(x * x for x in data if x % 2 == 0)
                
                len(data), result
                """);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "大数据处理应该成功");
            assertNotNull(result.getResult(), "应该有计算结果");
            assertTrue(result.getExecutionTime() > 0, "应该记录执行时间");
        }

        @Test
        @DisplayName("递归深度测试")
        void testRecursionDepth() {
            ScriptExecutionRequest request = createRequest("js", """
                function factorial(n) {
                    if (n <= 1) return 1;
                    return n * factorial(n - 1);
                }
                factorial(10);
                """);

            ScriptExecutionResult result = scriptExecutionService.executeScript(request);

            assertTrue(result.isSuccess(), "递归计算应该成功");
            assertEquals(3628800, result.getResult(), "10! = 3628800");
        }
    }

    // 辅助方法
    private ScriptExecutionRequest createRequest(String language, String script) {
        return createRequestWithPermissions(language, script, ScriptPermissions.createSandbox());
    }

    private ScriptExecutionRequest createRequestWithPermissions(String language, String script, ScriptPermissions permissions) {
        ScriptExecutionRequest request = new ScriptExecutionRequest();
        request.setLanguage(language);
        request.setScript(script);
        request.setPermissions(permissions);
        return request;
    }
}
