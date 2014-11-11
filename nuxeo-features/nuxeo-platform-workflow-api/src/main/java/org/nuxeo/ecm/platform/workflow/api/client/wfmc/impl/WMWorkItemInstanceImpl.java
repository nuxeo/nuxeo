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
 * $Id: WMWorkItemInstanceImpl.java 21821 2007-07-03 08:59:32Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import java.util.Date;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;

/**
 * Work item instance implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMWorkItemInstanceImpl implements WMWorkItemInstance {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected String name;

    protected String description;

    protected WMProcessInstance processInstance;

    protected WMWorkItemDefinition workItemDefinition;

    protected WMParticipant participant;

    protected String participantName;

    protected Date creationDate;

    protected Date startDate;

    protected Date stopDate;

    protected Date dueDate;

    protected String directive;

    protected boolean ended;

    protected boolean cancelled;

    protected boolean rejected;

    protected int order;

    protected String comment;

    public WMWorkItemInstanceImpl() {
    }

    public WMWorkItemInstanceImpl(String id) {
        this.id = id;
    }

    public WMWorkItemInstanceImpl(String id, int order) {
        this(id);
        this.order = order;
    }

    public WMWorkItemInstanceImpl(String id, WMParticipant participant,
            int order, boolean rejected, boolean ended, boolean cancelled) {
        this(id, order);
        this.participant = participant;
        if (participant != null) {
            this.participantName = participant.getName();
        }
        this.rejected = rejected;
        this.ended = ended;
        this.cancelled = cancelled;
    }

    public WMWorkItemInstanceImpl(String id, String name, String description,
            WMWorkItemDefinition workItemDefinition, WMParticipant participant,
            WMProcessInstance processInstance, Date startDate, Date stopDate,
            Date dueDate, String directive, boolean cancelled, String comment,
            int order, boolean rejected, Date creationDate) {
        this(id, participant, order, rejected, stopDate !=  null, cancelled);
        this.name = name;
        this.description = description;
        this.processInstance = processInstance;
        this.workItemDefinition = workItemDefinition;
        this.startDate = startDate;
        this.stopDate = stopDate;
        this.dueDate = dueDate;
        this.directive = directive;
        this.comment = comment;
        this.creationDate = creationDate;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WMWorkItemDefinition getWorkItemDefinition() {
        return workItemDefinition;
    }

    public WMParticipant getParticipant() {
        return participant;
    }

    public WMProcessInstance getProcessInstance() {
        return processInstance;
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

    public String getParticipantName() {
        return participantName;
    }

    public boolean hasEnded() {
        return ended;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public boolean getEnded() {
        return ended;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean getCancelled() {
        return cancelled;
    }

    public String getComment() {
        return comment;
    }

    public int getOrder() {
        return order;
    }

    public boolean getRejected() {
        return rejected;
    }

    public boolean isRejected() {
        return rejected;
    }

    public Date getCreationDate() {
        return creationDate;
    }

}
