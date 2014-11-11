/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi.dws;

import java.util.Date;

public class TaskImpl extends AbstractTask implements Task {

    protected String assignee;
    protected String body;
    protected String title;
    protected Date dueDate;
    protected String priority;
    protected String status;

    public TaskImpl(String id, String authorLogin, Date created, Date modified,
            String fileRef) {
        super(id, authorLogin, created, modified, fileRef);
    }

    public void setTaskData(String assignee, String title, String body, Date dueDate, String priority, String status) {
        this.assignee=assignee;
        this.title=title;
        this.body=body;
        this.dueDate=dueDate;
        this.priority = priority;
        this.status=status;
    }

    public String getAssigneeLogin() {
        return assignee;
    }

    public String getBody() {
        return body;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

}
