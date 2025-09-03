package cn.lihongjie.websocket;

import cn.lihongjie.model.ScriptExecutionRequest;
import cn.lihongjie.model.ScriptExecutionResult;
import cn.lihongjie.service.ScriptExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@ServerEndpoint("/ws/script")
@ApplicationScoped
public class ScriptExecutionWebSocket {
    
    @Inject
    ScriptExecutionService scriptExecutionService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        sendMessage(session, createMessage("connection", "Connected to script execution service", null));
    }
    
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        sendMessage(session, createMessage("error", "WebSocket error: " + throwable.getMessage(), null));
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            ScriptExecutionRequest request = objectMapper.readValue(message, ScriptExecutionRequest.class);
            
            // 发送开始执行消息
            sendMessage(session, createMessage("start", "Script execution started", null));
            
            // 异步执行脚本
            CompletableFuture<ScriptExecutionResult> future = scriptExecutionService.executeScriptAsync(
                request, 
                output -> {
                    // 实时发送输出
                    sendMessage(session, createMessage("output", output, null));
                }
            );
            
            // 处理执行结果
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    sendMessage(session, createMessage("error", "Execution failed: " + throwable.getMessage(), null));
                } else {
                    sendMessage(session, createMessage("result", "Script execution completed", result));
                }
            });
            
        } catch (Exception e) {
            sendMessage(session, createMessage("error", "Invalid request: " + e.getMessage(), null));
        }
    }
    
    private void sendMessage(Session session, String message) {
        try {
            if (session.isOpen()) {
                // 使用异步发送避免在IO线程中阻塞
                session.getAsyncRemote().sendText(message);
            }
        } catch (Exception e) {
            // 忽略发送失败
        }
    }
    
    private String createMessage(String type, String message, Object data) {
        try {
            WebSocketMessage wsMessage = new WebSocketMessage(type, message, data, System.currentTimeMillis());
            return objectMapper.writeValueAsString(wsMessage);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"message\":\"Failed to serialize message\",\"timestamp\":" + System.currentTimeMillis() + "}";
        }
    }
    
    // WebSocket消息格式
    public static class WebSocketMessage {
        private String type;
        private String message;
        private Object data;
        private long timestamp;
        
        public WebSocketMessage() {}
        
        public WebSocketMessage(String type, String message, Object data, long timestamp) {
            this.type = type;
            this.message = message;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
