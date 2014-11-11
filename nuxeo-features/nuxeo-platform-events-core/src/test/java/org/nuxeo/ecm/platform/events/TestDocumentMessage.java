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

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;

/**
 * JMSDocumentMessageProducer test case.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestDocumentMessage extends RepositoryTestCase {

    private Document root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Session session = getRepository().getSession(null);
        root = session.getRootDocument();
    }

    public void testMessageDocument() throws DocumentException {
        Document doc = root.addChild("doc", "File");

        DocumentMessage msg = DocumentMessageFactory.createDocumentMessage(doc);

        assertNull(msg.getCategory());
        assertNull(msg.getComment());
        assertNull(msg.getDocCurrentLifeCycle());
        assertNull(msg.getEventDate());
        assertNull(msg.getEventId());
        Map<String, Serializable> info = msg.getEventInfo();
        assertNotNull(info);
        assertEquals(0, info.size());
        assertNull(msg.getPrincipal());
        assertNull(msg.getPrincipalName());

        root.removeChild("doc");
    }

    public void testMessageDeletedDocModel() throws DocumentException {
        Document doc = root.addChild("doc", "File");
        DocumentModel docModel = DocumentModelFactory.createDocumentModel(doc,
                new String[] {"file"});
        root.removeChild("doc");

        DocumentMessage msg = new DocumentMessageImpl(docModel);

        assertNull(msg.getCategory());
        assertNull(msg.getComment());
        assertNull(msg.getDocCurrentLifeCycle());
        assertNull(msg.getEventDate());
        assertNull(msg.getEventId());
        Map<String, Serializable> info = msg.getEventInfo();
        assertNotNull(info);
        assertEquals(0, info.size());
        assertNull(msg.getPrincipal());
        assertNull(msg.getPrincipalName());
    }

    public void testMessageDocumentNoSource() throws DocumentException {
        DocumentMessage msg = DocumentMessageFactory.createDocumentMessage(null);

        assertNull(msg.getCategory());
        assertNull(msg.getComment());
        assertNull(msg.getDocCurrentLifeCycle());
        assertNull(msg.getEventDate());
        assertNull(msg.getEventId());
        assertNull(msg.getEventInfo());
        assertNull(msg.getPrincipal());
        assertNull(msg.getPrincipalName());
    }

}
