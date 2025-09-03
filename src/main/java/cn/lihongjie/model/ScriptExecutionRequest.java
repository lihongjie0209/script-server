package cn.lihongjie.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptExecutionRequest {
    
    private String script;
    private String language = "js"; // 默认JavaScript
    private String entryFunction; // 入口函数名称
    private ScriptPermissions permissions; // 脚本权限配置
    private Object[] args; // 传递给入口函数的参数
    
    public ScriptExecutionRequest() {}
    
    public ScriptExecutionRequest(String script, String language, String entryFunction, ScriptPermissions permissions, Object[] args) {
        this.script = script;
        this.language = language;
        this.entryFunction = entryFunction;
        this.permissions = permissions;
        this.args = args;
    }
    
    // Getters and Setters
    public String getScript() {
        return script;
    }
    
    public void setScript(String script) {
        this.script = script;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getEntryFunction() {
        return entryFunction;
    }
    
    public void setEntryFunction(String entryFunction) {
        this.entryFunction = entryFunction;
    }
    
    public ScriptPermissions getPermissions() {
        return permissions;
    }
    
    public void setPermissions(ScriptPermissions permissions) {
        this.permissions = permissions;
    }
    
    public Object[] getArgs() {
        return args;
    }
    
    public void setArgs(Object[] args) {
        this.args = args;
    }
}
