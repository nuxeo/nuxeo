/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: WorkItemEntry.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.workitem;

import java.io.Serializable;
import java.util.Date;

/**
 * Work item entry.
 *
 * <p/>
 *
 * A work item entry is bound to one and only one work items list entry. It
 * holds basic properties of a work item so that it can be restored on a
 * running compatible process instance.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkItemEntry extends Serializable {

    /**
     * Returns the entry id.
     *
     * @return the entry id
     */
    int getEntryId();

    /**
     * Returns the work item name.
     *
     * @return the work item name
     */
    String getWiName();

    /**
     * Returns the work item order.
     *
     * @return the work item order
     */
    int getWiOrder();

    /**
     * Returns the work item directive.
     *
     * @return the work item directive
     */
    String getWiDirective();

    /**
     * Returns the work item participant.
     *
     * @return the work item participant
     */
    String getWiParticipant();

    /**
     * Returns the work item due date.
     *
     * @return the work item due date
     */
    Date getWiDueDate();

    /**
     * Returns the work item comment.
     *
     * @return the work item comment
     */
    String getWiComment();

}
