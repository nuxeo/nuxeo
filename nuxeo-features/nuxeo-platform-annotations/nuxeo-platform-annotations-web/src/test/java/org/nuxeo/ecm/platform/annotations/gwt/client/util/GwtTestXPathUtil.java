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

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.AbstractDocumentGWTTest;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestXPathUtil extends AbstractDocumentGWTTest {
    private final XPathUtil xPathUtil = new XPathUtil();

    @SuppressWarnings("static-access")
    public void testGetGetXPath() {
        createDocument();
        NodeList<Element> el = RootPanel.get().getBodyElement().getElementsByTagName(
                "span");
        assertNotNull(el);
        Node node = el.getItem(0);
        assertNotNull(node);
        String xpath = xPathUtil.getXPath(node);
        assertNotNull(xpath);
        assertEquals("/html[0]/body[0]/div[0]/div[0]/div[0]/nobr[0]/span[0]", xpath.toLowerCase());
    }

    public void testGetNode() {
        createDocument();
        Node node = xPathUtil.getNode("/html[0]/body[0]/div[0]/div[0]/div[0]/nobr[0]",
                Document.get()).get(0);
        assertNotNull(node);
        assertEquals(node.getNodeName().toLowerCase(), "nobr");
        List<Node> nodes = xPathUtil.getNode("//img", Document.get());
        assertEquals(1, nodes.size());
    }

    //test fail in mvn, succeed in eclipse ...
    public void _testGetXPathDecoratedDocument() {
        createDocument();
        List<Node> nodes = xPathUtil.getNode("/html[0]/body[0]/div[0]/b[0]/span[1]", Document.get());
        assertNotNull(nodes);
        Node node = nodes.get(0);
        SpanElement span = SpanElement.as(node).cast();
        assertNotNull(span);
        assertEquals("c", span.getInnerHTML());
        node = RootPanel.get("myspan").getElement();
        String xpath = xPathUtil.getXPath(node);
        assertEquals(xpath.toLowerCase(), "/html[0]/body[0]/div[0]/b[0]/span[1]");
    }
    public void testGetShortLength() {
        assertEquals(xPathUtil.getShortLength("a\nb\n\nc"), 3);
    }
    @Override
    public String getInnerHtml() {
        return super.getInnerHtml() + "<b><span class=\"decorate decorate1\">a</span><span>b</span><span id=\"myspan\">c</span></b>";
    }
}
