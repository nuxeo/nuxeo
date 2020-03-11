/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Benjamin JALON
 *     Thierry Delprat
 *
 */

package org.nuxeo.template.serializer.executors;

import static org.nuxeo.template.api.InputType.MapValue;
import static org.nuxeo.template.api.InputType.StringValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;

/**
 * {@link TemplateInput} parameters are stored in the {@link DocumentModel} as a single String Property via XML
 * Serialization. This class contains the Serialization/Deserialization logic.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author bjalon (bjalon@qastia.com)
 * @since 11.1
 */
public class XMLTemplateSerializer implements TemplateSerializer {

    public static final String XML_NAMESPACE = "http://www.nuxeo.org/DocumentTemplate";

    public static final String XML_NAMESPACE_PREFIX = "nxdt";

    public static final Namespace ns = new Namespace(XML_NAMESPACE_PREFIX, XML_NAMESPACE);

    public static final QName fieldsTag = DocumentFactory.getInstance().createQName("templateParams", ns);

    public static final QName fieldTag = DocumentFactory.getInstance().createQName("field", ns);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final Logger log = LogManager.getLogger(XMLTemplateSerializer.class);

    @Override
    public List<TemplateInput> deserialize(String xml) {
        try {
            Document xmlDoc = DocumentHelper.parseText(xml);
            List<Element> nodes = xmlDoc.getRootElement().elements(fieldTag);
            return nodes.stream().map(this::extractTemplateInputFromXMLNode).collect(Collectors.toList());
        } catch (DocumentException e) {
            throw new NuxeoException(e);
        }
    }

    protected TemplateInput extractTemplateInputFromXMLNode(Element node) {
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
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            try {
                paramValue = dateFormat.parse((String) paramValue);
            } catch (ParseException e) {
                throw new NuxeoException(e);
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
        return readonly != null ? Boolean.parseBoolean(readonly.getValue()) : null;
    }

    protected Boolean getIsAutoloopFromXMLNode(Element elem) {
        Attribute autoloop = elem.attribute("autoloop");
        return autoloop != null ? Boolean.parseBoolean(autoloop.getValue()) : null;
    }

    protected String getNameFromXMLNode(Element elem) {
        Attribute name = elem.attribute("name");
        return name != null ? name.getValue() : null;
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
    public String serialize(List<TemplateInput> params) {
        Element root = DocumentFactory.getInstance().createElement(fieldsTag);

        for (TemplateInput param : params) {
            Element field = root.addElement(fieldTag);
            try {
                doSerialization(field, param);
            } catch (TemplateInputBadFormat e) {
                log.error("Can't Serialize the following param: {}", param);
                root.remove(field);
            }
        }
        return root.asXML();
    }

    protected void doSerialization(Element field, TemplateInput param) {

        field.addAttribute("name", param.getName());

        InputType type = param.getType();

        if (type == null) {
            if (param.getStringValue() == null) {
                log.warn("Null param: {}", param::getName);
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
            break;
        case MapValue:
        case ListValue:
            Map<String, TemplateInput> map = param.getMapValue();
            for (String childParamName : map.keySet()) {
                TemplateInput childParam = map.get(childParamName);
                if (MapValue.equals(type) && !childParamName.equals(childParam.getName())) {
                    log.warn("Child param in map and child param name doesn't match, get child param name as key: {}",
                            childParam);
                }
                Element subfield = field.addElement(fieldTag);
                doSerialization(subfield, childParam);
            }
            break;
        case DocumentProperty:
        case PictureProperty:
        case Content:
            field.addAttribute("source", param.getSource());
            break;
        }
    }

    protected static class TemplateInputBadFormat extends NuxeoException {
    }
}
