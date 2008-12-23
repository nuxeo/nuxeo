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
 * $Id: WorkItemsListsManager.java 29625 2008-01-25 14:20:52Z div $
 */

package org.nuxeo.ecm.platform.workflow.document.api.workitem;

import java.io.Serializable;
import java.util.List;

/**
 * Work items list manager.
 * <p>
 * Interface for the service that deals with stored work items lists.
 *
 * @see org.nuxeo.ecm.platform.workflow.document.ejb.WorkItemsListsBean
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkItemsListsManager extends Serializable {

    /**
     * Fetch the entry in the backend given its id.
     *
     * @param entryId the work items list entry identifier
     * @return the work item list entry instance or null if not found.
     */
    WorkItemsListEntry getWorkItemListEntry(int entryId);

    /**
     * Fetch the entry in the backend given its name for a given participant.
     * <p>
     * For a given paticipant, only one name exist for a given list.
     * <p>
     * Returns null if the list does not exist.
     *
     * @return the work item list entry instance or null if not found.
     */
    WorkItemsListEntry getWorkItemListEntryByName(String participantName,
            String name) throws WorkItemsListException;

    /**
     * Return work items entries for a given participant and for a givens
     * process given its name.
     *
     * @param participantName The participant name
     * @param processName The processName
     *
     * @return a list of work items lists
     * @throws WorkItemsListException
     */
    List<WorkItemsListEntry> getWorkItemListsFor(String participantName,
            String processName) throws WorkItemsListException;

    /**
     * Return work items entries for all participants and for a given
     * process given its name.
     *
     * @param processName The processName
     *
     * @return a list of work items lists
     * @throws WorkItemsListException
     */
    List<WorkItemsListEntry> getWorkItemListsForAll(String processName)
             throws WorkItemsListException;

    /**
     * Saves a work items list entry.
     *
     * @param pid the process identifier
     * @param participantName TODO
     * @param name TODO
     * @throws WorkItemsListException
     */
    void saveWorkItemsListFor(String pid, String participantName, String name)
            throws WorkItemsListException;

    /**
     * Saves a work items list entry.
     *
     * @param entry a work items list entry.
     * @throws WorkItemsListException
     */
    void saveWorkItemsList(WorkItemsListEntry entry)
            throws WorkItemsListException;

    /**
     * Removes a work items list entry given its entry id.
     *
     * @param entryId
     * @throws WorkItemsListException
     */
    void deleteWorkItemsListById(int entryId) throws WorkItemsListException;

    /**
     * Restores a work items list on a given process.
     *
     * @param pid the target process identifier
     * @param wiListEntryId the work items list entry id
     * @param merge boolean indicating if old entries should be kept
     * @param start boolean indicating if tasks have to be started
     * @throws WorkItemsListException
     */
    void restoreWorkItemsListFor(String pid, int wiListEntryId, boolean merge,
            boolean start) throws WorkItemsListException;

}
