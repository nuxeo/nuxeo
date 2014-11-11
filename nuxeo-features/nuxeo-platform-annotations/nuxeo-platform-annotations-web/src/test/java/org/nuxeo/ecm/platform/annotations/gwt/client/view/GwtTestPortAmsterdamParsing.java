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

import org.nuxeo.ecm.platform.annotations.gwt.client.AbstractDocumentGWTTest;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestPortAmsterdamParsing extends AbstractDocumentGWTTest {
    private AnnotatedDocument annotatedDocument = new AnnotatedDocument(null);

    public void testParse() {
        createDocument();
        for (Annotation annotation : getAnnotations(new String[] {
                "/HTML[0]/BODY[0]/DIV[0]/P[0]/SPAN[0],\"\",55,47",
                "/HTML[0]/BODY[0]/DIV[0]/P[0]/SPAN[0],\"\",321,92",
                "/HTML[0]/BODY[0]/DIV[0]/P[0]/SPAN[0],\"\",15,9" })) {
            annotatedDocument.decorate(annotation);
        }
        String resultInnerHtml = RootPanel.get("insidediv").getElement().getInnerHTML();
        @SuppressWarnings("unused")
        String debugString = resultInnerHtml.substring(resultInnerHtml.indexOf("<p"));
        assertTrue(resultInnerHtml.contains("<span class=\"decorate decorate0\"> Les rêves qui les hantent</span><br>"
                + "<span class=\"decorate decorate0\"> Au large d'Amsterda</span>m"));
    }

    public String getInnerHtml() {
        return new StringBuilder(
                "<p class=\"MsoNormal\"><span>Dans le port d&#39;Amsterdam <br />").append(
                " Y a des marins qui chantent<br /> Les r&ecirc;ves qui les hantent<br />").append(
                " Au large d&#39;Amsterdam<br /> Dans le port d&#39;Amsterdam<br /> Y a des marins qui dorment<br />").append(
                " Comme des oriflammes<br /> Le long des berges mornes<br /> Dans le port d&#39;Amsterdam<br /> Y a des marins qui meurent<br />").append(
                " Pleins de bi&egrave;re et de drames<br /> Aux premi&egrave;res lueurs<br /> Mais dans le port d&#39;Amsterdam<br />").append(
                " Y a des marins qui naissent<br /> Dans la chaleur &eacute;paisse<br /> Des langueurs oc&eacute;anes</span></p>    <p>Dans le port d'Amsterdam<br> Y a des marins qui mangent<br> Sur des nappes trop blanches<br>").toString();
    }
}
