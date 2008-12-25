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

package org.nuxeo.ecm.webapp.dashboard;

import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;

/**
 * Dashboard item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class DashBoardItemImpl implements DashBoardItem {

    private static final long serialVersionUID = 919752175741886376L;

    private final String name;

    private final String id;

    private final String description;

    private final String comment;

    private final Date startDate;

    private final Date dueDate;

    private final String directive;

    private final DocumentModel docModel;

    private final String docRefTitle;

    // XXX: used?
    private String priority;

    // XXX: used?
    private int docUUID;

    private String authorName;

    private String workflowType;

    private boolean expired;

    private String text;

    private Map<String, String> textUtils;


    public DashBoardItemImpl(WMWorkItemInstance wfTaskInstance,
            DocumentModel docModel, String docRefTitle) {
        this.docModel = docModel;
        this.docRefTitle = docRefTitle;
        id = wfTaskInstance.getId();
        name = wfTaskInstance.getName();
        description = wfTaskInstance.getDescription();
        dueDate = wfTaskInstance.getDueDate();
        startDate = wfTaskInstance.getStartDate();
        directive = wfTaskInstance.getDirective();
        comment = wfTaskInstance.getComment();
        if (dueDate != null) {
            Date today = new Date();
            expired = dueDate.before(today);
        }
    }

    public String getComment() {
        return comment;
    }

    public String getDescription() {
        return description;
    }

    public DocumentRef getDocRef() {
        return docModel.getRef();
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getId() {
        return id;
    }

    public String getPriority() {
        return priority;
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

    public String getDocRefTitle() {
        return docRefTitle;
    }

    public int getDocUUID() {
        return docUUID;
    }

    public DocumentModel getDocument() {
        return docModel;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }

    public String getText() {
        String schemaName = textUtils.keySet().toArray(new String[0])[0];
        String field = textUtils.get(schemaName);
        try {
            text = (String) docModel.getProperty(schemaName, field);
        } catch (ClientException e) {
            text = null;
        }
        if (text != null) {
            String[] lines = text.split("\n");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lines.length && i < 5; i++) {
                if (lines[i].length() > 0) {
                    result.append(lines[i]).append("<br />");
                }
            }
            text = result.toString();
        }
        return text != null ? text : "";
    }

    public void setTextUtils(Map<String, String> textUtils) {
        this.textUtils = textUtils;
    }

}
