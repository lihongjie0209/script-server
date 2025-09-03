package cn.lihongjie.controller;

import cn.lihongjie.model.ScriptExecutionRequest;
import cn.lihongjie.model.ScriptExecutionResult;
import cn.lihongjie.model.ScriptPermissions;
import cn.lihongjie.service.ScriptExecutionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/script")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScriptController {
    
    @Inject
    ScriptExecutionService scriptExecutionService;
    
    /**
     * 执行脚本
     */
    @POST
    @Path("/execute")
    public Response executeScript(ScriptExecutionRequest request) {
        try {
            // 验证语言是否支持
            if (!scriptExecutionService.isLanguageAvailable(request.getLanguage())) {
                String[] availableLanguages = scriptExecutionService.getAvailableLanguages();
                String errorMsg = String.format("语言 '%s' 不支持。可用语言: %s", 
                    request.getLanguage(), String.join(", ", availableLanguages));
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ScriptExecutionResult.error(errorMsg, "", 0))
                        .build();
            }
            
            ScriptExecutionResult result = scriptExecutionService.executeScript(request);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ScriptExecutionResult.error(e.getMessage(), "", 0))
                    .build();
        }
    }
    
    /**
     * 获取默认的沙盒权限配置
     */
    @GET
    @Path("/permissions/sandbox")
    public Response getSandboxPermissions() {
        ScriptPermissions permissions = ScriptPermissions.createSandbox();
        return Response.ok(permissions).build();
    }
    
    /**
     * 获取宽松的权限配置
     */
    @GET
    @Path("/permissions/permissive")
    public Response getPermissivePermissions() {
        ScriptPermissions permissions = ScriptPermissions.createPermissive();
        return Response.ok(permissions).build();
    }
    
    /**
     * 健康检查
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok("{\"status\":\"UP\",\"service\":\"script-execution\"}").build();
    }
    
    /**
     * 获取支持的脚本语言
     */
    @GET
    @Path("/languages")
    public Response getSupportedLanguages() {
        String[] languages = scriptExecutionService.getAvailableLanguages();
        return Response.ok(languages).build();
    }
}
