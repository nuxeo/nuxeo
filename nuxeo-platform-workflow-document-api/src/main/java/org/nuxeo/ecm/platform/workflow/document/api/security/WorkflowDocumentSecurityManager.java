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
 * $Id: WorkflowDocumentSecurityManager.java 19560 2007-05-28 17:54:38Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.api.security;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.platform.workflow.document.api.BaseWorkflowDocumentManager;

/**
 * Workflow Security manager.
 *
 * <p>
 * Deals with a specific workflow ACL.
 * <p>
 * Each workflow instance will have a dedicated ACL at their disposal so that
 * several workflows can be bound to a given document and ran in a concurrent
 * manner.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowDocumentSecurityManager extends
        BaseWorkflowDocumentManager {

    /**
     * Returns the ACL name for a given process instance.
     *
     * @param pid the process instance identifier
     * @return an ACL instance
     */
    String getACLNameFor(String pid) throws WorkflowDocumentSecurityException;

    /**
     * Returns the workflow-specific ACL for a given process identifier.
     * <p>
     * If the ACL or ACP doesn't exist it will return a null value.
     *
     * @param docRef the document reference
     * @param pid the process identifier
     * @return an ACL instance
     */
    ACL getACL(DocumentRef docRef, String pid)
            throws WorkflowDocumentSecurityException;

    /**
     * Removes the workflow ACL for a given process identifier.
     *
     * @param docRef the docuement reference
     * @param pid the process identifier
     */
    void removeACL(DocumentRef docRef, String pid)
            throws WorkflowDocumentSecurityException;

    /**
     * Grants a participant.
     *
     * @param docRef the document holding the WF ACP
     * @param participantName the participant username
     * @param perm the permission to grant to username
     * @param pid the process instance identifier (used to compute the ACL name)
     */
    void grantPrincipal(DocumentRef docRef, String principalName, String perm,
            String pid) throws WorkflowDocumentSecurityException;

    /**
     * Denies a participant.
     *
     * @param docRef the document holding the WF ACP
     * @param participantName the participant username
     * @param perm the permission to be removed to the participant
     * @param pid the process instance identifier (used to compute the ACL name)
     */
    void denyPrincipal(DocumentRef docRef, String principalName, String perm,
            String pid) throws WorkflowDocumentSecurityException;

    /**
     * Sets rules.
     *
     * @param docRef the document holding the WF ACP
     * @param userEntries a list of user entries
     * @param pid the process instance identifier (Used to compute the ACL name)
     */
    void setRules(DocumentRef docRef, List<UserEntry> userEntries, String pid)
            throws WorkflowDocumentSecurityException;
}
