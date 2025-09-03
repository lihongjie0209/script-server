package cn.lihongjie.controller;

import cn.lihongjie.model.ScriptExecutionRequest;
import cn.lihongjie.model.ScriptPermissions;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ScriptControllerTest {

    @Test
    public void testHealthEndpoint() {
        given()
                .when().get("/api/script/health")
                .then()
                .statusCode(200)
                .body(containsString("UP"));
    }

    @Test
    public void testGetSupportedLanguages() {
        given()
                .when().get("/api/script/languages")
                .then()
                .statusCode(200)
                .body("$", hasItems("js", "python"));
                // 移除对ruby的检查，因为现在支持的是llvm, js, python
    }

    @Test
    public void testGetSandboxPermissions() {
        given()
                .when().get("/api/script/permissions/sandbox")
                .then()
                .statusCode(200)
                .body("allowIO", is(false))
                .body("allowNetwork", is(false))
                .body("maxExecutionTime", is(30000));
    }

    @Test
    public void testExecuteSimpleJavaScript() {
        ScriptExecutionRequest request = new ScriptExecutionRequest();
        request.setScript("1 + 2 + 3");
        request.setLanguage("js");
        request.setPermissions(ScriptPermissions.createSandbox());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/script/execute")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("result", is(6));
    }

    @Test
    public void testExecuteJavaScriptFunction() {
        ScriptExecutionRequest request = new ScriptExecutionRequest();
        request.setScript("function add(a, b) { return a + b; }");
        request.setLanguage("js");
        request.setEntryFunction("add");
        request.setArgs(new Object[]{10, 20});
        request.setPermissions(ScriptPermissions.createSandbox());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/script/execute")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("result", is(30));
    }

    @Test
    public void testExecuteWithConsoleOutput() {
        ScriptExecutionRequest request = new ScriptExecutionRequest();
        request.setScript("console.log('Hello World'); 'result'");
        request.setLanguage("js");
        request.setPermissions(ScriptPermissions.createSandbox());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/script/execute")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("result", is("result"))
                .body("output", containsString("Hello World"));
    }

    @Test
    public void testExecuteInvalidScript() {
        ScriptExecutionRequest request = new ScriptExecutionRequest();
        request.setScript("invalid javascript syntax !!!!");
        request.setLanguage("js");
        request.setPermissions(ScriptPermissions.createSandbox());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/script/execute")
                .then()
                .statusCode(200)
                .body("success", is(false))
                .body("error", notNullValue());
    }
}
