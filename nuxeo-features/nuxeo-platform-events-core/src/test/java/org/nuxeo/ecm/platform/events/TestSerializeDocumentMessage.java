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
 * $Id:TestSerializeDocumentMessage.java 3386 2006-09-29 13:32:49Z janguenot $
 */

package org.nuxeo.ecm.platform.events;

import org.nuxeo.common.utils.SerializableHelper;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * Test document message serialization.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestSerializeDocumentMessage extends RepositoryTestCase {

    private Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Session session = getRepository().getSession(null);
        root = session.getRootDocument();
    }

    public void testDocumentMessageSerialization() throws DocumentException {
        Document doc = root.addChild("doc", "File");
        DocumentMessage msg = DocumentMessageFactory.createDocumentMessage(doc);
        assertTrue(SerializableHelper.isSerializable(msg));
    }

    public void testDocumentMessageSerializationNoSource() throws DocumentException {
        Document doc = null;
        DocumentMessage msg = DocumentMessageFactory.createDocumentMessage(doc);
        assertTrue(SerializableHelper.isSerializable(msg));
    }

}
