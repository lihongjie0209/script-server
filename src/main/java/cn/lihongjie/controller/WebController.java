package cn.lihongjie.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class WebController {
    
    @GET
    public Response index() {
        // 重定向到静态资源目录下的index.html
        return Response.seeOther(URI.create("/index.html")).build();
    }
}
