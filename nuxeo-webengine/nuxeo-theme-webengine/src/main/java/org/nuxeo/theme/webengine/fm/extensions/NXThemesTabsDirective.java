/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webengine.fm.extensions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 *
 */
public class NXThemesTabsDirective implements TemplateDirectiveModel {

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        if (loopVars.length != 0) {
            throw new TemplateModelException(
                    "This directive doesn't allow loop variables.");
        }
        if (body == null) {
            throw new TemplateModelException("Expected a body");
        }

        Map<String, String> attributes = Utils.getTemplateDirectiveParameters(params);
        String identifier = attributes.get("identifier");
        String styleClass = attributes.get("styleClass");
        String controlledBy = attributes.get("controlledBy");

        // view
        final Map<String, Object> view = new HashMap<String, Object>();
        view.put("id", identifier);
        final Map<String, Object> widget = new HashMap<String, Object>();
        widget.put("type", "tabs");
        if (styleClass != null) {
            widget.put("styleClass", styleClass);
        }
        if (null != controlledBy) {
            view.put("controllers", controlledBy.split(","));
        }

        StringWriter sw = new StringWriter();
        body.render(sw);
        String content = String.format("<tabs>%s</tabs>",
                sw.getBuffer().toString());

        final List<Map<String, String>> items = new ArrayList<Map<String, String>>();

        // Parse the XML content
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            Document doc = dbf.newDocumentBuilder().parse(
                    new ByteArrayInputStream(content.getBytes()));
            NodeList itemList = doc.getElementsByTagName("tab");
            for (int i = 0; i < itemList.getLength(); i++) {
                Node itemNode = itemList.item(i);
                NamedNodeMap attrs = itemNode.getAttributes();
                Node link = attrs.getNamedItem("link");
                Node label = attrs.getNamedItem("label");
                Node switchTo = attrs.getNamedItem("switchTo");
                Map<String, String> itemMap = new HashMap<String, String>();
                if (link != null) {
                    itemMap.put("link", link.getNodeValue());
                }
                if (label != null) {
                    itemMap.put("label", label.getNodeValue());
                }
                if (switchTo != null) {
                    itemMap.put("switchTo", switchTo.getNodeValue());
                }
                items.add(itemMap);
            }
        } catch (Exception e) {
        }

        widget.put("items", items);
        view.put("widget", widget);

        Writer writer = env.getOut();
        writer.write(String.format("<ins class=\"view\">%s</ins>",
                org.nuxeo.theme.html.Utils.toJson(view)));

    }
}
