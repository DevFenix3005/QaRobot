package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.qarobot.RestActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.RestMethod;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SetType;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.di.modules.RemoteModule;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import com.sun.net.httpserver.HttpServer;
import kong.unirest.GenericType;
import kong.unirest.UnirestInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises real HTTP and JSONPath with only the browser-facing helper replaced. */
class RestActionTest {
    private HttpServer server;
    private UnirestInstance client;
    private String baseUrl;
    private final Map<String, Object> context = new HashMap<>();
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestContentType = new AtomicReference<>();

    @BeforeEach
    void startLocalApi() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] body = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        respond("/items", 200, "[{\"name\":\"primero\"},{\"name\":\"segundo\"}]");
        respond("/empty", 204, "");
        respond("/invalid", 200, "{invalid-json");
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = RemoteModule.unirestProvide();
    }

    @AfterEach
    void stopLocalApi() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void postPreservesTheBodyAndStoresJsonPathValuesUsingJackson3() {
        RestActionType dto = action(RestMethod.POST, "/echo");
        String body = """
                {"name":"Prueba ñ $5","path":"C:\\\\temp","count":3,"ok":true,"missing":null}
                """;
        context.put("requestBody", body);
        dto.setBody("${requestBody}");
        dto.getHeader().add(value("Content-Type", "application/json; charset=utf-8"));
        dto.getStorage().add(value("name", "$.name"));
        dto.getStorage().add(value("path", "$.path"));
        dto.getStorage().add(value("count", "$.count"));
        dto.getStorage().add(value("ok", "$.ok"));
        dto.getStorage().add(value("missing", "$.missing"));

        run(dto);

        assertEquals("POST", requestMethod.get());
        assertEquals("application/json; charset=utf-8", requestContentType.get());
        assertEquals("Prueba ñ $5", context.get("name"));
        assertEquals("C:\\temp", context.get("path"));
        assertEquals("3", context.get("count"));
        assertEquals("true", context.get("ok"));
        assertEquals("null", context.get("missing"));
    }

    @Test
    void getSupportsAnArrayAtTheResponseRoot() {
        RestActionType dto = action(RestMethod.GET, "/items");
        dto.getStorage().add(value("second", "$[1].name"));
        dto.getStorage().add(value("names", "$[*].name"));
        dto.getStorage().add(value("first", "$[0]"));

        run(dto);

        assertEquals("segundo", context.get("second"));
        assertEquals("[\"primero\",\"segundo\"]", context.get("names"));
        assertEquals("{\"name\":\"primero\"}", context.get("first"));
    }

    @Test
    void acceptsNoContentWhenThereIsNothingToStore() {
        assertDoesNotThrow(() -> run(action(RestMethod.DELETE, "/empty")));
    }

    @Test
    void reportsMalformedJsonInsteadOfSilentlyStoringAnEmptyObject() {
        assertThrows(JacksonException.class, () -> run(action(RestMethod.GET, "/invalid")));
    }

    @Test
    void unirestAdapterRoundTripsTypedAndGenericBodiesWithJavaTime() {
        ApiResult expected = new ApiResult("QaRobot", LocalDate.of(2026, 9, 28));
        var response = client.post(baseUrl + "/echo")
                .header("Content-Type", "application/json")
                .body(expected).asObject(ApiResult.class);
        assertFalse(response.getParsingError().isPresent());
        assertEquals(expected, response.getBody());

        var generic = client.post(baseUrl + "/echo")
                .header("Content-Type", "application/json")
                .body(List.of(expected)).asObject(new GenericType<List<ApiResult>>() {});
        assertFalse(generic.getParsingError().isPresent());
        assertEquals(List.of(expected), generic.getBody());
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            if (status != 204) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    private RestActionType action(RestMethod method, String path) {
        RestActionType dto = new RestActionType();
        dto.setId("rest-smoke");
        dto.setDesc("Local HTTP test");
        dto.setMethod(method);
        dto.setUrl(baseUrl + path);
        dto.setTimeout(BigInteger.ZERO);
        return dto;
    }

    private void run(RestActionType dto) {
        SeleniumHelper helper = (SeleniumHelper) Proxy.newProxyInstance(
                SeleniumHelper.class.getClassLoader(), new Class<?>[]{SeleniumHelper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "verificacionesOk" -> true;
                    case "getValueFormContext" -> context.get(args[0]);
                    case "getInterpolationOfValueIfExistsOrGetRawValue" ->
                            InterpolationResult.create(null, (String) args[0], false);
                    case "addValue2Contexto" -> context.put((String) args[0], args[1]);
                    case "actionLog", "delay", "sendAction2View" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        RestAction action = new RestAction(helper, client, Map.of(PatternEnum.INTERPOLATION_PATTERN,
                Pattern.compile("^(?<interpolation>\\$\\{(?<value>[\\w\\-.]+)})$")));
        action.setAction(dto);
        action.run();
    }

    private SetType value(String key, String value) {
        SetType set = new SetType();
        set.setKey(key);
        set.setValue(value);
        return set;
    }

    public record ApiResult(String name, LocalDate date) {
    }
}
