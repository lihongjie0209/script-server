package cn.lihongjie.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import java.net.URI;

@Path("/")
public class WebController {
    
    @GET
    public Response index(@Context UriInfo uriInfo) {
        // 使用相对路径重定向，避免反向代理问题
        URI baseUri = uriInfo.getBaseUri();
        URI indexUri = baseUri.resolve("index.html");
        return Response.seeOther(indexUri).build();
    }
}
