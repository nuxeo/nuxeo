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
 *     anguenot
 *
 * $Id: WSAuditBean.java 30185 2008-02-14 17:56:36Z tdelprat $
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.delegate.AuditLogsServiceDelegate;
import org.nuxeo.ecm.platform.audit.ws.api.WSAudit;
import org.nuxeo.ecm.platform.ws.AbstractNuxeoWebService;

/**
 * Audit Web Service bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Stateless
@SerializedConcurrentAccess
@Local(WSAudit.class)
@Remote(WSAudit.class)
@WebService(name = "WSAuditInterface", serviceName = "WSAuditService")
@SOAPBinding(style = Style.DOCUMENT)
public class WSAuditBean extends AbstractNuxeoWebService implements WSAudit {

    private static final long serialVersionUID = 1L;

    private transient Logs logsBean;

    protected final Logs getLogsBean() throws AuditException {
        logsBean = AuditLogsServiceDelegate.getRemoteAuditLogsService();
        if (logsBean == null) {
            throw new AuditException("Cannot find log remote bean...");
        }
        return logsBean;
    }

    @WebMethod
    public ModifiedDocumentDescriptor[] listModifiedDocuments(String sessionId,
            String dateRangeQuery) throws AuditException {

        try {
            initSession(sessionId);
        } catch (ClientException ce) {
            throw new AuditException(ce.getMessage(), ce);
        }

        String[] eventIds = new String[] { DocumentEventTypes.DOCUMENT_UPDATED,
                DocumentEventTypes.DOCUMENT_CREATED,
                DocumentEventTypes.DOCUMENT_REMOVED };

        BatchInfo batchInfo = BatchHelper.getBatchInfo(sessionId, dateRangeQuery);

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(eventIds,
                batchInfo.getPageDateRange(), batchInfo.getNextPage(), batchInfo.getPageSize());
        if (logEntries.size() < batchInfo.getPageSize()) {
            // we are at the end of the batch
            // ==> reset the batch
            BatchHelper.resetBatchInfo(sessionId);
        } else {
            // set the batchInfo ready for next call
            batchInfo.prepareNextCall();
        }

        List<ModifiedDocumentDescriptor> ldocs = new ArrayList<ModifiedDocumentDescriptor>();
        Set<String> uuids = new HashSet<String>();
        for (LogEntry logEntry : logEntries) {
            if (!uuids.contains(logEntry.getDocUUID())) {
                uuids.add(logEntry.getDocUUID());
                ldocs.add(new ModifiedDocumentDescriptor(
                        logEntry.getEventDate(), logEntry.getDocType(),
                        logEntry.getDocUUID()));
            }
        }

        ModifiedDocumentDescriptor[] docs = new ModifiedDocumentDescriptor[ldocs.size()];
        ldocs.toArray(docs);

        return docs;
    }

}
