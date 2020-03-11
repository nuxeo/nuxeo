/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.task.dashboard;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;

/**
 * Dashboard item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @since 5.5
 */
public class DashBoardItemImpl extends AbstractDashBoardItemImpl implements DashBoardItem {

    private static final long serialVersionUID = 919752175741886376L;

    protected final String id;

    protected final String name;

    protected final String description;

    protected final Date startDate;

    protected final Date dueDate;

    protected final boolean expired;

    protected final String directive;

    protected final DocumentModel document;

    protected final Task task;

    protected String comment;

    public DashBoardItemImpl(Task task, Locale locale) {
        this(task, task.getDocument(), locale);
    }

    public DashBoardItemImpl(Task task, DocumentModel document, Locale locale) {
        this.task = task;
        this.document = document;
        this.locale = locale;
        id = task.getId();
        name = task.getName();
        description = task.getDescription();
        dueDate = task.getDueDate();
        startDate = task.getCreated();
        directive = task.getDirective();
        List<TaskComment> comments = task.getComments();
        if (comments != null && !comments.isEmpty()) {
            comment = comments.get(comments.size() - 1).getText();
        } else {
            comment = null;
        }
        if (dueDate != null) {
            Date today = new Date();
            expired = dueDate.before(today);
        } else {
            expired = false;
        }
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DocumentRef getDocRef() {
        return document.getRef();
    }

    @Override
    public Date getDueDate() {
        return dueDate;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDirective() {
        return directive;
    }

    @Override
    public DocumentModel getDocument() {
        return document;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public Task getTask() {
        return task;
    }

}
