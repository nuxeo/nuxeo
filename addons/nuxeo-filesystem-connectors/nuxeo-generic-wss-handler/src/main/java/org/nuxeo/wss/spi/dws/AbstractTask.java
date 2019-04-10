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
import java.util.List;

public abstract class AbstractTask extends AbstractDWSItem implements Task {


    public AbstractTask(String id, String authorLogin, Date created,
            Date modified, String fileRef) {
        super(id, authorLogin, created, modified, fileRef);
    }

    protected String assigneeId = "";

    public String getAssigneeRef() {
        return assigneeId + ";#" + getAssigneeLogin();
    }

    public String getDueDateTS() {
        Date date = getDueDate();
        if (date==null) {
            date = new Date(System.currentTimeMillis());
        }
        return getDateFormat().format(date);    }

    public void updateReferences(List<User> users, List<User> assignees) {

        super.updateReferences(users);
        if (assignees!=null) {
            for (int i =0; i< assignees.size(); i++) {
                if (assignees.get(i).getLogin().equals(getAssigneeLogin())) {
                    //assigneeId = ""+ i+1;
                    assigneeId = assignees.get(i).getId();
                    break;
                }
            }
        }
    }

}
