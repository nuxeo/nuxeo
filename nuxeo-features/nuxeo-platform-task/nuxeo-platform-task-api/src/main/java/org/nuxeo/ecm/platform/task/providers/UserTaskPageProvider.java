/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.task.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider for {@link DashBoardItem} elements.
 * <p>
 * Useful for content views displaying users' tasks.
 * <p>
 * WARNING: this page provider does not handle sorting, and its pagination management is not efficient (done in post
 * filter).
 * <p>
 * This page provider requires the property {@link #CORE_SESSION_PROPERTY} to be filled with a core session. It also
 * accepts an optional property {@link #FILTER_DOCS_FROM_TRASH}, defaulting to true.
 *
 * @since 5.5
 */
public class UserTaskPageProvider extends AbstractPageProvider<DashBoardItem> implements PageProvider<DashBoardItem> {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserTaskPageProvider.class);

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String FILTER_DOCS_FROM_TRASH = "filterDocumentsFromTrash";

    protected List<DashBoardItem> userTasks;

    protected List<DashBoardItem> pageTasks;

    @Override
    public List<DashBoardItem> getCurrentPage() {
        if (pageTasks == null) {
            pageTasks = new ArrayList<>();
            if (userTasks == null) {
                getAllTasks();
            }
            if (!hasError()) {
                long resultsCount = userTasks.size();
                setResultsCount(resultsCount);
                // post-filter the results "by hand" to handle pagination
                long pageSize = getMinMaxPageSize();
                if (pageSize == 0) {
                    pageTasks.addAll(userTasks);
                } else {
                    // handle offset
                    long offset = getCurrentPageOffset();
                    if (offset <= resultsCount) {
                        for (int i = Long.valueOf(offset).intValue(); i < resultsCount && i < offset + pageSize; i++) {
                            pageTasks.add(userTasks.get(i));
                        }
                    }
                }
            }
        }
        return pageTasks;
    }

    protected Locale getLocale() {
        String locale = (String) getProperties().get("locale");
        if (!StringUtils.isBlank(locale)) {
            return LocaleUtils.toLocale(locale);
        }
        return Locale.getDefault();
    }

    protected void getAllTasks() {
        error = null;
        errorMessage = null;
        userTasks = new ArrayList<>();
        CoreSession coreSession = getCoreSession();
        boolean filterTrashDocs = getFilterDocumentsInTrash();
        NuxeoPrincipal pal = coreSession.getPrincipal();
        TaskService taskService = Framework.getService(TaskService.class);
        List<Task> tasks = taskService.getAllCurrentTaskInstances(coreSession, getSortInfos());
        if (tasks != null) {
            for (Task task : tasks) {
                List<String> targetDocumentsIds = task.getTargetDocumentsIds();
                boolean hasTargetDocuments = targetDocumentsIds != null && !targetDocumentsIds.isEmpty();
                if (task.hasEnded() || task.isCancelled() || !hasTargetDocuments) {
                    continue;
                }
                DocumentModel doc = taskService.getTargetDocumentModel(task, coreSession);
                if (doc != null) {
                    if (!filterTrashDocs || !doc.isTrashed()) {
                        userTasks.add(new DashBoardItemImpl(task, doc, getLocale()));
                    }
                } else {
                    log.warn(String.format("User '%s' has a task of type '%s' on a missing or deleted document",
                            pal.getName(), task.getName()));
                }
            }
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
            throw new NuxeoException("cannot find core session");
        }
        return coreSession;
    }

    /**
     * This page provider does not support sort for now => override what may be contributed in the definition
     */
    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    protected void pageChanged() {
        pageTasks = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        userTasks = null;
        pageTasks = null;
        super.refresh();
    }

}
