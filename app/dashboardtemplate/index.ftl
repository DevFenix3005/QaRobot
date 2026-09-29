<#macro accordionbutton kindOfButton icon verificacion_index verificacion_desc  >
    <button class="accordion-button bg-${kindOfButton}" type="button" data-bs-toggle="collapse"
            data-bs-target="#panel-collapse-${verificacion_index}" aria-expanded="false"
            aria-controls="panel-collapse-${verificacion_index}">
        ${verificacion_index}.- ${verificacion_desc}&nbsp;&nbsp;&nbsp;
        <i class="fas fa-${icon}"></i>
    </button>
</#macro>

<#macro failureVerificationRows verifications>
    <#list verifications as verification>
        <#if (verification.verificadorList)?has_content>
            <@failureVerificationRows verifications=verification.verificadorList />
        <#else>
            <tr class="${verification.ok?then('table-success', 'table-danger')}">
                <td>${verification.rule!"Sin regla"}</td>
                <td class="text-break">${verification.evaluado!"No disponible"}</td>
                <td class="text-break">${verification.resultado!"No disponible"}</td>
                <td>${verification.resultadoEvaluacion!"No disponible"}</td>
            </tr>
        </#if>
    </#list>
</#macro>


<#macro tableGenerator verificador_ok verificador_id verificador_rule verificador_evaluado verificador_resultado verificador_resultadoEvaluacion verificador_verificadorList=[]>
    <#if verificador_ok>
        <#assign tableSufixClass="success">
    <#else>
        <#assign tableSufixClass="danger">
        <p style="color: red">Referencia en XML: ${verificador_id}</p>
    </#if>
    <table class="table table-${tableSufixClass} table-striped">
        <thead>
        <tr>
            <th>Regla</th>
            <th>Valor Evaluado</th>
            <th>Valor de comparacion</th>
            <th>Resultado de la evaluacion</th>
        </tr>
        </thead>
        <tbody>
        <#if verificador_verificadorList?has_content>
            <#list verificador_verificadorList as verificadorChild>
                <tr>
                    <td>${verificadorChild.rule}</td>
                    <td>${verificadorChild.evaluado}</td>
                    <td>${verificadorChild.resultado}</td>
                    <td>${verificadorChild.resultadoEvaluacion}</td>
                </tr>
            </#list>
        <#else>
            <tr>
                <td>${verificador_rule}</td>
                <td>${verificador_evaluado}</td>
                <td>${verificador_resultado}</td>
                <td>${verificador_resultadoEvaluacion}</td>
            </tr>
        </#if>
        </tbody>
    </table>
</#macro>

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        <#include "main.css" parse=false encoding="UTF-8"/>
    </style>
    <title>QaRobot - Dashboard</title>
</head>
<body class="d-flex flex-column vh-100">
<header>
    <nav class="navbar navbar-dark bg-dark shadow-sm">
        <div class="container">
            <a id="logo-img-content" class="navbar-brand d-flex align-items-center">
            </a>
        </div>
    </nav>
</header>

<main class="flex-shrink-0">
    <div class="container mt-4">
        <div class="d-flex align-items-start">
            <div class="nav flex-column nav-pills me-3" id="v-pills-tab" role="tablist" aria-orientation="vertical">
                <button class="nav-link active" id="pills-resumen-tab" data-bs-toggle="pill"
                        data-bs-target="#pills-resumen"
                        type="button" role="tab" aria-controls="pills-resumen" aria-selected="true">Resumen
                </button>
                <button class="nav-link" id="pills-contexto-tab" data-bs-toggle="pill"
                        data-bs-target="#pills-contexto"
                        type="button" role="tab" aria-controls="pills-contexto" aria-selected="true">Contexto
                </button>
                <button class="nav-link" id="pills-actions-tab" data-bs-toggle="pill" data-bs-target="#pills-action"
                        type="button" role="tab" aria-controls="pills-actions" aria-selected="false">Acciones
                </button>
                <button class="nav-link" id="pills-verify-tab" data-bs-toggle="pill" data-bs-target="#pills-verify"
                        type="button" role="tab" aria-controls="pills-verify" aria-selected="false">Verificacion
                </button>
                <button class="nav-link" id="pills-xml-tab" data-bs-toggle="pill" data-bs-target="#pills-xml"
                        type="button" role="tab" aria-controls="pills-xml" aria-selected="false">Ver XML
                </button>
            </div>
            <div class="tab-content" id="pills-tabContent" style="flex: 1; min-width: 0">
                <div class="tab-pane fade show active" id="pills-resumen" role="tabpanel"
                     aria-labelledby="pills-resumen-tab"
                     tabindex="0">
                    <h5>Acciones</h5>
                    <hr/>
                    <div class="row row-cols-1 row-cols-md-4 g-4 mb-5">
                        <div class="col">
                            <div class="card">
                                <div class="card-header">Acciones Realizadas</div>
                                <div class="card-body">
                                    <span class="info-circle">${counterAcciones}</span>
                                </div>
                            </div>
                        </div>
                        <div class="col">
                            <div class="card">
                                <div class="card-header">Pruebas Satisfactorias</div>
                                <div class="card-body">
                                    <span class="success-circle">${counterPruebasOk}</span>
                                </div>
                            </div>
                        </div>
                        <div class="col">
                            <div class="card">
                                <div class="card-header">Pruebas Erroneas</div>
                                <div class="card-body">
                                    <span class="error-circle">${counterPruebasErr}</span>
                                </div>
                            </div>
                        </div>
                        <div class="col">
                            <div class="card">
                                <div class="card-header">Pruebas Omitidas</div>
                                <div class="card-body">
                                    <span class="skip-circle">${counterPruebasSkip}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <section class="mb-5" aria-labelledby="failure-evidence-title">
                        <#assign failures=failureEvidence![]>
                        <h5 id="failure-evidence-title">Diagn&oacute;stico de fallos
                            <span class="badge bg-${failures?has_content?then('danger', 'secondary')}">${failures?size}</span>
                        </h5>
                        <#list failures as failure>
                            <article class="card border-danger mt-3">
                                <div class="card-header bg-danger text-white">
                                    <strong>Paso con fallo: ${failure.description!"Sin descripcion"}</strong>
                                </div>
                                <div class="card-body">
                                    <dl class="row mb-3">
                                        <dt class="col-sm-3">Identificador del paso</dt>
                                        <dd class="col-sm-9 text-break">${failure.actionId!"No disponible"}</dd>
                                        <dt class="col-sm-3">Tipo de acci&oacute;n</dt>
                                        <dd class="col-sm-9 text-break">${failure.actionType!"No disponible"}</dd>
                                        <dt class="col-sm-3">Momento del fallo</dt>
                                        <dd class="col-sm-9">${failure.capturedAt!"No disponible"}</dd>
                                        <dt class="col-sm-3">URL del navegador</dt>
                                        <dd class="col-sm-9 text-break">${failure.url!"No disponible"}</dd>
                                        <dt class="col-sm-3">Selector utilizado</dt>
                                        <dd class="col-sm-9 text-break"><code>${failure.selectorUsed!"No se selecciono un elemento"}</code></dd>
                                        <#if (failure.selectors)?has_content>
                                            <dt class="col-sm-3">Selectores configurados</dt>
                                            <dd class="col-sm-9">
                                                <ul class="mb-0 ps-3">
                                                    <#list failure.selectors as selector>
                                                        <li class="text-break"><code>${selector}</code></li>
                                                    </#list>
                                                </ul>
                                            </dd>
                                        </#if>
                                    </dl>
                                    <div class="alert alert-danger">
                                        <h6>Error original: ${failure.exceptionType!"Fallo de verificacion"}</h6>
                                        <pre class="mb-0 text-break" style="white-space: pre-wrap">${failure.message!"Sin mensaje adicional"}</pre>
                                    </div>
                                    <#if (failure.verifications)?has_content>
                                        <h6>Detalle de las verificaciones</h6>
                                        <p class="small text-muted">En comparaciones de contenido, el valor evaluado es el obtenido y el valor de comparaci&oacute;n es el esperado.</p>
                                        <div class="table-responsive">
                                            <table class="table table-bordered table-sm">
                                                <thead>
                                                <tr>
                                                    <th scope="col">Regla</th>
                                                    <th scope="col">Valor evaluado</th>
                                                    <th scope="col">Valor de comparaci&oacute;n</th>
                                                    <th scope="col">Resultado</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                    <@failureVerificationRows verifications=failure.verifications />
                                                </tbody>
                                            </table>
                                        </div>
                                    </#if>
                                    <#if (failure.screenshotPath)?has_content>
                                        <figure class="mt-3">
                                            <figcaption class="fw-bold mb-2">Captura del navegador al fallar</figcaption>
                                            <img class="img-fluid border rounded" src="${failure.screenshotPath}"
                                                 alt="Captura del paso con fallo: ${failure.actionId!''}" loading="lazy">
                                        </figure>
                                        <a class="btn btn-outline-primary btn-sm me-2 mb-2" href="${failure.screenshotPath}" download>Descargar captura</a>
                                    <#else>
                                        <p class="text-muted">No hay captura del navegador disponible para este fallo.</p>
                                    </#if>
                                    <#if (failure.detailsPath)?has_content>
                                        <a class="btn btn-outline-secondary btn-sm mb-2" href="${failure.detailsPath}" download>Descargar diagn&oacute;stico</a>
                                    </#if>
                                    <#if (failure.captureProblems)?has_content>
                                        <div class="alert alert-warning mt-3 mb-0">
                                            <h6>Limitaciones al recopilar evidencia</h6>
                                            <ul class="mb-0">
                                                <#list failure.captureProblems as problem>
                                                    <li class="text-break">${problem}</li>
                                                </#list>
                                            </ul>
                                        </div>
                                    </#if>
                                </div>
                            </article>
                        <#else>
                            <p class="text-muted">No se registraron fallos durante la ejecuci&oacute;n.</p>
                        </#list>
                    </section>
                </div>
                <div class="tab-pane fade" id="pills-contexto" role="tabpanel" aria-labelledby="pills-contexto-tab"
                     tabindex="0">
                    <h5>Contexto</h5>
                    <hr/>
                    <table class="table">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">Llave</th>
                            <th scope="col">Valor</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list myConfigvalues as setval>
                            <#if setval.key = "ERROR">
                                <tr class="table-danger">
                            <#else>
                                <tr>
                            </#if>
                            <th scope="row">${setval?counter}</th>
                            <td>${setval.key}</td>
                            <td>${setval.value}</td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </div>
                <div class="tab-pane fade" id="pills-action" role="tabpanel" aria-labelledby="pills-action-tab"
                     tabindex="0">
                    <h5>Acciones Ejecutadas</h5>
                    <hr/>
                    <table class="table">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th>Identificador</th>
                            <th>Tipo de accion</th>
                            <th>Descripcion</th>
                            <th>Tiempo de espera</th>
                            <th>Orden</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list actionsdto as actiondto>
                            <#if actiondto.skip>
                                <#assign klass="OMITIDA[${actiondto.class.simpleName}]">
                                <tr class="table-warning">
                            <#else>
                                <#assign klass="${actiondto.class.simpleName}">
                                <tr>
                            </#if>
                            <td>${actiondto?counter}</td>
                            <td>${actiondto.id}</td>
                            <td>${klass}</td>
                            <td>${actiondto.desc}</td>
                            <td>
                                <#if actiondto.timeout??>
                                    ${actiondto.timeout}
                                <#else>
                                    0
                                </#if>
                            </td>
                            <td>${actiondto.order}</td>
                            </tr>
                        <#else>
                            <h2>No hay Acciones Ejecutadas</h2>
                        </#list>
                        </tbody>
                    </table>

                </div>
                <div class="tab-pane fade" id="pills-verify" role="tabpanel" aria-labelledby="pills-verify-tab"
                     tabindex="0">
                    <h5>Verificaciones</h5>
                    <hr/>
                    <div class="accordion" id="accordionPanelsStayOpenExample">
                        <#list verificadors as verificador>
                            <div class="accordion-item">
                                <h2 class="accordion-header" id="heading-${verificador?counter}">
                                    <#if verificador.ok>
                                        <@accordionbutton kindOfButton="success" icon="check"        verificacion_index=verificador?counter verificacion_desc=verificador.descVerifyAccion />
                                    <#elseif verificador.skip>
                                        <@accordionbutton kindOfButton="warning" icon="minus-circle" verificacion_index=verificador?counter verificacion_desc=verificador.descVerifyAccion />
                                    <#else>
                                        <@accordionbutton kindOfButton="danger" icon="bug"           verificacion_index=verificador?counter verificacion_desc=verificador.descVerifyAccion />
                                    </#if>
                                </h2>
                                <div id="panel-collapse-${verificador?counter}"
                                     class="accordion-collapse collapse"
                                     aria-labelledby="heading-${verificador?counter}">
                                    <div class="accordion-body">
                                        <div class="row">
                                            <div class="col">
                                                <@tableGenerator verificador_ok=verificador.ok
                                                verificador_id=verificador.id
                                                verificador_rule=verificador.rule
                                                verificador_evaluado=verificador.evaluado
                                                verificador_resultado=verificador.resultado
                                                verificador_resultadoEvaluacion=verificador.resultadoEvaluacion
                                                verificador_verificadorList=verificador.verificadorList />
                                            </div>

                                        </div>
                                        <div class="row">
                                <pre>
                                    <#if verificador.script??>
                                        <code class="language-javascript">${verificador.script}</code>
                                    </#if>
                                </pre>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        <#else>
                            <h1>No se agregaron verificaciones</h1>
                        </#list>
                    </div>
                </div>
                <div class="tab-pane fade" id="pills-xml" role="tabpanel" aria-labelledby="pills-xml-tab"
                     tabindex="0">
                    <h5>Vista del XML</h5>
                    <hr/>
                    <pre>
                        <code id="xml-content" class="language-html">${execxml}</code>
                    </pre>
                </div>
            </div>
        </div>
    </div>
</main>

<footer class="footer mt-auto py-3 bg-dark">
    <div class="container">
        <p class="float-end mb-1">
            <a href="#">Back to top</a>
        </p>
        <p class="text-muted">QaRobot &copy; ReBIRTH</p>
    </div>
</footer>


<script>
    <#include "main.js" parse=false encoding="UTF-8"/>
</script>
</body>
</html>
