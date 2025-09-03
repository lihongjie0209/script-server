package cn.lihongjie.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptExecutionResult {
    
    private boolean success;
    private Object result;
    private String error;
    private String output; // 脚本输出日志
    private long executionTime; // 执行时间（毫秒）
    private long memoryUsed; // 内存使用量（字节）
    
    public ScriptExecutionResult() {}
    
    public ScriptExecutionResult(boolean success, Object result, String error, String output, long executionTime, long memoryUsed) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.output = output;
        this.executionTime = executionTime;
        this.memoryUsed = memoryUsed;
    }
    
    public static ScriptExecutionResult success(Object result, String output, long executionTime, long memoryUsed) {
        return new ScriptExecutionResult(true, result, null, output, executionTime, memoryUsed);
    }
    
    public static ScriptExecutionResult error(String error, String output, long executionTime) {
        return new ScriptExecutionResult(false, null, error, output, executionTime, 0);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
    
    public long getMemoryUsed() {
        return memoryUsed;
    }
    
    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }
}
