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

import org.nuxeo.ecm.platform.annotations.gwt.client.AbstractDocumentGWTTest;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.TextGrabberVisitor;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Visitor;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;

/**
 * @author Alexandre Russel
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
        assertEquals(" and other stuff", text.getNodeValue());
        TextGrabberVisitor processor = new TextGrabberVisitor();
        Visitor visitor = new Visitor(processor);
        visitor.process(start, end);
        String result = processor.getText();
        assertNotNull(result);
        assertEquals("The Da Service", result);
    }
}
