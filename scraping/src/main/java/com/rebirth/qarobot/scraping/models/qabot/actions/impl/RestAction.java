package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.spi.json.Jackson3JsonProvider;
import com.jayway.jsonpath.spi.mapper.Jackson3MappingProvider;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import kong.unirest.HttpRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.qarobot.RestActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.RestMethod;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SetType;
import com.rebirth.qarobot.commons.utils.Constantes;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;

import javax.inject.Inject;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public final class RestAction extends Action<RestActionType> {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final Configuration JSON_PATH = Configuration.builder()
            .jsonProvider(new Jackson3JsonProvider(JSON_MAPPER))
            .mappingProvider(new Jackson3MappingProvider(JSON_MAPPER))
            .build();

    private final AtomicReference<String> payload = new AtomicReference<>();
    private final UnirestInstance unirestInstance;
    private JsonNode responseBody;
    private boolean hasBody;
    private boolean verificacionesOK;
    private int status;
    private final Pattern interpolationPattern;

    @Inject
    public RestAction(SeleniumHelper seleniumHelper,
                      UnirestInstance unirestInstance,
                      Map<PatternEnum, Pattern> patternEnumPatternMap) {
        super(seleniumHelper);
        this.unirestInstance = unirestInstance;
        this.interpolationPattern = patternEnumPatternMap.get(PatternEnum.INTERPOLATION_PATTERN);
    }

    @Override
    public void beforeExecute() {
        super.beforeExecute();
        this.verificacionesOK = Boolean.logicalOr(this.seleniumHelper.verificacionesOk(), this.actionDto.isIgnoreverifications());
        preprocessBodyRestRequest();
    }

    private void preprocessBodyRestRequest() {
        String json = this.actionDto.getBody();
        hasBody = Objects.nonNull(json);
        if (hasBody) {
            StringBuffer sb = new StringBuffer();
            Matcher matcher = interpolationPattern.matcher(json);
            while (matcher.find()) {
                String key = matcher.group(Constantes.VALUE);
                String value = this.seleniumHelper.getValueFormContext(key);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(sb);
            String newValues = sb.toString();
            payload.set(newValues);
        }
    }

    @Override
    public void execute() {
        if (this.verificacionesOK) {

            InterpolationResult interpolation = this.getSeleniumHelper().getInterpolationOfValueIfExistsOrGetRawValue(this.getActionDto().getUrl());
            String requestEntPoint = interpolation.getValue();

            RestMethod requestMethod = this.actionDto.getMethod();
            List<SetType> requestHeaders = this.actionDto.getHeader();
            HttpRequest<?> request;

            switch (requestMethod) {
                case POST:
                    request = unirestInstance.post(requestEntPoint);
                    break;
                case PUT:
                    request = unirestInstance.put(requestEntPoint);
                    break;
                case PATCH:
                    request = unirestInstance.patch(requestEntPoint);
                    break;
                case DELETE:
                    request = unirestInstance.delete(requestEntPoint);
                    break;
                default:
                    request = unirestInstance.get(requestEntPoint);
                    break;
            }

            processHeaders(request, requestHeaders);

            HttpResponse<String> response;
            if (hasBody) {
                String requestBody = this.payload.get();
                log.info("JSON -> {}", requestBody);

                if (!(request instanceof HttpRequestWithBody requestWithBody)) {
                    throw new IllegalArgumentException("El método " + requestMethod + " no admite body");
                }
                response = requestWithBody.body(requestBody).asString();
            } else {
                response = request.asString();
            }
            status = response.getStatus();
            String body = response.getBody();
            responseBody = body == null || body.isBlank()
                    ? JSON_MAPPER.getNodeFactory().nullNode()
                    : JSON_MAPPER.readTree(body);
        }
    }

    private void processHeaders(HttpRequest<?> request, List<SetType> headers) {
        if (Objects.nonNull(headers)) {
            for (SetType setValue : headers) {
                request.header(setValue.getKey(), setValue.getValue());
            }
        }
    }

    @Override
    public void afterExecute() {
        log.info("Se termino el flujo de la accion " + this.getClass().getSimpleName() + " con la informacion con el id: " + this.actionDto.getId());
        if (this.verificacionesOK) {
            seleniumHelper.sendAction2View(this.actionDto, Color.GREEN, null);
            log.info("Status code :{}", status);
            log.info("Response payload: {}", responseBody.toPrettyString());
            this.processToStorageBody();
        } else {
            seleniumHelper.sendAction2View(this.actionDto, Color.YELLOW, null);
            log.info("Las validaciones no se cumplieron para enviar la peticion");
        }
        this.element = null;
        this.actionDto = null;
        this.responseBody = null;
        this.payload.set(null);
        this.hasBody = false;
        this.verificacionesOK = false;
        this.status = 0;
    }

    private void processToStorageBody() {
        List<SetType> storage = this.actionDto.getStorage();
        if (Objects.nonNull(storage) && !storage.isEmpty()) {
            DocumentContext json = JsonPath.using(JSON_PATH).parse(responseBody.toString());

            for (SetType setValue : storage) {
                String key = setValue.getKey();
                String path = setValue.getValue();
                Object extracted = json.read(path);
                String salida = extracted instanceof Map<?, ?> || extracted instanceof Collection<?>
                        ? JSON_MAPPER.writeValueAsString(extracted)
                        : String.valueOf(extracted);
                this.seleniumHelper.addValue2Contexto(key, salida);
            }
        }
    }

}
