/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
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
 * {@link TemplateInput} parameters are stored in the {@link DocumentModel} as a
 * single String Property via XML Serialization. This class contains the
 * Serialization/Deserialization logic.
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
 */
public class XMLSerializer {

    protected static final Log log = LogFactory.getLog(XMLSerializer.class);

    public static final String XML_NAMESPACE = "http://www.nuxeo.org/DocumentTemplate";

    public static final String XML_NAMESPACE_PREFIX = "nxdt";

    public static final Namespace ns = new Namespace(XML_NAMESPACE_PREFIX,
            XML_NAMESPACE);

    public static final QName fieldsTag = DocumentFactory.getInstance().createQName(
            "templateParams", ns);

    public static final QName fieldTag = DocumentFactory.getInstance().createQName(
            "field", ns);

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd hh:mm:ss:sss");

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
                field.addAttribute("value",
                        dateFormat.format(input.getDateValue()));
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

    public static List<TemplateInput> readFromXml(String xml) throws Exception {

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
                param.setDateValue(dateFormat.parse(strValue));
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
