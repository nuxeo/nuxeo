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
 * $Id: WMWorkItemInstance.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;
import java.util.Date;

/**
 * Work item instance.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMWorkItemInstance extends Serializable {

    /**
     * Returns the work item identifier.
     *
     * @return the work item identifier
     */
    String getId();

    /**
     * Returns the name of the work item.
     *
     * @return the name of the work item
     */
    String getName();

    /**
     * Returns the work item description.
     *
     * @return the work item description
     */
    String getDescription();

    /**
     * Returns the corresponding work item definition.
     *
     * @return the corresponding work item definition
     */
    WMWorkItemDefinition getWorkItemDefinition();

    /**
     * Returns the participant.
     *
     * @return a WMParticipant instance
     */
    WMParticipant getParticipant();

    /**
     * Returns the participant username.
     *
     * @return the participant username
     */
    String getParticipantName();

    /**
     * Returns the process instance bound to this work item.
     *
     * @return a WMProcessInstance instance
     */
    WMProcessInstance getProcessInstance();

    /**
     * Returns the work item creation date.
     *
     * @return the work item creation date
     */
    Date getCreationDate();

    /**
     * Returns the work item due date.
     *
     * @return the work item due date
     */
    Date getDueDate();

    /**
     * Returns the work item start date.
     *
     * @return the work item start date
     */
    Date getStartDate();

    /**
     * Gets the work item directive.
     *
     * @return the work item directive
     */
    String getDirective();

    /**
     * Gets the work item stop date.
     *
     * @return the work item stop date
     */
    Date getStopDate();

    /**
     * Has the work item ended?
     *
     * @return true if the work item is blocking, false otherwise
     */
    boolean hasEnded();

    /**
     * Gets the ended flag value.
     * <p>
     * Added for JSF
     *
     * @return the ended flag value
     */
    boolean getEnded();

    /**
     * Is this work item cancelled?
     *
     * @return true if this work item was cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * Gets the cancelled flag.
     *
     * @return the cancelled flag
     */
    boolean getCancelled();

    /**
     * Returns the associated work item comment.
     *
     * @return the associated work item comment
     */
    String getComment();

    /**
     * Returns the work item order.
     *
     * @return the work item order.
     */
    int getOrder();

    /**
     * Has the work item been rejected?
     *
     * @return true if the work item has been rejected, false otherwise
     */
    boolean isRejected();

    /**
     * Gets the rejected flag value.
     * <p>
     * :XXX: JSF use only.
     *
     * @return the rejected flag value
     */
    boolean getRejected();

}
