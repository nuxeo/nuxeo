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
 *     bchaffangeon
 *
 * $Id: TaskModuleImpl.java 25373 2007-09-25 08:03:06Z sfermigier $
 */

package org.nuxeo.ecm.platform.syndication.workflow;

import java.util.Date;

import com.sun.syndication.feed.module.ModuleImpl;

/**
 * @author bchaffangeon
 */
@SuppressWarnings("serial")
public class TaskModuleImpl extends ModuleImpl implements TaskModule {

    private Date dueDate;

    private Date startDate;

    private String directive;

    private String description;

    private String name;

    private String comment;

    public TaskModuleImpl() {
        super(TaskModule.class, URI);
    }

    public String getDirective() {
        return directive;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setDirective(String directive) {
        this.directive = directive;
    }

    public void setDueDate(Date date) {
        dueDate = date;
    }

    public void setStartDate(Date date) {
        startDate = date;
    }

    public void copyFrom(Object obj) {
        TaskModule tm = (TaskModule) obj;
        dueDate = (Date) tm.getDueDate().clone();
        startDate = (Date) tm.getStartDate().clone();
        directive = tm.getDirective();
    }

    public Class<TaskModule> getInterface() {
        return TaskModule.class;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
