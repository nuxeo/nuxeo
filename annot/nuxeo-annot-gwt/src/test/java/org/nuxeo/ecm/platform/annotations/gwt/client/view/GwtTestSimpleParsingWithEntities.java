/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    private final AnnotatedDocument annotatedDocument = new AnnotatedDocument(
            null);

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
        annotation.setXpointer(XPointerFactory.getXPointer(
                "http://localhost:8080/nuxeo/nxdoc/default/f6c3a8c3-427f-40fc-a0a0-e7630c41fdce/#xpointer(string-range(/HTML[0]/BODY[0]/DIV[0]/DIV[0],\"\",7,30))"));
        annotation.setId(1);
        annotations.add(annotation);
    }

    @Override
    public String getInnerHtml() {
        return "Les r&ecirc;ve<span> qui les hantent</span> Au large d&#39;Amsterdam";
    }

}
