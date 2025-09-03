package cn.lihongjie.service;

import cn.lihongjie.model.ScriptExecutionRequest;
import cn.lihongjie.model.ScriptExecutionResult;
import cn.lihongjie.model.ScriptPermissions;
import jakarta.enterprise.context.ApplicationScoped;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@ApplicationScoped
public class ScriptExecutionService {
    
    /**
     * 获取可用的语言列表
     */
    public String[] getAvailableLanguages() {
        try (Context context = Context.newBuilder().allowPolyglotAccess(PolyglotAccess.ALL).build()) {
            return context.getEngine().getLanguages().keySet().toArray(new String[0]);
        } catch (Exception e) {
            // 如果GraalVM语言不可用，返回基本支持
            return new String[]{"js", "python"};
        }
    }
    
    /**
     * 检查指定语言是否可用
     */
    public boolean isLanguageAvailable(String language) {
        try (Context context = Context.newBuilder().allowPolyglotAccess(PolyglotAccess.ALL).build()) {
            return context.getEngine().getLanguages().containsKey(language);
        } catch (Exception e) {
            return "js".equals(language) || "python".equals(language);
        }
    }
    
    /**
     * 执行脚本
     */
    public ScriptExecutionResult executeScript(ScriptExecutionRequest request) {
        return executeScript(request, null);
    }
    
    /**
     * 执行脚本，支持实时输出回调
     */
    public ScriptExecutionResult executeScript(ScriptExecutionRequest request, Consumer<String> outputCallback) {
        long startTime = System.currentTimeMillis();
        
        // 使用实时输出流
        RealTimeOutputStream realTimeOutput = new RealTimeOutputStream(outputCallback);
        PrintStream printStream = new PrintStream(realTimeOutput);
        
        try {
            // 创建引擎配置
            Context.Builder contextBuilder = createContextBuilder(request.getPermissions());
            
            // 设置输出流
            contextBuilder.out(printStream).err(printStream);
            
            try (Context context = contextBuilder.build()) {
                
                // 执行脚本
                Value result = executeInContext(context, request);
                
                long executionTime = System.currentTimeMillis() - startTime;
                String output = realTimeOutput.getFullOutput();
                
                // 转换结果
                Object resultValue = convertValue(result);
                
                return ScriptExecutionResult.success(resultValue, output, executionTime, 0);
                
            }
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String output = realTimeOutput.getFullOutput();
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            
            if (outputCallback != null) {
                outputCallback.accept("ERROR: " + errorMessage);
            }
            
            return ScriptExecutionResult.error(errorMessage, output, executionTime);
        } finally {
            printStream.close();
        }
    }
    
    /**
     * 异步执行脚本
     */
    public CompletableFuture<ScriptExecutionResult> executeScriptAsync(ScriptExecutionRequest request, Consumer<String> outputCallback) {
        return CompletableFuture.supplyAsync(() -> executeScript(request, outputCallback));
    }
    
    /**
     * 创建上下文构建器
     */
    private Context.Builder createContextBuilder(ScriptPermissions permissions) {
        final ScriptPermissions finalPermissions = permissions != null ? permissions : ScriptPermissions.createSandbox();
        
        Context.Builder builder = Context.newBuilder()
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .allowExperimentalOptions(true)  // 启用实验性选项
                // 启用 JavaScript 基本功能
                .option("js.console", "true");

        
        // 配置IO访问
        if (finalPermissions.isAllowIO()) {
            builder.allowIO(IOAccess.ALL);
        } else {
            builder.allowIO(IOAccess.NONE);
        }
        
        // 配置主机访问 - 原生模块需要主机访问权限
        if (finalPermissions.isAllowHostAccess() || finalPermissions.isAllowNetwork()) {
            builder.allowHostAccess(HostAccess.ALL);
        } else {
            builder.allowHostAccess(HostAccess.NONE);
        }
        
        // 配置网络访问 - 启用主机类查找以支持网络操作
        if (finalPermissions.isAllowNetwork()) {
            builder.allowHostClassLookup(className -> true);
        } else {
            builder.allowHostClassLookup(className -> false);
        }
        
        // 配置创建线程
        builder.allowCreateThread(finalPermissions.isAllowCreateThread());
        
        // 配置环境变量访问
        if (finalPermissions.isAllowEnvironmentAccess()) {
            builder.allowEnvironmentAccess(EnvironmentAccess.INHERIT);
        } else {
            builder.allowEnvironmentAccess(EnvironmentAccess.NONE);
        }
        
        // 设置资源限制
        if (finalPermissions.getMaxExecutionTime() > 0) {
            // 注意：GraalVM的资源限制API可能需要额外配置
            // 这里我们会在执行时通过超时机制来控制
        }
        
        return builder;
    }
    
    /**
     * 在上下文中执行脚本
     */
    private Value executeInContext(Context context, ScriptExecutionRequest request) throws Exception {
        // 如果是 JavaScript，先注入 setTimeout 和其他 polyfills
        if ("js".equals(request.getLanguage()) || "javascript".equals(request.getLanguage())) {
            injectJavaScriptPolyfills(context);
        }
        
        Value result;
        
        if (request.getEntryFunction() != null && !request.getEntryFunction().isEmpty()) {
            // 先执行脚本定义函数
            context.eval(request.getLanguage(), request.getScript());
            
            // 然后调用指定的入口函数
            Value function = context.getBindings(request.getLanguage()).getMember(request.getEntryFunction());
            if (function == null || !function.canExecute()) {
                throw new RuntimeException("Function '" + request.getEntryFunction() + "' not found or not executable");
            }
            
            Object[] args = request.getArgs() != null ? request.getArgs() : new Object[0];
            result = function.execute(args);
        } else {
            // 直接执行脚本
            result = context.eval(request.getLanguage(), request.getScript());
        }
        
        return result;
    }
    
    /**
     * 为 JavaScript 上下文注入 polyfills
     */
    private void injectJavaScriptPolyfills(Context context) {
        // 注入 setTimeout 和 clearTimeout 实现
        // 由于沙盒限制，使用简单的忙等待实现
        String setTimeoutPolyfill = """
            // setTimeout and clearTimeout polyfill for GraalJS
            (function() {
                var timeoutId = 0;
                var timeouts = {};
                
                globalThis.setTimeout = function(callback, delay) {
                    var id = ++timeoutId;
                    
                    // 简单的忙等待实现 - 在沙盒环境中更安全
                    setTimeout._busyWait(callback, delay || 0);
                    
                    return id;
                };
                
                // 忙等待辅助函数
                setTimeout._busyWait = function(callback, delay) {
                    var start = Date.now();
                    while (Date.now() - start < delay) {
                        // Busy wait - 在小延时下是可以接受的
                        if (Date.now() - start >= delay) {
                            break;
                        }
                    }
                    
                    if (typeof callback === 'function') {
                        try {
                            callback();
                        } catch (e) {
                            console.error('setTimeout callback error:', e.message || e);
                        }
                    }
                };
                
                globalThis.clearTimeout = function(timeoutId) {
                    // 简单实现 - 由于忙等待是同步的，clearTimeout 无法真正取消
                    if (timeouts[timeoutId]) {
                        delete timeouts[timeoutId];
                    }
                };
                
                // 简单的 setInterval 实现 - 仅作演示，实际使用中应避免
                globalThis.setInterval = function(callback, delay) {
                    console.warn('setInterval in sandbox environment may cause performance issues');
                    var intervalId = ++timeoutId;
                    
                    // 这是一个简化实现，实际不推荐在生产环境使用
                    var executeInterval = function() {
                        if (timeouts[intervalId]) {
                            setTimeout._busyWait(function() {
                                if (timeouts[intervalId] && typeof callback === 'function') {
                                    try {
                                        callback();
                                        executeInterval(); // 递归调用
                                    } catch (e) {
                                        console.error('setInterval callback error:', e.message || e);
                                        delete timeouts[intervalId];
                                    }
                                }
                            }, delay || 0);
                        }
                    };
                    
                    timeouts[intervalId] = true;
                    executeInterval();
                    
                    return intervalId;
                };
                
                globalThis.clearInterval = function(intervalId) {
                    if (timeouts[intervalId]) {
                        delete timeouts[intervalId];
                    }
                };
                
                // 添加一些有用的信息
                console.log('setTimeout polyfill loaded for GraalJS sandbox environment');
            })();
            """;
        
        try {
            context.eval("js", setTimeoutPolyfill);
        } catch (Exception e) {
            // 如果上面的实现失败，使用最简单的版本
            String fallbackSetTimeout = """
                // Fallback setTimeout implementation
                globalThis.setTimeout = function(callback, delay) {
                    var start = Date.now();
                    while (Date.now() - start < (delay || 0)) {
                        // Busy wait
                    }
                    if (typeof callback === 'function') {
                        callback();
                    }
                    return Math.random(); // 返回一个假的ID
                };
                
                globalThis.clearTimeout = function(timeoutId) {
                    // No-op in fallback implementation
                };
                
                console.log('Fallback setTimeout implementation loaded');
                """;
            
            try {
                context.eval("js", fallbackSetTimeout);
            } catch (Exception e2) {
                // 如果连最简单的实现都失败，记录错误但不抛出异常
                System.err.println("Failed to inject setTimeout polyfill: " + e2.getMessage());
            }
        }
    }
    
    /**
     * 转换GraalVM Value为Java对象
     */
    private Object convertValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        
        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInDouble()) {
                return value.asDouble();
            }
        } else if (value.isString()) {
            return value.asString();
        } else if (value.hasArrayElements()) {
            // 处理数组
            Object[] array = new Object[(int) value.getArraySize()];
            for (int i = 0; i < array.length; i++) {
                array[i] = convertValue(value.getArrayElement(i));
            }
            return array;
        } else if (value.hasMembers()) {
            // 处理对象 - 这里简化处理，返回字符串表示
            return value.toString();
        }
        
        return value.toString();
    }
    
    /**
     * 实时输出流，用于捕获并即时转发脚本输出
     */
    private static class RealTimeOutputStream extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final Consumer<String> outputCallback;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        
        public RealTimeOutputStream(Consumer<String> outputCallback) {
            this.outputCallback = outputCallback;
        }
        
        @Override
        public void write(int b) {
            if (closed.get()) return;
            
            buffer.write(b);
            
            // 如果遇到换行符或者达到一定长度，就发送输出
            if (b == '\n' || buffer.size() > 100) {
                flushOutput();
            }
        }
        
        @Override
        public void write(byte[] b, int off, int len) {
            if (closed.get()) return;
            
            buffer.write(b, off, len);
            
            // 检查是否包含换行符
            boolean hasNewline = false;
            for (int i = off; i < off + len; i++) {
                if (b[i] == '\n') {
                    hasNewline = true;
                    break;
                }
            }
            
            if (hasNewline || buffer.size() > 100) {
                flushOutput();
            }
        }
        
        @Override
        public void flush() {
            flushOutput();
        }
        
        @Override
        public void close() {
            if (!closed.getAndSet(true)) {
                flushOutput();
            }
        }
        
        private void flushOutput() {
            if (outputCallback != null && buffer.size() > 0) {
                String output = buffer.toString();
                buffer.reset();
                outputCallback.accept(output);
            }
        }
        
        public String getFullOutput() {
            return buffer.toString();
        }
    }
}
