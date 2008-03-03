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
 * $Id:TestLogEntry.java 3535 2006-10-04 09:53:05Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.facade;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.ejb.LogEntryImpl;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/*
 * @author anguenot
 */
public class TestLogEntry extends MockObjectTestCase {

    private DocumentMessage createDocumentMessage(String eventId) {

        Mock mockDocMsg = mock(DocumentMessage.class);
        mockDocMsg.expects(once()).method("getEventId").will(returnValue(eventId));
        mockDocMsg.expects(once()).method("getId");
        mockDocMsg.expects(once()).method("getPathAsString");
        mockDocMsg.expects(once()).method("getType");
        mockDocMsg.expects(once()).method("getEventDate");
        mockDocMsg.expects(once()).method("getPrincipalName");
        mockDocMsg.expects(once()).method("getComment");
        mockDocMsg.expects(once()).method("getCategory");
        mockDocMsg.expects(once()).method("getDocCurrentLifeCycle");

        return (DocumentMessage) mockDocMsg.proxy();
    }

    public void testGetEventId() throws DocumentException {
        String eventId = "documentCreated";
        DocumentMessage docMsg = createDocumentMessage(eventId);

        LogEntry logEntry = new LogEntryImpl(docMsg);
        assertEquals(logEntry.getEventId(), eventId);
    }

    public void testFetUUID() throws DocumentException {
        String eventId = "documentCreated";
        DocumentMessage docMsg = createDocumentMessage(eventId);

        LogEntryImpl logEntry = new LogEntryImpl(docMsg);
        System.out.println("Log Entry doc UUID : " + logEntry.getDocUUID());

        // :XXX: Fake document is not initialized
        assertNull(logEntry.getDocUUID());
    }

}
