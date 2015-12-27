/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     anguenot
 *
 * $Id: WSAuditBean.java 30185 2008-02-14 17:56:36Z tdelprat $
 */

package org.nuxeo.ecm.platform.audit.ws;

import static org.nuxeo.ecm.core.api.event.DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
import static org.nuxeo.ecm.core.api.event.DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.ws.api.WSAudit;
import org.nuxeo.ecm.platform.ws.AbstractNuxeoWebService;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit Web Service bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebService(name = "WSAuditInterface", serviceName = "WSAuditService")
@SOAPBinding(style = Style.DOCUMENT)
public class WSAuditBean extends AbstractNuxeoWebService implements WSAudit {

    private static final long serialVersionUID = 1L;

    protected final Logs getLogsBean() {
        return Framework.getService(Logs.class);
    }

    @WebMethod
    public ModifiedDocumentDescriptor[] listModifiedDocuments(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "dataRangeQuery") String dateRangeQuery) {
        initSession(sessionId);

        BatchInfo batchInfo = BatchHelper.getBatchInfo(sessionId, dateRangeQuery);

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(null, batchInfo.getPageDateRange(),
                EVENT_DOCUMENT_CATEGORY, null, batchInfo.getNextPage(), batchInfo.getPageSize());
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
                ldocs.add(new ModifiedDocumentDescriptor(logEntry.getEventDate(), logEntry.getDocType(),
                        logEntry.getDocUUID()));
            }
        }

        ModifiedDocumentDescriptor[] docs = new ModifiedDocumentDescriptor[ldocs.size()];
        ldocs.toArray(docs);

        return docs;
    }

    @WebMethod
    public ModifiedDocumentDescriptorPage listModifiedDocumentsByPage(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "dataRangeQuery") String dateRangeQuery, @WebParam(name = "docPath") String path,
            @WebParam(name = "pageIndex") int page, @WebParam(name = "pageSize") int pageSize) {
        initSession(sessionId);

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(null, dateRangeQuery, EVENT_DOCUMENT_CATEGORY, path,
                page, pageSize);

        boolean hasMorePage = logEntries.size() >= pageSize;

        List<ModifiedDocumentDescriptor> ldocs = new ArrayList<ModifiedDocumentDescriptor>();
        Set<String> uuids = new HashSet<String>();
        for (LogEntry logEntry : logEntries) {
            if (!uuids.contains(logEntry.getDocUUID())) {
                uuids.add(logEntry.getDocUUID());
                ldocs.add(new ModifiedDocumentDescriptor(logEntry.getEventDate(), logEntry.getDocType(),
                        logEntry.getDocUUID()));
            }
        }

        ModifiedDocumentDescriptor[] docs = new ModifiedDocumentDescriptor[ldocs.size()];
        ldocs.toArray(docs);

        return new ModifiedDocumentDescriptorPage(docs, page, hasMorePage);
    }

    @WebMethod
    public ModifiedDocumentDescriptorPage listDeletedDocumentsByPage(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "dataRangeQuery") String dateRangeQuery, @WebParam(name = "docPath") String path,
            @WebParam(name = "pageIndex") int page, @WebParam(name = "pageSize") int pageSize) {
        initSession(sessionId);

        String[] eventIds = { "documentRemoved" };

        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(eventIds, dateRangeQuery, "eventDocumentCategory",
                path, page, pageSize);

        boolean hasMorePage = logEntries.size() >= pageSize;

        List<ModifiedDocumentDescriptor> ldocs = new ArrayList<ModifiedDocumentDescriptor>();
        Set<String> uuids = new HashSet<String>();
        for (LogEntry logEntry : logEntries) {
            if (!uuids.contains(logEntry.getDocUUID())) {
                uuids.add(logEntry.getDocUUID());
                ldocs.add(new ModifiedDocumentDescriptor(logEntry.getEventDate(), logEntry.getDocType(),
                        logEntry.getDocUUID()));
            }
        }

        ModifiedDocumentDescriptor[] docs = new ModifiedDocumentDescriptor[ldocs.size()];
        ldocs.toArray(docs);

        return new ModifiedDocumentDescriptorPage(docs, page, hasMorePage);
    }

    @WebMethod
    public EventDescriptorPage listEventsByPage(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "dataRangeQuery") String dateRangeQuery, @WebParam(name = "pageIndex") int page,
            @WebParam(name = "pageSize") int pageSize) {
        initSession(sessionId);

        String[] categories = new String[0];
        List<LogEntry> logEntries = getLogsBean().queryLogsByPage(null, dateRangeQuery, categories, null, page,
                pageSize);
        boolean hasMorePage = logEntries.size() >= pageSize;

        List<EventDescriptor> events = new ArrayList<EventDescriptor>();

        for (LogEntry logEntry : logEntries) {
            events.add(new EventDescriptor(logEntry));
        }

        EventDescriptor[] evts = new EventDescriptor[events.size()];
        events.toArray(evts);

        return new EventDescriptorPage(evts, page, hasMorePage);
    }

    @WebMethod
    public EventDescriptorPage listDocumentEventsByPage(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "dataRangeQuery") String dateRangeQuery, @WebParam(name = "startDate") String startDate,
            @WebParam(name = "path") String path, @WebParam(name = "pageIndex") int page,
            @WebParam(name = "pageSize") int pageSize) {
        initSession(sessionId);

        String[] docCategories = { EVENT_DOCUMENT_CATEGORY, EVENT_LIFE_CYCLE_CATEGORY };

        List<LogEntry> logEntries;
        if (dateRangeQuery != null && dateRangeQuery.length() > 0) {
            logEntries = getLogsBean().queryLogsByPage(null, dateRangeQuery, docCategories, path, page, pageSize);
        } else {
            Date limit = DateParser.parseW3CDateTime(startDate);
            logEntries = getLogsBean().queryLogsByPage(null, limit, docCategories, path, page, pageSize);
        }
        boolean hasMorePage = logEntries.size() >= pageSize;

        List<EventDescriptor> events = new ArrayList<EventDescriptor>();

        for (LogEntry logEntry : logEntries) {
            events.add(new EventDescriptor(logEntry));
        }

        EventDescriptor[] evts = new EventDescriptor[events.size()];
        events.toArray(evts);

        return new EventDescriptorPage(evts, page, hasMorePage);
    }

    @WebMethod
    public EventDescriptorPage queryEventsByPage(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "whereClause") String whereClause, @WebParam(name = "pageIndex") int page,
            @WebParam(name = "pageSize") int pageSize) {
        initSession(sessionId);

        List<LogEntry> logEntries = getLogsBean().nativeQueryLogs(whereClause, page, pageSize);
        boolean hasMorePage = logEntries.size() >= pageSize;

        List<EventDescriptor> events = new ArrayList<EventDescriptor>();

        for (LogEntry logEntry : logEntries) {
            events.add(new EventDescriptor(logEntry));
        }

        EventDescriptor[] evts = new EventDescriptor[events.size()];
        events.toArray(evts);

        return new EventDescriptorPage(evts, page, hasMorePage);
    }

}
