/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.jbpm.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItem;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItemImpl;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider for {@link DocumentProcessItem} elements.
 * <p>
 * Useful for content views displaying users' processes.
 * <p>
 * WARNING: this page provider does not handle sorting, and its pagination
 * management is not efficient (done in post filter).
 * <p>
 * This page provider requires the property {@link #CORE_SESSION_PROPERTY} to
 * be filled with a core session. It also accepts an optional property
 * {@link #FILTER_DOCS_FROM_TRASH}, defaulting to true.
 *
 * @since 5.4.2
 */
public class UserProcessPageProvider extends
        AbstractPageProvider<DocumentProcessItem> implements
        PageProvider<DocumentProcessItem> {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserProcessPageProvider.class);

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String FILTER_DOCS_FROM_TRASH = "filterDocumentsFromTrash";

    protected List<DocumentProcessItem> userProcesses;

    protected List<DocumentProcessItem> pageProcesses;

    @Override
    public List<DocumentProcessItem> getCurrentPage() {
        if (pageProcesses == null) {
            pageProcesses = new ArrayList<DocumentProcessItem>();
            if (userProcesses == null) {
                getAllProcesses();
            }
            if (!hasError()) {
                int resultsCount = userProcesses.size();
                setResultsCount(resultsCount);
                // post-filter the results "by hand" to handle pagination
                long pageSize = getMinMaxPageSize();
                if (pageSize == 0) {
                    pageProcesses.addAll(userProcesses);
                } else {
                    // handle offset
                    long offset = getCurrentPageOffset();
                    if (offset <= resultsCount) {
                        for (int i = Long.valueOf(offset).intValue(); i < resultsCount
                                && i < offset + pageSize; i++) {
                            pageProcesses.add(userProcesses.get(i));
                        }
                    }
                }
            }
        }
        return pageProcesses;
    }

    protected void getAllProcesses() {
        error = null;
        errorMessage = null;
        userProcesses = new ArrayList<DocumentProcessItem>();

        try {
            CoreSession coreSession = getCoreSession();
            boolean filterTrashDocs = getFilterDocumentsInTrash();
            NuxeoPrincipal pal = (NuxeoPrincipal) coreSession.getPrincipal();
            JbpmService jbpmService = Framework.getService(JbpmService.class);
            List<ProcessInstance> processes = jbpmService.getCurrentProcessInstances(
                    pal, null);
            if (processes != null) {
                for (ProcessInstance process : processes) {
                    try {
                        if (process.hasEnded()) {
                            continue;
                        }
                        DocumentModel doc = jbpmService.getDocumentModel(
                                process, pal);
                        if (doc != null) {
                            if (filterTrashDocs
                                    && LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                                continue;
                            } else {
                                userProcesses.add(new DocumentProcessItemImpl(
                                        process, doc));
                            }
                        } else {
                            log.warn(String.format(
                                    "User '%s' has a process of type '%s' on a "
                                            + "missing or deleted document",
                                    pal.getName(),
                                    process.getProcessDefinition().getName()));
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        } catch (Exception e) {
            error = e;
            errorMessage = e.getMessage();
            log.warn(e.getMessage(), e);
        }
    }

    protected boolean getFilterDocumentsInTrash() {
        Map<String, Serializable> props = getProperties();
        if (props.containsKey(FILTER_DOCS_FROM_TRASH)) {
            return Boolean.TRUE.equals(Boolean.valueOf((String) props.get(FILTER_DOCS_FROM_TRASH)));
        }
        return true;
    }

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new ClientRuntimeException("cannot find core session");
        }
        return coreSession;
    }

    /**
     * This page provider does not support sort for now => override what may be
     * contributed in the definition
     */
    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    protected void pageChanged() {
        super.pageChanged();
        pageProcesses = null;
    }

    @Override
    public void refresh() {
        super.refresh();
        userProcesses = null;
        pageProcesses = null;
    }

}
