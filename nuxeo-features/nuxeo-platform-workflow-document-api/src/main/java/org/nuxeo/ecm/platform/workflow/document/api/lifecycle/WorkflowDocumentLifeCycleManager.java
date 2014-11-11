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
 * $Id: WorkflowDocumentLifeCycleManager.java 7904 2006-12-13 15:12:33Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.api.lifecycle;

import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.document.api.BaseWorkflowDocumentManager;

/**
 * Workflow document life cycle manager.
 * <p>
 * Deals with document life cycle using a dedicated document manager. Useful
 * since the backing document manager will be authenticated with another
 * participant than the caller participant. Allow more flexibility regarding
 * security prerequisites.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowDocumentLifeCycleManager extends
        BaseWorkflowDocumentManager {

    /**
     * Returns the life cycle of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @param docRef the document reference
     * @return the life cycle as a string
     * @throws WorkflowDocumentLifeCycleException
     */
    String getCurrentLifeCycleState(DocumentRef docRef)
            throws WorkflowDocumentLifeCycleException;

    /**
     * Returns the life cycle policy of the document.
     *
     * @see org.nuxeo.ecm.core.lifecycle
     *
     * @param docRef the document reference
     * @return the life cycle policy
     * @throws WorkflowDocumentLifeCycleException
     */
    String getLifeCyclePolicy(DocumentRef docRef)
            throws WorkflowDocumentLifeCycleException;

    /**
     * Follows a given life cycle transition.
     * <p>
     * This will update the current life cycle of the document.
     *
     * @parem docRef the document reference
     * @param transition the name of the transition to follow
     * @return a boolean representing the status if the operation
     * @throws WorkflowDocumentLifeCycleException
     */
    boolean followTransition(DocumentRef docRef, String transition)
            throws WorkflowDocumentLifeCycleException;

    /**
     * Gets the allowed state transitions for this document.
     *
     * @param docRef the document reference
     * @return a collection of state transitions as string
     */
    Collection<String> getAllowedStateTransitions(DocumentRef docRef)
            throws WorkflowDocumentLifeCycleException;

}
