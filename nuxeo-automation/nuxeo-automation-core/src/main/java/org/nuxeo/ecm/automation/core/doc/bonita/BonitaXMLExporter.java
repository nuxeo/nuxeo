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

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.automation.OperationDocumentation;

/**
 * Exporter for the XML part of a Bonita connector
 *
 * @since 5.4.1
 */
public class BonitaXMLExporter {

    protected final BonitaExportConfiguration configuration;

    protected final OperationDocumentation operation;

    public BonitaXMLExporter(BonitaExportConfiguration configuration,
            OperationDocumentation operation) {
        super();
        this.configuration = configuration;
        this.operation = operation;
    }

    public String run() throws IOException, UnsupportedEncodingException {
        Document xml = DocumentHelper.createDocument();
        Element connector = xml.addElement("connector");
        connector.addElement("connectorId").setText(
                configuration.getConnectorId(operation.getId()));
        connector.addElement("version").setText("5.0");
        connector.addElement("icon").setText("avatar_nuxeo.png");
        Element cats = connector.addElement("categories");
        Element cat = cats.addElement("category");
        cat.addElement("name").setText("Nuxeo");
        cat.addElement("icon").setText(
                "org/bonitasoft/connectors/nuxeo/avatar_nuxeo.png");

        Element inputs = connector.addElement("inputs");

        // write the file
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter fw = new OutputStreamWriter(out,
                BonitaExportConfiguration.ENCODING);
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

    protected void addLoginInputs(Element inputsEl) {

    }

    protected void addInputSetter(Element inputsEl, String fieldName) {

    }

    protected void addLoginPage(Element pagesEl) {

    }

}
