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
 * $Id: WorkflowDocumentVersioningPolicyManager.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.versioning;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;

/**
 * Workflow vesioning policy interface
 * <p>
 * Exposes an API allowing third party code to query for the versioning policy
 * the underlying workflow applies.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowDocumentVersioningPolicyManager extends Serializable {

    /**
     * Return the list of all workflow versioning policies.
     *
     * @see org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants
     *
     * @return the list of all workflow versioning policies.
     */
    String[] getWorkflowVersioningPolicyIds();

    /**
     * Return the workflow versioning policy applied by the workflow on a given
     * document;
     * <p>
     * If no versioning policy or no worklfow are associated to the given
     * document ref then we will return null
     * <p>
     * The workflow component defines the workflow related versioning policy
     * constants.
     *
     * See
     * org.nuxeo.ecm.platform.workflow.document.api.WorkflowVersioningPolicyManager
     *
     * @param docRef :
     *            the document core reference
     * @return a constants reprensenting the versioning policy
     */
    String getVersioningPolicyFor(DocumentRef docRef)
            throws WMWorkflowException;

}
