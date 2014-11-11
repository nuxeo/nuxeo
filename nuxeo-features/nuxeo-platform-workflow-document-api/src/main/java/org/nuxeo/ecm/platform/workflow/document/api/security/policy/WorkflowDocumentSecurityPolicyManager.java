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
 * $Id: WorkflowDocumentSecurityPolicyManager.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.security.policy;

import java.io.Serializable;

import org.nuxeo.runtime.model.ComponentName;


/**
 * Workflow document rights policy manager.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowDocumentSecurityPolicyManager extends Serializable {

    public static final ComponentName NAME = new ComponentName(
                "org.nuxeo.ecm.platform.workflow.document.service.WorkflowDocumentSecurityPolicyService");

    /**
     * Returns the workflow document security policy given it's name.
     *
     * @param name the name of the workflow document rights policy
     * @return the corresponding WorkflowDocumentSecurityPolicy instance or null
     *         if not found
     */
    WorkflowDocumentSecurityPolicy getWorkflowDocumentSecurityPolicyByName(
            String name);

    /**
     * Returns the workflow document security policy for a given workflow name.
     *
     * @param workflowName the workflow name
     * @return the corresponding WorkflowDocumentSecurityPolicy instance or null
     *         if workflow name not found
     */
    WorkflowDocumentSecurityPolicy getWorkflowDocumentSecurityPolicyFor(
            String workflowName);

}
