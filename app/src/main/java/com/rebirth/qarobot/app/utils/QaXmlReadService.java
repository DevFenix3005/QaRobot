package com.rebirth.qarobot.app.utils;

import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.thedeanda.lorem.Lorem;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.exceptions.NotFoundQaXmlFile;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.qarobot.Qarobot;

@Data
@Singleton
@Log4j2
public class QaXmlReadService {

    public static final int Q_OF_WORDS = 6;
    private static final String INCLUDE_TAG = "include";
    private static final String CYCLE_TAG = "cycle";
    private final DocumentBuilder documentBuilder;
    private final Lorem lorem;
    private final Random random;
    private final Configuracion configuracion;
    private final Pattern randomStringPattern;
    private final Pattern randomIntegerPattern;
    private final Comparator<Node> nodeComparator = new NodeOrderComparator();
    private File qaXmlFile;
    private AtomicInteger counter;

    @Inject
    public QaXmlReadService(DocumentBuilder documentBuilder,
                            Configuracion configuracion,
                            Lorem lorem,
                            Random random,
                            Map<PatternEnum, Pattern> patternEnumPatternMap) {
        this.configuracion = configuracion;
        this.documentBuilder = documentBuilder;
        this.lorem = lorem;
        this.random = random;
        this.randomStringPattern = patternEnumPatternMap.get(PatternEnum.STRING_PATTERN);
        this.randomIntegerPattern = patternEnumPatternMap.get(PatternEnum.INTEGER_PATTERN);
    }

    public QarobotWrapper read() throws IOException, TransformerException, NotFoundQaXmlFile, JAXBException {
        this.counter = new AtomicInteger(1);
        QarobotWrapper qarobotWrapper = new QarobotWrapper();

        Document newDocument = documentBuilder.newDocument();
        Element qarobotElement = newDocument.createElementNS("https://www.qarobot.rebirth.com.mx", "qarobot");
        searchAllInclude(qarobotElement, this.qaXmlFile, true);
        newDocument.appendChild(qarobotElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(newDocument), new StreamResult(writer));
        String xml = writer.toString();
        xml = randomIntegerProcessor(xml);
        xml = randomStringProcessor(xml);

        File tempXml = File.createTempFile("QaBot3005_", ".xml", new File(Objects.requireNonNull(StandardSystemProperty.JAVA_IO_TMPDIR.value())));
        Files.asByteSink(tempXml)
                .write(xml.getBytes(StandardCharsets.ISO_8859_1));

        xsdValidation(tempXml, qarobotWrapper);
        if (qarobotWrapper.isValidXml()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Qarobot.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Qarobot qarobot = (Qarobot) unmarshaller.unmarshal(tempXml);
            qarobotWrapper.setQarobot(qarobot);
        }

        qarobotWrapper.setXmlFile(this.qaXmlFile);
        qarobotWrapper.setXmlTempFile(tempXml);
        qarobotWrapper.setDashboardsFolder(this.configuracion.getDashboardsOutputs());
        return qarobotWrapper;
    }

    private void xsdValidation(File xmlFile, QarobotWrapper qarobotWrapper) {
        try {
            qarobotWrapper.setValidXml(true);
            URL xsdFile = Resources.getResource("com/rebirth/qarobot/commons/xsd/qarobot_v2.xsd");
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Schema schema = factory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new XsdValidationErrorHandler(qarobotWrapper));
            validator.validate(new StreamSource(xmlFile));
        } catch (IOException e) {
            log.error("No se encontro el XSD en el path asignado", e);
            qarobotWrapper.setValidXml(false);
        } catch (SAXException e) {
            log.error("Error en la validacion del XML", e);
            qarobotWrapper.setValidXml(false);
        }
    }

    private void searchAllInclude(Element rootElement, File file, boolean isTheParent) throws NotFoundQaXmlFile {

        StringBuilder desc = new StringBuilder();
        try {
            InputStream bufferedInputStream = Files.asByteSource(file).openBufferedStream();
            Document originDocument = documentBuilder.parse(bufferedInputStream);
            Element origenElement = originDocument.getDocumentElement();
            origenElement.normalize();

            List<Node> nodeList = getChildsNodeInSortedList(origenElement);

            for (Node node : nodeList) {
                NamedNodeMap attrs = node.getAttributes();

                boolean skip = returnNoNullValue(attrs, "skip", false, Boolean::parseBoolean);
                desc.append(returnNoNullValue(attrs, "desc", "unk", Function.identity()));

                String nodeName = node.getNodeName();
                if (nodeName.equals(INCLUDE_TAG)) {
                    if (!skip) includeProcess(rootElement, node, file);
                } else if (nodeName.equals(CYCLE_TAG)) {
                    if (!skip) cycleProcess(rootElement, node, file);
                } else {
                    normalNode(rootElement, node, isTheParent);
                }
            }
        } catch (IOException e) {
            throw new NotFoundQaXmlFile("Error en lectura", e, desc.toString(), file.getName());
        } catch (SAXException e) {
            throw new NotFoundQaXmlFile("Error la estructura del xml", e, desc.toString(), file.getName());
        }
    }

    private <R> R returnNoNullValue(NamedNodeMap namedNodeMap, String attrName, R deftvalue, Function<String, R> convertion) {
        Attr descAttr = (Attr) namedNodeMap.getNamedItem(attrName);
        return Objects.isNull(descAttr) ? deftvalue : convertion.apply(descAttr.getValue());
    }

    private List<Node> getChildsNodeInSortedList(Node origenElement) {
        NodeList childNodes = origenElement.getChildNodes();
        List<Node> nodeList = Lists.newArrayList();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                nodeList.add(node);
            }
        }
        nodeList.sort(nodeComparator);
        return nodeList;
    }


    private void includeProcess(Element rootElement, Node node, File parentFile) throws NotFoundQaXmlFile {
        NamedNodeMap attrs = node.getAttributes();
        Attr setAttr = (Attr) attrs.getNamedItem("set");
        String setAttrValue = setAttr.getValue();
        includeProcess(rootElement, setAttrValue, parentFile);
    }

    private void includeProcess(Element rootElement, String filename, File parentFile) throws NotFoundQaXmlFile {
        File includeFile = new File(parentFile.getParent(), filename + ".xml");
        UUID uuid = UUID.randomUUID();
        addXmlComment(rootElement, String.format("[%s]::Inicio leyendo el xml %s, tag incluida en el xml %s", uuid, includeFile.getName(), parentFile.getName()));
        searchAllInclude(rootElement, includeFile, false);
        addXmlComment(rootElement, String.format("[%s]::Fin terminando de leer el %s", uuid, includeFile.getName()));
    }

    private void cycleProcess(Element rootElement, Node node, File parentFile) throws NotFoundQaXmlFile {
        NamedNodeMap attrs = node.getAttributes();
        Attr timesAttr = (Attr) attrs.getNamedItem("times");
        Attr descAttr = (Attr) attrs.getNamedItem("desc");
        Attr timeoutAttr = (Attr) attrs.getNamedItem("timeout");
        Attr includeAttr = (Attr) attrs.getNamedItem(INCLUDE_TAG);

        int times = Integer.parseInt(timesAttr.getValue());
        List<Node> nodesInCyle = getChildsNodeInSortedList(node);
        UUID uuid = UUID.randomUUID();

        // <delay desc="retardo para renderizar html" timeout="5000" order="100" />

        for (int cycle = 0; cycle < times; cycle++) {
            addXmlComment(rootElement, String.format("[%s]::Inicio ciclo %d, descripcion: %s", uuid, cycle, descAttr.getValue()));
            Document parentDocument = rootElement.getOwnerDocument();
            Element delayNode = parentDocument.createElementNS("https://www.qarobot.rebirth.com.mx", "delay");
            delayNode.setAttribute("desc", "Retardo para el inicio del ciclo " + cycle);
            delayNode.setAttribute("timeout", timeoutAttr.getValue());
            delayNode.setAttribute("order", "" + counter.getAndIncrement());
            delayNode.setAttribute("id", UUID.randomUUID().toString());
            rootElement.appendChild(delayNode);

            if (Objects.nonNull(includeAttr)) {
                String includeFile = includeAttr.getValue();
                this.includeProcess(rootElement, includeFile, parentFile);
            } else {
                for (Node nodeInCycle : nodesInCyle) {
                    if (nodeInCycle.getNodeName().equals(INCLUDE_TAG)) {
                        includeProcess(rootElement, nodeInCycle, parentFile);
                    } else {
                        normalNode(rootElement, nodeInCycle, false);
                    }
                }
            }
            addXmlComment(rootElement, String.format("[%s]::Fin ciclo %d", uuid, cycle));
        }
    }

    private void normalNode(Element rootElement, Node node, boolean addConfiguration) {
        String nodeName = node.getNodeName();
        Document parentDocument = rootElement.getOwnerDocument();
        Element copyElement = (Element) parentDocument.importNode(node, true);

        boolean isConfigurationNode = nodeName.equals("configuration");
        boolean isInfoNode = nodeName.equals("info");

        if (!isConfigurationNode && !isInfoNode) {
            NamedNodeMap attributes = copyElement.getAttributes();
            Attr idAttr = (Attr) attributes.getNamedItem("id");
            if (Objects.isNull(idAttr)) {
                copyElement.setAttribute("id", UUID.randomUUID().toString());
            }
            Attr orderAttr = (Attr) attributes.getNamedItem("order");
            orderAttr.setValue("" + counter.getAndIncrement());
            attributes.setNamedItem(orderAttr);
            rootElement.appendChild(copyElement);
        }

        if (isConfigurationNode && addConfiguration) {
            rootElement.appendChild(copyElement);
        }
    }

    private void addXmlComment(Element rootElement, String mensaje) {
        Document parentDocument = rootElement.getOwnerDocument();
        Comment commentNode = parentDocument.createComment(mensaje);
        rootElement.appendChild(commentNode);
    }


    private String randomIntegerProcessor(String xml) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = randomIntegerPattern.matcher(xml);
        while (matcher.find()) {
            String randomInteger = getRandomInteger(matcher);
            matcher.appendReplacement(sb, randomInteger);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String getRandomInteger(Matcher randomIntegergMatcher) {
        int randValue;
        int min = 100;
        int max = 0;
        int times = 0;

        String q1 = randomIntegergMatcher.group("q1");
        String q2 = randomIntegergMatcher.group("q2");
        String timesStr = randomIntegergMatcher.group("timesval");

        if (q1 != null) {
            min = Integer.parseInt(q1);
        }
        if (q2 != null) {
            max = Integer.parseInt(q2);
        }
        if (timesStr != null) {
            times = Integer.parseInt(timesStr);
        }

        if (max > 0 && max > min) {
            randValue = randomByRange(min, max);
        } else {
            randValue = random.nextInt(min);
        }

        if (times > 0) {
            randValue *= times;
        }
        return Integer.toString(randValue);
    }

    private int randomByRange(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    private String randomStringProcessor(String xml) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = randomStringPattern.matcher(xml);
        while (matcher.find()) {
            String randomInteger = getRandomString(matcher);
            matcher.appendReplacement(sb, randomInteger);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String getRandomString(Matcher randomStringMatcher) {
        String cantidad = randomStringMatcher.group("quantity");
        if (cantidad == null) {
            return lorem.getWords(Q_OF_WORDS);
        } else if (cantidad.equals("NAME")) {
            return lorem.getFirstName() + " " + "ROBOT";
        } else {
            return lorem.getWords(Integer.parseInt(cantidad));
        }
    }


}
