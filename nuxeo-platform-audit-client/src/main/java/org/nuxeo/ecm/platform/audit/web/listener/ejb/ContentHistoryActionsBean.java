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
 * $Id:ContentHistoryActionsBean.java 4487 2006-10-19 22:27:14Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.AuditEventTypes;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.delegate.AuditLogsServiceDelegate;
import org.nuxeo.ecm.platform.audit.web.listener.ContentHistoryActions;
import org.nuxeo.ecm.platform.audit.web.listener.ejb.local.ContentHistoryActionsLocal;
import org.nuxeo.ecm.platform.audit.web.listener.ejb.remote.ContentHistoryActionsRemote;
import org.nuxeo.ecm.platform.audit.web.listener.events.EventNames;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Content history actions bean.
 * <p>
 * :XXX: http://jira.nuxeo.org/browse/NXP-514
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */

@Name("contentHistoryActions")
@Scope(CONVERSATION)
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

    @In(required = false)
    protected DocumentModel currentDocument;

    // :FIXME: Hardcoded. See interface for more details about the reason
    protected final int nbLogEntries = 5;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @RequestParameter("sortColumn")
    protected String newSortColumn;

    protected SortInfo sortInfo;

    protected Map<String, FilterMapEntry> filterMap;

    protected Comparator<LogEntry> comparator;

    public ContentHistoryActionsBean() {
        // init sorting information
        sortInfo = new SortInfo(EVENT_DATE, false);
    }

    @Destroy
    public void destroy() {
        log.debug("Removing Audit Seam component...");
    }

    @Observer(value = { EventNames.CONTENT_ROOT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED, EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOMAIN_SELECTION_CHANGED,
            EventNames.LOCATION_SELECTION_CHANGED,
            AuditEventTypes.HISTORY_CHANGED }, create = false, inject=false)
    public void invalidateLogEntries() {
        log.debug("Invalidate log entries.................");
        logEntries = null;
        latestLogEntries = null;
        logEntriesComments = null; // new HashMap<Long,String>();
        logEntriesLinkedDocs = null; // new HashMap<Long,LinkedDocument>();
    }

    @Factory(value = "latestLogEntries", scope = EVENT)
    public List<LogEntry> computeLatestLogEntries() throws AuditException {
        if (logEntries == null) {
            compute();
        }
        return latestLogEntries;
    }

    @Factory(value = "logEntries", scope = EVENT)
    public List<LogEntry> computeLogEntries() throws AuditException {
        compute();
        return logEntries;
    }

    @Factory(value = "logEntriesComments", scope = EVENT)
    public Map<Long, String> computeLogEntriesComments() {
        postProcessComments();
        return logEntriesComments;
    }

    @Factory(value = "logEntriesLinkedDocs", scope = EVENT)
    public Map<Long, LinkedDocument> computeLogEntrieslinkedDocs() {
        if (logEntriesLinkedDocs == null) {
            postProcessComments();
        }
        return logEntriesLinkedDocs;
    }

    public void compute() throws AuditException {
        if (currentDocument == null) {
            log.error("Selected document has not been injected !............");
        } else {
            try {
                Logs logsBean = AuditLogsServiceDelegate.getRemoteAuditLogsService();
                /**
                 * in case the document is a proxy,meaning is the result of a
                 * publishing,to have the history of the document from which
                 * this proxy was created,first we have to get to the version
                 * that was created when the document was publish,and to which
                 * the proxy document indicates,and then from that version we
                 * have to get to the root document.
                 */
                boolean doDefaultSort = comparator == null ? true : false;
                if (currentDocument.isProxy()) {
                    DocumentModel version = documentManager.getSourceDocument(currentDocument.getRef());
                    // logEntries =
                    // logsBean.getLogEntriesFor(documentManager.getSourceDocument(version.getRef()).getId());
                    logEntries = logsBean.getLogEntriesFor(
                            documentManager.getSourceDocument(version.getRef()).getId(),
                            filterMap, doDefaultSort);
                } else {
                    // logEntries =
                    // logsBean.getLogEntriesFor(currentDocument.getId());
                    logEntries = logsBean.getLogEntriesFor(
                            currentDocument.getId(), filterMap, doDefaultSort);
                }

                log.debug("logEntries computed .................!");
            } catch (Exception e) {
                log.error("An error occurred while grabbing log entries for "
                        + currentDocument.getId());
                throw new AuditException(e);
            }
            if (logEntries.size() > nbLogEntries) {
                latestLogEntries = new ArrayList<LogEntry>(logEntries.subList(
                        0, nbLogEntries));
            } else {
                latestLogEntries = logEntries;
            }
        }
    }

    public String doSearch() throws AuditException {
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
     * Post-process log entries comments to add links.
     * e5e7b4ba-0ffb-492d-8bf2-f2f2e6683ae2
     */
    private void postProcessComments() {
        logEntriesComments = new HashMap<Long, String>();
        logEntriesLinkedDocs = new HashMap<Long, LinkedDocument>();

        // Check if logEntries have been computed because required
        // XXX JA : workaround. Need to cleanup the whole action listener here.
        if (logEntries == null) {
            try {
                compute();
            } catch (AuditException ae) {
                log.error(
                        "An error occured while trying to compute log entries...",
                        ae);
                return;
            }
        }

        for (LogEntry entry : logEntries) {
            String newComment = null;
            String oldComment = entry.getComment();
            if (oldComment == null) {
                continue;
            }

            DocumentRef docRef = null;
            RepositoryLocation repoLoc = null;

            try {
                String repoName = oldComment.split(":")[0];
                String strDocRef = oldComment.split(":")[1];

                docRef = new IdRef(strDocRef);
                repoLoc = new RepositoryLocation(repoName);
            } catch (Exception e) {
                // not the expected format : continue to next entry
                logEntriesComments.put(entry.getId(), oldComment);
                continue;
            }

            // init the LinkedDoc object
            LinkedDocument linkedDoc = new LinkedDocument();
            linkedDoc.setDocumentRef(docRef);
            linkedDoc.setRepository(repoLoc);

            // try to resolve target document
            try {
                // XXX multi-repository management
                DocumentModel targetDoc = documentManager.getDocument(docRef);
                linkedDoc.setDocument(targetDoc);
                linkedDoc.setBrokenDocument(false);
            } catch (ClientException e) {
                // error or broken document
                linkedDoc.setBrokenDocument(true);
            }

            // update comment
            if (entry.getEventId().equals("documentDuplicated")) {
                newComment = "audit.duplicated_to";
            } else if (entry.getEventId().equals("documentCreatedByCopy")) {
                newComment = "audit.copied_from";
            } else if (entry.getEventId().equals("documentMoved")) {
                newComment = "audit.moved_from";
            }

            if (newComment != null) {
                logEntriesComments.put(entry.getId(), newComment);
                logEntriesLinkedDocs.put(entry.getId(), linkedDoc);
            } else {
                logEntriesComments.put(entry.getId(), oldComment);
            }

        }
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }

}
