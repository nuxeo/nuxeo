package org.nuxeo.template.serializer.executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.*;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.nuxeo.template.api.InputType.StringValue;

/**
 * {@link TemplateInput} parameters are stored in the {@link DocumentModel} as a single String Property via XML
 * Serialization. This class contains the Serialization/Deserialization logic.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author bjalon (bjalon@qastia.com)
 *
 * @Since 11.1
 */
public class XMLSerializer implements Serializer {

    protected static final Log log = LogFactory.getLog(XMLSerializer.class);

    public static final String XML_NAMESPACE = "http://www.nuxeo.org/DocumentTemplate";

    public static final String XML_NAMESPACE_PREFIX = "nxdt";

    public static final Namespace ns = new Namespace(XML_NAMESPACE_PREFIX, XML_NAMESPACE);

    public static final QName fieldsTag = DocumentFactory.getInstance().createQName("templateParams", ns);

    public static final QName fieldTag = DocumentFactory.getInstance().createQName("field", ns);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    @Override
    public List<TemplateInput> doDeserialization(String xml) throws DocumentException {
        List<TemplateInput> result = new ArrayList<>();

        Document xmlDoc = DocumentHelper.parseText(xml);

        List<Element> nodes = xmlDoc.getRootElement().elements(fieldTag);

        for (Element node : nodes) {
            result.add(extractTemplateInputFromXMLNode(node));
        }

        return result;
    }

    protected TemplateInput extractTemplateInputFromXMLNode(Element node) throws DocumentException {
        String paramName = getNameFromXMLNode(node);
        InputType paramType = getTypeFromXMLNode(node);
        String paramDesc = node.getText();
        Boolean isReadonly = getIsReadonlyFromXMLNode(node);
        Boolean isAutoloop = getIsAutoloopFromXMLNode(node);

        Object paramValue = node.attributeValue("value");
        switch (paramType) {
            case StringValue:
            case BooleanValue:
                break;
            case DateValue:
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                    paramValue = dateFormat.parse((String) paramValue);
                } catch (ParseException e) {
                    throw new DocumentException(e);
                }
                break;
            case MapValue:
            case ListValue:
                Map<String, TemplateInput> listValue = new LinkedHashMap<>();
                for (Element childNode : node.elements()) {
                    TemplateInput childParam = extractTemplateInputFromXMLNode(childNode);
                    if (childNode != null) {
                        listValue.put(childParam.getName(), childParam);
                    }
                }
                paramValue = listValue;
                break;
            default:
                paramValue = node.attributeValue("source");
        }
        return TemplateInput.factory(paramName, paramType, paramValue, paramDesc, isReadonly, isAutoloop);
    }


    protected Boolean getIsReadonlyFromXMLNode(Element elem) {
        Attribute readonly = elem.attribute("readonly");
        return readonly != null ?
                Boolean.parseBoolean(readonly.getValue()) :
                null;
    }

    protected Boolean getIsAutoloopFromXMLNode(Element elem) {
        Attribute autoloop = elem.attribute("autoloop");
        return autoloop != null ?
                Boolean.parseBoolean(autoloop.getValue()) :
                null;
    }

    protected String getNameFromXMLNode(Element elem) {
        Attribute name = elem.attribute("name");
        return name != null ?
                name.getValue() :
                null;
    }

    protected InputType getTypeFromXMLNode(Element elem) {
        InputType type = null;
        Attribute typeAtt = elem.attribute("type");
        if (typeAtt != null) {
            type = InputType.getByValue(typeAtt.getValue());
        }

        return type == null ? StringValue : type;
    }

    @Override
    public String doSerialization(List<TemplateInput> params) {
        Element root = DocumentFactory.getInstance().createElement(fieldsTag);

        for (TemplateInput param : params) {
            Element field = root.addElement(fieldTag);
            try {
                doSerialization(field, param);
            } catch (TemplateInputBadFormat e) {
                log.error("Can't Serialize the following param: " + param);
                root.remove(field);
            }
        }
        return root.asXML();
    }

    protected void doSerialization(Element field, TemplateInput param) throws TemplateInputBadFormat {

        field.addAttribute("name", param.getName());

        InputType type = param.getType();

        if (type == null) {
            if (param.getStringValue() == null) {
                log.warn(param.getName() + " is null");
                throw new TemplateInputBadFormat();
            } else
                type = StringValue;
        }

        field.addAttribute("type", type.getValue());
        if (param.isReadOnly()) {
            field.addAttribute("readonly", "true");
        }
        if (param.isAutoLoop()) {
            field.addAttribute("autoloop", "true");
        }
        if (param.getDesciption() != null) {
            field.setText(param.getDesciption());
        }

        switch (type) {
            case StringValue:
                field.addAttribute("value", param.getStringValue());
                break;
            case DateValue:
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                field.addAttribute("value", dateFormat.format(param.getDateValue()));
                break;
            case BooleanValue:
                field.addAttribute("value", param.getBooleanValue().toString());
            case DocumentProperty:
            case PictureProperty:
            case Content:
                field.addAttribute("source", param.getSource());
        }
    }

    private class TemplateInputBadFormat extends Throwable {
    }
}
