/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: TestSerializeDocumentMessage.java 1307 2006-07-24 01:06:28Z janguenot $
 */

package org.nuxeo.ecm.platform.events;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;

/**
 * JMSDocumentMessageProducer test case.
 *
 * @author Julien Anguenot
 */
public class TestDocumentMessage extends RepositoryTestCase {

    private Document root;
    private Document doc;
    private CoreEvent event;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Session session = getRepository().getSession(null);
        root = session.getRootDocument();
        Map <String, Object> info = new HashMap<String, Object>();
        event = new CoreEventImpl("event_id", null, info, null, "category", "comment");
    }

    public void testMessageDocument() throws DocumentException {
        doc = root.addChild("doc", "File");
        DocumentMessage msg = DocumentMessageFactory.createDocumentMessage(doc, event);

        assertEquals("category", msg.getCategory());
        assertEquals("comment", msg.getComment());
        assertEquals("event_id", msg.getEventId());
        assertNull(msg.getDocCurrentLifeCycle());
        assertNotNull(msg.getEventDate());
        Map<String, Serializable> info = msg.getEventInfo();
        assertNotNull(info);
        assertEquals(2, info.size());
        assertNull(msg.getPrincipal());
        assertNull(msg.getPrincipalName());

        root.removeChild("doc");
    }

    public void testMessageDeletedDocModel() throws DocumentException {
        doc = root.addChild("doc", "File");
        DocumentModel docModel = DocumentModelFactory.createDocumentModel(doc,
                new String[] {"file"});
        root.removeChild("doc");

        DocumentMessage msg = new DocumentMessageImpl(docModel, event);

        assertEquals("category", msg.getCategory());
        assertEquals("comment", msg.getComment());
        assertEquals("event_id", msg.getEventId());
        assertNull(msg.getDocCurrentLifeCycle());
        assertNotNull(msg.getEventDate());
        Map<String, Serializable> info = msg.getEventInfo();
        assertNotNull(info);
        assertEquals(2, info.size());
        assertNull(msg.getPrincipal());
        assertNull(msg.getPrincipalName());
    }

}
