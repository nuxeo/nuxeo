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
 * $Id: WorkItemsListsActions.java 24240 2007-08-24 16:43:34Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.web.api;

import java.io.Serializable;
import java.util.List;

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;

/**
 * Work items lists actions session bean business interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkItemsListsActions extends Serializable {

    /**
     * Invalidates the work items lists map.
     * <p>
     * Aimed at being used with Seam <i>Observer</i> annotation.
     *
     * @throws WorkItemsListException
     */
    void invalidateWorkItemsListsMap() throws WorkItemsListException;

    /**
     * Computes the work items lists map.
     * <p>
     * Aimed at being used with Seam <i>Factory</i> annotation.
     *
     * @throws WorkItemsListException
     */
    List<SelectItem> computeWorkItemsListsMap() throws WorkItemsListException;

    /**
     * Creates a work items list.
     * <p>
     * Expecting parameters from the UI XXXX
     *
     * @return a string for the redirection
     */
    String createWorkItemsList() throws WorkItemsListException;

    /**
     * Deletes a work items list.
     * <p>
     * Expecting parameters from the UI XXXX
     *
     * @return a string for the redirection
     */
    String deleteWorkItemsList() throws WorkItemsListException;

    /**
     * Loads a work items list removing old work items.
     * <p>
     * It assumes a work items list id and processName in the context.
     *
     * @return a string for the redirection
     */
    String loadWorkItemsList() throws WorkItemsListException;

    /**
     * Loads a work items list keeping old work items.
     * <p>
     * It assumes a work items list id and processName in the context.
     *
     * @return a string for the redirection
     */
    String loadWorkItemsListMerging() throws WorkItemsListException;

    /**
     * Returns the new work item list name.
     *
     * @return the new work item list name.
     */
    String getNewWorkItemsListName();

    /**
     * Sets the new work items list name.
     *
     * @param newWorkItemsListName : the name of the new work items list name.
     */
    void setNewWorkItemsListName(String newWorkItemsListName);

    /**
     * Returns the name of the selected work items list.
     *
     * @return the name of the selected work items list.
     */
    String getWorkItemsListsEntrySelectionName();

    /**
     * Sets the name of the selected work items list.
     *
     * @param name : the name of the selected work items list.
     */
    void setWorkItemsListsEntrySelectionName(String name);

}
