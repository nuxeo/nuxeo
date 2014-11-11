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
 * $Id: DashBoardItem.java 23683 2007-08-10 07:25:25Z btatar $
 */

package org.nuxeo.ecm.webapp.dashboard;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Dashboard item.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface DashBoardItem extends Serializable {

    /**
     * Returns the identifier of the dash board item.
     * <p>
     * Workflow task instance right now.
     *
     * @return the identifier of the dash board item.
     */
    String getId();

    /**
     * Returns the name of the DashBoardItem.
     *
     * @return the name of the DashBoardItem
     */
    String getName();

    /**
     * Returns the document reference on which the item is bound.
     *
     * @see org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager
     *
     * @return a document reference instance
     */
    DocumentRef getDocRef();

    /**
     * Returns the document UUID.
     *
     * @return the document UUID
     */
    int getDocUUID();

    /**
     * Returns the description of the item.
     *
     * @return the description of the item
     */
    String getDescription();

    /**
     * Returns the associated item comment.
     *
     * @return the associated item comment
     */
    String getComment();

    /**
     * Returns the date when the task has been started.
     *
     * @return the date when the task has been started
     */
    Date getStartDate();

    /**
     * Returns the date when the task needs to be closed.
     *
     * @return the date when the task needes to be closed
     */
    Date getDueDate();

    /**
     * Returns task priority.
     *
     * @return task priority
     */
    String getPriority();

    /**
     * Returns the doc ref title.
     *
     * @return the doc ref title
     */
    String getDocRefTitle();

    /**
     * Returns the item associated directive.
     *
     * @return the item associated directive
     */
    String getDirective();

    /**
     * Returns the icon path for the doc ref.
     *
     * @return the icon path for the doc ref
     */
    DocumentModel getDocument();

    /**
     * Returns the name of the workflow initiator.
     *
     * @return the name of the workflow initiator
     */
    String getAuthorName();

    /**
     * Returns the type of the workflow.
     *
     * @return the type of the workflow
     */
    String getWorkflowType();

    /**
     * Does the user reach the deadline?
     *
     * @return the expired flag.
     */
    boolean isExpired();

    /**
     * Returns the text of an dashboard item.
     * @return
     */
    String getText();

    /**
     * This method is used to set the schema name and field name which are going to be used to return
     * the text on an dashboard item.
     * @param textUtils
     */
    void setTextUtils(Map<String, String> textUtils);

}
