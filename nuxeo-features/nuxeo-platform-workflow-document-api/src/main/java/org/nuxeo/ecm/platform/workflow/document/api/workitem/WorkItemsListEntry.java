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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.document.api.workitem;

import java.io.Serializable;

/**
 * List of work items.
 * <p>
 * Work items lists are used to store work items along with their properties
 * (Directive, order, Name, assignee, etc...).
 * <p>
 * A work item is bound to the user and a given process name on which the
 * work item list is valid and thus usable.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkItemsListEntry extends Serializable {

    /**
     * Returns the entry identifier.
     *
     * @return the entry identifier
     */
    int getEntryId();

    /**
     * Returns the process name for which the work items list is valid.
     *
     * @return the process name for which the work items list is valid
     */
    String getProcessName();

    /**
     * Returns the name of the participant who saved this list.
     *
     * @return the name of the participant who saved this list
     */
    String getParticipantName();

    /**
     * Returns the name of the work items list.
     *
     * @return the name of the work items list
     */
    String getName();

}
