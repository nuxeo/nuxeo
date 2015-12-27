/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointerFactory;

import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Alexandre Russel
 */
public abstract class AbstractDocumentGWTTest extends GWTTestCase {
    public void gwtSetUp() {
        com.google.gwt.user.client.Element bodyElem = RootPanel.getBodyElement();

        List<com.google.gwt.user.client.Element> toRemove = new ArrayList<com.google.gwt.user.client.Element>();
        for (int i = 0, n = DOM.getChildCount(bodyElem); i < n; ++i) {
            com.google.gwt.user.client.Element elem = DOM.getChild(bodyElem, i);
            String nodeName = getNodeName(elem);
            if (!"script".equals(nodeName) && !"iframe".equals(nodeName)) {
                toRemove.add(elem);
            }
        }

        for (int i = 0, n = toRemove.size(); i < n; ++i) {
            DOM.removeChild(bodyElem, toRemove.get(i));
        }
    }

    public Annotation getDefaultAnnotation() {
        AnnotationDefinition def = WebConfiguration.DEFAULT_WEB_CONFIGURATION.getAnnotationDefinitions().get(0);
        Annotation annotation = new Annotation();
        annotation.setType(def.getUri());
        return annotation;
    }

    public void createDocument() {
        BodyElement body = RootPanel.getBodyElement().cast();
        DivElement div = DivElement.as(DOM.createDiv());
        div.setId("insidediv");
        div.setInnerHTML(getInnerHtml());
        body.appendChild(div);
    }

    public String getInnerHtml() {
        return INNER_HTML;
    }

    @Override
    public String getModuleName() {
        return "org.nuxeo.ecm.platform.annotations.gwt.AnnotationPanel";
    }

    public List<Annotation> getAnnotations(String[] xpointers) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        int counter = 0;
        for (String pointer : xpointers) {
            Annotation annotation = getDefaultAnnotation();
            annotation.setXpointer(XPointerFactory.getXPointer("http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/#xpointer(string-range("
                    + pointer + "))"));
            annotation.setId(counter++);
            annotation.setType("Comment");
            annotations.add(annotation);
        }
        return annotations;
    }

    public static native String getNodeName(Element elem) /*-{
                                                          return (elem.nodeName || "").toLowerCase();
                                                          }-*/;

    public static String INNER_HTML = "<a name=\"1\"></a>"
            + "<div style=\"position: relative; width: 595px; height: 842px;\">"
            + "<style type=\"text/css\">"
            + "<!--"
            + "    .ft0{font-size:30px;font-family:Helvetica;color:#000000;}"
            + "    .ft1{font-size:10px;font-family:Helvetica;color:#000000;}"
            + "-->"
            + "</style>"
            + "<img src=\"_tmp_PDF2Html_1221805514793_index_files/index001.png\" alt=\"background image\" width=\"595\" height=\"842\">"
            + "<div style=\"position: absolute; top: 290px; left: 59px;\"><nobr><span class=\"ft0\"><b>Nuxeo EP 5 - Nuxeo Annotation</b></span></nobr></div>"
            + "<div id=\"thediv\" style=\"position: absolute; top: 329px; left: 242px;\">The <nobr><span class=\"ft0\">Da <b>Service</b></span></nobr> and other stuff</div>"
            + "<div style=\"position: absolute; top: 691px; left: 199px;\"><nobr><span class=\"ft1\">Copyright © 2000-2007, Nuxeo SAS.</span></nobr></div>"
            + "</div>";
}
