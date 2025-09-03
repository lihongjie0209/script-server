package cn.lihongjie.test;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Engine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/test")
@ApplicationScoped
public class GraalVMTestController {
    
    @GET
    @Path("/setTimeout")
    @Produces(MediaType.TEXT_PLAIN)
    public String testSetTimeout() {
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("=== GraalVM JavaScript setTimeout Test ===\n");
            
            // 检查 JavaScript 引擎版本
            Engine engine = Engine.create();
            result.append("GraalVM Engine version: ").append(engine.getVersion()).append("\n");
            result.append("Available languages: ").append(String.join(", ", engine.getLanguages().keySet())).append("\n\n");
            
            // 测试基本的 setTimeout 支持
            try (Context context = Context.newBuilder("js")
                    .allowPolyglotAccess(PolyglotAccess.ALL)
                    .allowCreateThread(true)
                    .allowExperimentalOptions(true)
                    .option("js.console", "true")
                    .build()) {
                
                // 注入 setTimeout polyfill
                injectSetTimeoutPolyfill(context);
                
                // 检查 setTimeout 是否可用
                Value setTimeoutType = context.eval("js", "typeof setTimeout");
                result.append("typeof setTimeout: ").append(setTimeoutType.asString()).append("\n");
                
                // 检查 Promise 是否可用
                Value promiseType = context.eval("js", "typeof Promise");
                result.append("typeof Promise: ").append(promiseType.asString()).append("\n");
                
                // 检查 console 对象
                Value consoleType = context.eval("js", "typeof console");
                result.append("typeof console: ").append(consoleType.asString()).append("\n\n");
                
                // 检查全局对象
                Value globalBindings = context.getBindings("js");
                result.append("Available global functions: ");
                for (String key : globalBindings.getMemberKeys()) {
                    result.append(key).append(", ");
                }
                result.append("\n\n");
                
                // 尝试执行简单的同步代码
                try {
                    Value simpleResult = context.eval("js", 
                        "var result = 'sync code executed'; " +
                        "result;"
                    );
                    result.append("Simple sync result: ").append(simpleResult.asString()).append("\n");
                } catch (Exception e) {
                    result.append("Sync code error: ").append(e.getMessage()).append("\n");
                }
                
                // 尝试 setTimeout 测试 - 测试 polyfill 是否工作
                try {
                    result.append("=== Testing setTimeout polyfill ===\n");
                    
                    // 测试基本的 setTimeout 功能
                    Value timeoutResult = context.eval("js", 
                        "var testResult = 'before timeout';" +
                        "setTimeout(function() { testResult = 'timeout executed'; }, 50);" +
                        "testResult;"
                    );
                    result.append("setTimeout initial result: ").append(timeoutResult.asString()).append("\n");
                    
                    // 给点时间让 setTimeout 执行（如果是异步的话）
                    Thread.sleep(100);
                    
                    // 检查结果
                    Value finalResult = context.eval("js", "testResult");
                    result.append("setTimeout final result: ").append(finalResult.asString()).append("\n");
                    
                    // 测试回调函数是否被执行
                    Value callbackTest = context.eval("js", 
                        "var callbackExecuted = false;" +
                        "setTimeout(function() { callbackExecuted = true; console.log('Callback executed!'); }, 10);" +
                        "callbackExecuted;"
                    );
                    result.append("Callback test result: ").append(callbackTest.asString()).append("\n");
                    
                    Thread.sleep(50);
                    Value callbackFinal = context.eval("js", "callbackExecuted");
                    result.append("Callback final result: ").append(callbackFinal.asString()).append("\n");
                    
                } catch (Exception e) {
                    result.append("setTimeout test error: ").append(e.getMessage()).append("\n");
                }
                
            } catch (Exception e) {
                result.append("Context creation error: ").append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            result.append("General error: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        
        return result.toString();
    }
    
    /**
     * 注入 setTimeout polyfill
     */
    private void injectSetTimeoutPolyfill(Context context) {
        String setTimeoutPolyfill = """
            // setTimeout polyfill for GraalJS test
            (function() {
                globalThis.setTimeout = function(callback, delay) {
                    var start = Date.now();
                    while (Date.now() - start < (delay || 0)) {
                        // Busy wait
                    }
                    if (typeof callback === 'function') {
                        try {
                            callback();
                        } catch (e) {
                            console.error('setTimeout callback error:', e.message || e);
                        }
                    }
                    return Math.random(); // 返回一个假的ID
                };
                
                globalThis.clearTimeout = function(timeoutId) {
                    // No-op in test implementation
                };
                
                console.log('setTimeout polyfill loaded for testing');
            })();
            """;
        
        try {
            context.eval("js", setTimeoutPolyfill);
        } catch (Exception e) {
            System.err.println("Failed to inject setTimeout polyfill in test: " + e.getMessage());
        }
    }
}
