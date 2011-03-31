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
 * $Id: DashBoardItemImpl.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.jbpm.dashboard;

import java.util.Date;
import java.util.List;

import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.jbpm.JbpmService.TaskVariableName;

/**
 * Dashboard item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class DashBoardItemImpl implements DashBoardItem {

    private static final long serialVersionUID = 919752175741886376L;

    protected final Long id;

    protected final String name;

    protected final String description;

    protected final Date startDate;

    protected final Date dueDate;

    protected final boolean expired;

    protected final String directive;

    protected final DocumentModel document;

    protected final TaskInstance task;

    protected String comment;

    @SuppressWarnings("unchecked")
    public DashBoardItemImpl(TaskInstance task, DocumentModel document) {
        this.task = task;
        this.document = document;
        id = Long.valueOf(task.getId());
        name = task.getName();
        description = task.getDescription();
        dueDate = task.getDueDate();
        startDate = task.getCreate();
        directive = (String) task.getVariableLocally(TaskVariableName.directive.name());
        List<Comment> comments = task.getComments();
        if (comments != null && !comments.isEmpty()) {
            comment = comments.get(comments.size() - 1).getMessage();
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

    public String getComment() {
        return comment;
    }

    public String getDescription() {
        return description;
    }

    public DocumentRef getDocRef() {
        return document.getRef();
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Long getId() {
        return id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getName() {
        return name;
    }

    public String getDirective() {
        return directive;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public boolean isExpired() {
        return expired;
    }

    public TaskInstance getTaskInstance() {
        return task;
    }

}
