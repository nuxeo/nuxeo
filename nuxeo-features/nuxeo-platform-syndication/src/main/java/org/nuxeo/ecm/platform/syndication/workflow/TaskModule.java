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
 * $Id: TaskModule.java 25373 2007-09-25 08:03:06Z sfermigier $
 */

package org.nuxeo.ecm.platform.syndication.workflow;

import java.util.Date;

import com.sun.syndication.feed.module.Module;

public interface TaskModule extends Module {

    String URI = "http://nuxeo.org/module/task/1.0";

    Date getDueDate();

    void setDueDate(Date date);

    Date getStartDate();

    void setStartDate(Date date);

    String getDirective();

    void setDirective(String directive);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    String getComment();

    void setComment(String comment);

}
