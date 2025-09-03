package cn.lihongjie.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptPermissions {
    
    private boolean allowIO = false; // 是否允许IO操作
    private boolean allowNetwork = false; // 是否允许网络访问
    private boolean allowHostAccess = false; // 是否允许主机访问
    private boolean allowFileAccess = false; // 是否允许文件访问
    private boolean allowCreateThread = false; // 是否允许创建线程
    private boolean allowEnvironmentAccess = false; // 是否允许环境变量访问
    private long maxExecutionTime = 30000; // 最大执行时间（毫秒）
    private long maxMemoryUsage = 128 * 1024 * 1024; // 最大内存使用（字节）
    
    public ScriptPermissions() {}
    
    public ScriptPermissions(boolean allowIO, boolean allowNetwork, boolean allowHostAccess, 
                           boolean allowFileAccess, boolean allowCreateThread, 
                           boolean allowEnvironmentAccess, long maxExecutionTime, long maxMemoryUsage) {
        this.allowIO = allowIO;
        this.allowNetwork = allowNetwork;
        this.allowHostAccess = allowHostAccess;
        this.allowFileAccess = allowFileAccess;
        this.allowCreateThread = allowCreateThread;
        this.allowEnvironmentAccess = allowEnvironmentAccess;
        this.maxExecutionTime = maxExecutionTime;
        this.maxMemoryUsage = maxMemoryUsage;
    }
    
    // 创建默认的安全沙盒配置
    public static ScriptPermissions createSandbox() {
        return new ScriptPermissions(false, false, false, false, false, false, 30000, 128 * 1024 * 1024);
    }
    
    // 创建宽松的配置
    public static ScriptPermissions createPermissive() {
        return new ScriptPermissions(true, true, true, true, true, true, 60000, 512 * 1024 * 1024);
    }
    
    // Getters and Setters
    public boolean isAllowIO() {
        return allowIO;
    }
    
    public void setAllowIO(boolean allowIO) {
        this.allowIO = allowIO;
    }
    
    public boolean isAllowNetwork() {
        return allowNetwork;
    }
    
    public void setAllowNetwork(boolean allowNetwork) {
        this.allowNetwork = allowNetwork;
    }
    
    public boolean isAllowHostAccess() {
        return allowHostAccess;
    }
    
    public void setAllowHostAccess(boolean allowHostAccess) {
        this.allowHostAccess = allowHostAccess;
    }
    
    public boolean isAllowFileAccess() {
        return allowFileAccess;
    }
    
    public void setAllowFileAccess(boolean allowFileAccess) {
        this.allowFileAccess = allowFileAccess;
    }
    
    public boolean isAllowCreateThread() {
        return allowCreateThread;
    }
    
    public void setAllowCreateThread(boolean allowCreateThread) {
        this.allowCreateThread = allowCreateThread;
    }
    
    public boolean isAllowEnvironmentAccess() {
        return allowEnvironmentAccess;
    }
    
    public void setAllowEnvironmentAccess(boolean allowEnvironmentAccess) {
        this.allowEnvironmentAccess = allowEnvironmentAccess;
    }
    
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }
    
    public void setMaxExecutionTime(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }
    
    public long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }
    
    public void setMaxMemoryUsage(long maxMemoryUsage) {
        this.maxMemoryUsage = maxMemoryUsage;
    }
}
