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
 *     Nuxeo - initial API and implementation
 *
 * $Id:ContentHistoryActionsBean.java 4487 2006-10-19 22:27:14Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.web.listener.ejb;

import static org.jboss.seam.ScopeType.EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.ecm.platform.audit.api.comment.LinkedDocument;
import org.nuxeo.ecm.platform.audit.web.listener.ContentHistoryActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Content history actions bean.
 * <p>
 * :XXX: http://jira.nuxeo.org/browse/NXP-514
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Name("contentHistoryActions")
@Scope(EVENT)
public class ContentHistoryActionsBean implements ContentHistoryActions {

    private static final long serialVersionUID = -6110545879809627627L;

    private static final String EVENT_DATE = "eventDate";

    private static final Log log = LogFactory.getLog(ContentHistoryActionsBean.class);

    // @Out(required = false)
    protected List<LogEntry> logEntries;

    private Map<Long, String> logEntriesComments;

    private Map<Long, LinkedDocument> logEntriesLinkedDocs;

    // :FIXME: Should disappear with Seam 1.1 method params.
    // @Out(required = false)
    private List<LogEntry> latestLogEntries;

    // :FIXME: Hardcoded. See interface for more details about the reason
    protected final int nbLogEntries = 5;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    private transient NavigationContext navigationContext;

    @RequestParameter("sortColumn")
    protected String newSortColumn;

    protected SortInfo sortInfo;

    protected Map<String, FilterMapEntry> filterMap = Collections.emptyMap();

    protected Comparator<LogEntry> comparator;

    public ContentHistoryActionsBean() {
        // init sorting information
        sortInfo = new SortInfo(EVENT_DATE, false);
    }

    @Factory(value = "latestLogEntries", scope = EVENT)
    public List<LogEntry> computeLatestLogEntries() {
        if (latestLogEntries == null) {
            if (logEntries == null) {
                logEntries = computeLogEntries(navigationContext.getCurrentDocument());
            }
            if (logEntries != null) {
                if (logEntries.size() > nbLogEntries) {
                    latestLogEntries = new ArrayList<LogEntry>(logEntries.subList(0, nbLogEntries));
                } else {
                    latestLogEntries = logEntries;
                }
            }
        }
        return latestLogEntries;
    }

    @Factory(value = "logEntries", scope = EVENT)
    public List<LogEntry> computeLogEntries() {
        if (logEntries == null) {
            logEntries = computeLogEntries(navigationContext.getCurrentDocument());
        }
        return logEntries;
    }

    @Factory(value = "logEntriesComments", scope = EVENT)
    public Map<Long, String> computeLogEntriesComments() {
        if (logEntriesComments == null) {
            computeLogEntries();
            postProcessComments(logEntries);
        }
        return logEntriesComments;
    }

    @Factory(value = "logEntriesLinkedDocs", scope = EVENT)
    public Map<Long, LinkedDocument> computeLogEntrieslinkedDocs() {
        if (logEntriesLinkedDocs == null) {
            computeLogEntries();
            postProcessComments(logEntries);
        }
        return logEntriesLinkedDocs;
    }

    public List<LogEntry> computeLogEntries(DocumentModel document) {
        if (document == null) {
            return null;
        } else {
            Logs service = Framework.getLocalService(Logs.class);
            Logs logsBean = service;
            /*
             * In case the document is a proxy,meaning is the result of a publishing,to have the history of the document
             * from which this proxy was created,first we have to get to the version that was created when the document
             * was publish,and to which the proxy document indicates,and then from that version we have to get to the
             * root document.
             */
            boolean doDefaultSort = comparator == null;
            if (document.isProxy()) {
                // all users should have access to logs
                GetVersionInfoForDocumentRunner runner = new GetVersionInfoForDocumentRunner(documentManager, document);
                runner.runUnrestricted();
                if (runner.sourceDocForVersionId == null || runner.version == null) {
                    String message = "An error occurred while grabbing log entries for " + document.getId();
                    throw new NuxeoException(message);
                }

                Date versionCreationDate = getCreationDateForVersion(logsBean, runner.version);
                // add all the logs from the source document until the
                // version was created
                addLogEntries(getLogsForDocUntilDate(logsBean, runner.sourceDocForVersionId, versionCreationDate,
                        doDefaultSort));

                // !! add the first publishing
                // event after the version is created; since the publishing
                // event is logged few milliseconds after the version is
                // created

                List<LogEntry> publishingLogs = getLogsForDocUntilDateWithEvent(logsBean, runner.sourceDocForVersionId,
                        versionCreationDate, DocumentEventTypes.DOCUMENT_PUBLISHED, doDefaultSort);
                if (!publishingLogs.isEmpty()) {
                    addLogEntry(publishingLogs.get(0));
                }
                // add logs from the actual version
                filterMap = new HashMap<String, FilterMapEntry>();
                addLogEntries(logsBean.getLogEntriesFor(runner.version.getId(), filterMap, doDefaultSort));

            } else {
                addLogEntries(logsBean.getLogEntriesFor(document.getId(), filterMap, doDefaultSort));
            }

            if (log.isDebugEnabled()) {
                log.debug("logEntries computed .................!");
            }
            return logEntries;
        }
    }

    public String doSearch() {
        // toggle newOrderDirection
        if (StringUtils.isEmpty(newSortColumn)) {
            newSortColumn = EVENT_DATE;
        }
        String sortColumn = sortInfo.getSortColumn();
        boolean sortAscending = sortInfo.getSortAscending();
        if (newSortColumn.equals(sortColumn)) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = newSortColumn;
            sortAscending = true;
        }
        sortInfo = new SortInfo(sortColumn, sortAscending);
        logEntries = null;
        return null;
    }

    /**
     * Post-process log entries comments to add links. e5e7b4ba-0ffb-492d-8bf2-f2f2e6683ae2
     */
    private void postProcessComments(List<LogEntry> logEntries) {
        logEntriesComments = new HashMap<Long, String>();
        logEntriesLinkedDocs = new HashMap<Long, LinkedDocument>();

        CommentProcessorHelper cph = new CommentProcessorHelper(documentManager);

        if (logEntries == null) {
            return;
        }

        for (LogEntry entry : logEntries) {
            logEntriesComments.put(entry.getId(), cph.getLogComment(entry));
            LinkedDocument linkedDoc = cph.getLogLinkedDocument(entry);
            if (linkedDoc != null) {
                logEntriesLinkedDocs.put(entry.getId(), linkedDoc);
            }
        }
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }

    private Date getCreationDateForVersion(Logs logsService, DocumentModel version) {
        List<LogEntry> logs = logsService.getLogEntriesFor(version.getId(), filterMap, true);
        for (LogEntry logEntry : logs) {
            if (logEntry.getEventId().equals(DocumentEventTypes.DOCUMENT_CREATED)) {
                return logEntry.getEventDate();
            }
        }
        return null;
    }

    private void addLogEntries(List<LogEntry> entries) {
        if (logEntries != null) {
            logEntries.addAll(entries);
        } else {
            logEntries = entries;
        }
    }

    private void addLogEntry(LogEntry entry) {
        if (logEntries != null) {
            logEntries.add(entry);
        } else {
            logEntries = new ArrayList<LogEntry>();
            logEntries.add(entry);
        }
    }

    private static FilterMapEntry computeQueryForLogsOnDocUntilDate(Date date) {
        FilterMapEntry filterByDate = new FilterMapEntry();
        filterByDate.setColumnName(BuiltinLogEntryData.LOG_EVENT_DATE);
        filterByDate.setOperator("<=");
        filterByDate.setQueryParameterName(BuiltinLogEntryData.LOG_EVENT_DATE);
        filterByDate.setObject(date);
        return filterByDate;
    }

    private static FilterMapEntry computeQueryForLogsOnDocAfterDate(Date date) {
        FilterMapEntry filterByDate = new FilterMapEntry();
        filterByDate.setColumnName(BuiltinLogEntryData.LOG_EVENT_DATE);
        filterByDate.setOperator(">=");
        filterByDate.setQueryParameterName(BuiltinLogEntryData.LOG_EVENT_DATE);
        filterByDate.setObject(date);
        return filterByDate;
    }

    private static FilterMapEntry computeQueryForLogsWithEvent(String eventName) {
        FilterMapEntry filterByDate = new FilterMapEntry();
        filterByDate.setColumnName(BuiltinLogEntryData.LOG_EVENT_ID);
        filterByDate.setOperator("LIKE");
        filterByDate.setQueryParameterName(BuiltinLogEntryData.LOG_EVENT_ID);
        filterByDate.setObject(eventName);
        return filterByDate;
    }

    private List<LogEntry> getLogsForDocUntilDate(Logs logsService, String docId, Date date, boolean doDefaultSort) {
        filterMap = new HashMap<String, FilterMapEntry>();
        filterMap.put(BuiltinLogEntryData.LOG_EVENT_DATE, computeQueryForLogsOnDocUntilDate(date));
        return logsService.getLogEntriesFor(docId, filterMap, doDefaultSort);
    }

    private List<LogEntry> getLogsForDocUntilDateWithEvent(Logs logsService, String docId, Date date, String eventName,
            boolean doDefaultSort) {
        filterMap = new HashMap<String, FilterMapEntry>();
        filterMap.put(BuiltinLogEntryData.LOG_EVENT_DATE, computeQueryForLogsOnDocAfterDate(date));
        filterMap.put(BuiltinLogEntryData.LOG_EVENT_ID, computeQueryForLogsWithEvent(eventName));
        return logsService.getLogEntriesFor(docId, filterMap, doDefaultSort);

    }

    private class GetVersionInfoForDocumentRunner extends UnrestrictedSessionRunner {

        public String sourceDocForVersionId;

        public DocumentModel version;

        DocumentModel document;

        public GetVersionInfoForDocumentRunner(CoreSession session, DocumentModel document) {
            super(session);
            this.document = document;
        }

        @Override
        public void run() {
            version = documentManager.getSourceDocument(document.getRef());
            if (version != null) {
                sourceDocForVersionId = session.getSourceDocument(version.getRef()).getId();
            }
        }
    }

}
