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
import org.nuxeo.ecm.platform.annotations.gwt.client.util.TextGrabberVisitor;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Visitor;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;

/**
 * @author Alexandre Russel
 *
 */
public class GwtTestTextGrabberVisitor extends AbstractDocumentGWTTest {
    public void testProcess() {
        createDocument();
        Node start = DOM.getElementById("thediv");
        assertNotNull(start);
        Node end = start.getLastChild();
        assertNotNull(end);
        assertEquals(Node.TEXT_NODE, end.getNodeType());
        Text text = (Text) end;
        assertEquals(" and other stuff" ,text.getNodeValue());
        TextGrabberVisitor processor = new TextGrabberVisitor();
        Visitor visitor = new Visitor(processor);
        visitor.process(start, end);
        String result = processor.getText();
        assertNotNull(result);
        assertEquals("The Da Service", result);
    }
}
