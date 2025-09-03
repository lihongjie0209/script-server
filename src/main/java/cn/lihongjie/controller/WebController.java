package cn.lihongjie.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import java.net.URI;

@Path("/")
public class WebController {
    
    @GET
    public Response index(@Context UriInfo uriInfo, @Context HttpHeaders headers) {
        try {
            // 检查是否有反向代理头部信息
            String originalHost = headers.getHeaderString("X-Original-Host");
            String forwardedHost = headers.getHeaderString("X-Forwarded-Host");
            String forwardedProto = headers.getHeaderString("X-Forwarded-Proto");
            String forwardedPrefix = headers.getHeaderString("X-Forwarded-Prefix");
            
            // 优先使用 X-Original-Host，然后是 X-Forwarded-Host
            String targetHost = originalHost != null ? originalHost : 
                               (forwardedHost != null ? forwardedHost : uriInfo.getBaseUri().getHost());
            
            // 确定协议
            String protocol = forwardedProto != null ? forwardedProto : uriInfo.getBaseUri().getScheme();
            
            // 构建重定向URI
            if (originalHost != null || forwardedHost != null) {
                // 在反向代理环境下，构建完整的重定向URL
                StringBuilder redirectUrl = new StringBuilder();
                redirectUrl.append(protocol).append("://").append(targetHost);
                
                // 添加端口（如果不是标准端口）
                if (forwardedHost == null || !forwardedHost.contains(":")) {
                    int port = uriInfo.getBaseUri().getPort();
                    if ((!"https".equals(protocol) || port != 443) && 
                        (!"http".equals(protocol) || port != 80) && 
                        port != -1) {
                        redirectUrl.append(":").append(port);
                    }
                }
                
                // 添加前缀路径
                if (forwardedPrefix != null && !forwardedPrefix.isEmpty()) {
                    if (!forwardedPrefix.startsWith("/")) {
                        redirectUrl.append("/");
                    }
                    redirectUrl.append(forwardedPrefix);
                    if (!forwardedPrefix.endsWith("/")) {
                        redirectUrl.append("/");
                    }
                } else {
                    redirectUrl.append("/");
                }
                
                redirectUrl.append("index.html");
                
                return Response.seeOther(URI.create(redirectUrl.toString())).build();
            } else {
                // 本地环境，使用相对路径
                return Response.seeOther(URI.create("index.html")).build();
            }
        } catch (Exception e) {
            // 出错时使用简单的相对路径重定向
            return Response.seeOther(URI.create("index.html")).build();
        }
    }
}
