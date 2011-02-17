/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.doc.bonita;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;

/**
 * Exporter for the XML part of a Bonita connector
 *
 * @see BonitaExporter
 * @since 5.4.1
 */
public class BonitaXMLExporter {

    protected final BonitaOperationDocumentation bonitaOperation;

    protected final OperationDocumentation operation;

    public BonitaXMLExporter(BonitaOperationDocumentation bonitaOperation) {
        super();
        this.bonitaOperation = bonitaOperation;
        this.operation = bonitaOperation.getOperation();
    }

    public String run() throws IOException, UnsupportedEncodingException {
        Document xml = DocumentHelper.createDocument();
        Element connector = xml.addElement("connector");
        connector.addElement("connectorId").setText(
                bonitaOperation.getConnectorId(operation.getId()));
        connector.addElement("version").setText("5.0");
        connector.addElement("icon").setText("avatar_nuxeo.png");
        Element cats = connector.addElement("categories");
        Element cat = cats.addElement("category");
        cat.addElement("name").setText("Nuxeo");
        cat.addElement("icon").setText(
                "org/bonitasoft/connectors/nuxeo/avatar_nuxeo.png");

        Element inputs = connector.addElement("inputs");
        Element outputs = connector.addElement("outputs");
        Element pages = connector.addElement("pages");

        int setterNumberOffset = addLoginParams(inputs, pages);
        addOperationParams(inputs, pages, setterNumberOffset);
        addOutputs(outputs);

        // write the file
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter fw = new OutputStreamWriter(out,
                BonitaExportConstants.ENCODING);
        try {
            // OutputFormat.createPrettyPrint() cannot be used since it is
            // removing new lines in text
            OutputFormat format = new OutputFormat();
            format.setIndentSize(2);
            format.setNewlines(true);
            XMLWriter writer = new XMLWriter(fw, format);
            writer.write(xml);
        } finally {
            fw.close();
        }
        return out.toString();
    }

    protected int addLoginParams(Element inputsEl, Element pagesEl) {
        Element page = pagesEl.addElement("page");
        page.addElement("pageId").setText("login");
        Element widgetsEl = page.addElement("widgets");

        // setters
        // user name
        int setterNumber = 1;
        addInput(inputsEl, widgetsEl,
                BonitaExportConstants.NUXEO_LOGIN_USERNAME, true, "string",
                "text", setterNumber);
        setterNumber++;
        // password
        addInput(inputsEl, widgetsEl,
                BonitaExportConstants.NUXEO_LOGIN_USERPASSWORD, true, "string",
                "password", setterNumber);
        setterNumber++;
        // url
        addInput(inputsEl, widgetsEl,
                BonitaExportConstants.NUXEO_AUTOMATION_URL, true, "string",
                "text", setterNumber);
        setterNumber++;
        return setterNumber;
    }

    protected void addOperationParams(Element inputsEl, Element pagesEl,
            int setterNumberOffset) {
        Element page = pagesEl.addElement("page");
        page.addElement("pageId").setText("operationParams");
        Element widgetsEl = page.addElement("widgets");
        int setterNumber = setterNumberOffset;

        // add nuxeo operation input
        String input = bonitaOperation.getOperationInput();
        if (!"void".equals(input)) {
            // FIXME: only treat document for now
            if ("document".equals(input)) {
                addInput(inputsEl, widgetsEl,
                        BonitaExportConstants.NUXEO_AUTOMATION_DOCUMENT, true,
                        "string", "text", setterNumber);
            }
            setterNumber++;
        }

        // add nuxeo operation params
        List<Param> params = operation.getParams();
        // TODO: sort params by order for display (see param.getOrder())
        if (params != null) {
            for (Param param : params) {
                // TODO: use widget type instead of param type when defined
                // param.getWidget()
                String type = param.getType();
                // TODO: generate better wiggets
                String bonitaType = BonitaOperationDocumentation.TYPES_TO_BONITA_TYPES.get(type);
                if (bonitaType == null) {
                    bonitaType = "string";
                }
                String bonitaWidget = BonitaOperationDocumentation.TYPES_TO_BONITA_WIDGETS.get(type);
                if (bonitaWidget == null) {
                    bonitaWidget = "text";
                }
                addInput(inputsEl, widgetsEl, param.getName(),
                        param.isRequired(), bonitaType, bonitaWidget,
                        setterNumber);
                setterNumber++;
            }
        }

    }

    protected void addInput(Element inputsEl, Element widgetsEl,
            String fieldId, boolean required, String paramType,
            String widgetType, int setterNumber) {
        Element setter = inputsEl.addElement("setter");
        setter.addElement("setterName").setText(
                bonitaOperation.getSetterName(fieldId));
        if (required) {
            setter.addElement("required");
        }
        setter.addElement("parameters").addElement(paramType);
        addWidget(widgetsEl, fieldId, widgetType, setterNumber);
    }

    protected void addWidget(Element widgetsEl, String labelId,
            String widgetId, int setterNumber) {
        Element widget = widgetsEl.addElement(widgetId);
        widget.addElement("labelId").setText(labelId);
        String reference = "/connector/inputs/setter";
        if (setterNumber > 1) {
            reference += "[" + setterNumber + "]";
        }
        widget.addElement("setter").addAttribute("reference", reference);
        if ("text".equals(widgetId) || "password".equals(widgetId)) {
            widget.addElement("size").setText("20");
        }
        if ("list".equals(widgetId)) {
            widget.addElement("maxRows").setText("0");
        }
    }

    protected void addOutputs(Element outputsEl) {
        String output = bonitaOperation.getOperationOutput();
        if (!"void".equals(output)) {
            Element getter = outputsEl.addElement("getter");
            getter.addElement("name").setText(
                    BonitaExportConstants.NUXEO_AUTOMATION_RESULT);
            getter.addElement("metadata");
        }
    }

}
