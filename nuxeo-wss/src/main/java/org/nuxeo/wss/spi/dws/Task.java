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

public interface Task extends DWSItem {

    String getTitle();

    String getAssigneeLogin();

    String getAssigneeRef();

    String getPriority();

    Date getDueDate();

    String getDueDateTS();

    String getBody();

    String getStatus();

    void updateReferences(List<User> users, List<User> assignees);

}
