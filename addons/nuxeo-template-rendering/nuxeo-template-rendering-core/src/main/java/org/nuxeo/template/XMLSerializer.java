/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultElement;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;

/**
 * {@link TemplateInput} parameters are stored in the {@link DocumentModel} as a single String Property via XML
 * Serialization. This class contains the Serialization/Deserialization logic.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class XMLSerializer {

    protected static final Log log = LogFactory.getLog(XMLSerializer.class);

    public static final String XML_NAMESPACE = "http://www.nuxeo.org/DocumentTemplate";

    public static final String XML_NAMESPACE_PREFIX = "nxdt";

    public static final Namespace ns = new Namespace(XML_NAMESPACE_PREFIX, XML_NAMESPACE);

    public static final QName fieldsTag = DocumentFactory.getInstance().createQName("templateParams", ns);

    public static final QName fieldTag = DocumentFactory.getInstance().createQName("field", ns);

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static String serialize(List<TemplateInput> params) {

        Element root = DocumentFactory.getInstance().createElement(fieldsTag);

        for (TemplateInput input : params) {

            Element field = root.addElement(fieldTag);

            field.addAttribute("name", input.getName());

            InputType type = input.getType();
            if (type == null) {
                log.warn(input.getName() + " is null");
            }
            field.addAttribute("type", type.getValue());

            if (input.isReadOnly()) {
                field.addAttribute("readonly", "true");
            }

            if (input.isAutoLoop()) {
                field.addAttribute("autoloop", "true");
            }

            if (InputType.StringValue.equals(type)) {
                field.addAttribute("value", input.getStringValue());
            } else if (InputType.DateValue.equals(type)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                field.addAttribute("value", dateFormat.format(input.getDateValue()));
            } else if (InputType.BooleanValue.equals(type)) {
                field.addAttribute("value", input.getBooleanValue().toString());
            } else {
                field.addAttribute("source", input.getSource());
            }

            if (input.getDesciption() != null) {
                field.setText(input.getDesciption());
            }
        }
        return root.asXML();
    }

    public static List<TemplateInput> readFromXml(String xml) throws DocumentException {

        List<TemplateInput> result = new ArrayList<TemplateInput>();

        Document xmlDoc = DocumentHelper.parseText(xml);

        @SuppressWarnings("rawtypes")
        List nodes = xmlDoc.getRootElement().elements(fieldTag);

        for (Object node : nodes) {

            DefaultElement elem = (DefaultElement) node;
            Attribute name = elem.attribute("name");
            TemplateInput param = new TemplateInput(name.getValue());

            InputType type = InputType.StringValue;

            if (elem.attribute("type") != null) {
                type = InputType.getByValue(elem.attribute("type").getValue());
                param.setType(type);
            }

            String strValue = elem.attributeValue("value");
            if (InputType.StringValue.equals(type)) {
                param.setStringValue(strValue);
            } else if (InputType.DateValue.equals(type)) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                    param.setDateValue(dateFormat.parse(strValue));
                } catch (ParseException e) {
                    throw new DocumentException(e);
                }
            } else if (InputType.BooleanValue.equals(type)) {
                param.setBooleanValue(new Boolean(strValue));
            } else {
                param.setSource(elem.attributeValue("source"));
            }

            if (elem.attribute("readonly") != null) {
                param.setReadOnly(Boolean.parseBoolean(elem.attributeValue("readonly")));
            }

            if (elem.attribute("autoloop") != null) {
                param.setAutoLoop(Boolean.parseBoolean(elem.attributeValue("autoloop")));
            }

            param.setDesciption(elem.getText());

            result.add(param);
        }

        return result;
    }

}
