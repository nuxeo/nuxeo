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

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.AbstractDocumentGWTTest;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointerFactory;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class GwtTestSimpleParsingWithEntities extends AbstractDocumentGWTTest {

    private final AnnotatedDocument annotatedDocument = new AnnotatedDocument(null);

    private final List<Annotation> annotations = new ArrayList<Annotation>();

    public void testParse() {
        createDocument();
        setAnnotations(annotations);
        for (Annotation annotation : annotations) {
            annotatedDocument.decorate(annotation);
        }
        String resultInnerHtml = RootPanel.get("insidediv").getElement().getInnerHTML();
        assertEquals(
                "Les rêv<span class=\"decorate decorate1\">e</span><span><span class=\"decorate decorate1\"> qui les hantent</span></span><span class=\"decorate decorate1\"> Au large d'A</span>msterdam",
                resultInnerHtml);
    }

    private static void setAnnotations(List<Annotation> annotations) {
        Annotation annotation = new Annotation();
        annotation.setXpointer(XPointerFactory.getXPointer("http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/#xpointer(string-range(/HTML[0]/BODY[0]/DIV[0]/DIV[0],\"\",7,30))"));
        annotation.setId(1);
        annotations.add(annotation);
    }

    @Override
    public String getInnerHtml() {
        return "Les r&ecirc;ve<span> qui les hantent</span> Au large d&#39;Amsterdam";
    }

}
