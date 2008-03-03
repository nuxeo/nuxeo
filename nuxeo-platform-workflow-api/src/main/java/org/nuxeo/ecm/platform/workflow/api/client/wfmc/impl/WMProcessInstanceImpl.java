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
 * $Id: WMProcessInstanceImpl.java 28463 2008-01-03 18:02:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import java.util.Date;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;

/**
 * Process instance implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMProcessInstanceImpl implements WMProcessInstance {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected WMProcessDefinition processDefinition;

    protected String status;

    protected Date started;

    protected Date stopped;

    protected WMParticipant author;

    protected String authorName;

    public WMProcessInstanceImpl() {
    }

    public WMProcessInstanceImpl(String id,
            WMProcessDefinition processDefinition, String status, Date started,
            Date stopped, WMParticipant creator) {
        this.id = id;
        this.processDefinition = processDefinition;
        this.started = started;
        this.stopped = stopped;
        this.author = creator;
        if (creator != null) {
            authorName = creator.getName();
        }
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public WMProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public Date getStartDate() {
        return started;
    }

    public Date getStopDate() {
        return stopped;
    }

    public WMParticipant getAuthor() {
        return author;
    }

    public String getState() {
        return status;
    }

    public void setState(String state) {
        status = state;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getName() {
        return processDefinition.getName();
    }

}
