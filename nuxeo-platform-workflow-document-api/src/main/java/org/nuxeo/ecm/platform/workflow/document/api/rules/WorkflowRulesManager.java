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
 * $Id: WorkflowRulesManager.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.rules;

import java.io.Serializable;
import java.util.Set;

import org.nuxeo.runtime.model.ComponentName;

/**
 * Workflow rules interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowRulesManager extends Serializable {

    public static final ComponentName NAME = new ComponentName(
                "org.nuxeo.ecm.platform.workflow.document.service.WorkflowRulesService");

    /**
     * Returns allowed workflow definition names for a given document type.
     *
     * @param docType the document type
     * @return a collection of workflow definition names
     */
    Set<String> getAllowedWorkflowDefinitionNamesByDoctype(
            String docType);

    /**
     * Returns allowed workflow definition names for a given path.
     *
     * @param path the base path
     * @return a collection of workflow definition names
     */
    Set<String> getAllowedWorkflowDefinitionNamesByPath(String path);

    /**
     * Returns allowed workflow definition names for a given path.
     *
     * @param path the base path
     * @param docType the document type
     * @return a collection of workflow definition names
     */
    Set<String> getAllowedWorkflowDefinitionNames(String path,
            String docType);

    /**
     * Adds a doc type rule.
     *
     * @param workflowDefinitionId the workflow definition id
     * @param docType the document type
     */
    void addRuleByType(String workflowDefinitionId, String docType);

    /**
     * Adds a doc path rule.
     *
     * @param workflowInstanceId the workflow instance id
     * @param path the document path
     */
    void addRuleByPath(String workflowInstanceId, String path);

    /**
     * Deletes a doc type rule.
     *
     * @param workflowDefinitionId the workflow definition id
     * @param docType the document type
     */
    void delRuleByType(String workflowDefinitionId, String docType);

    /**
     * Deletes a doc path rule.
     *
     * @param workflowDefinitionId the workflow definition id
     * @param path the document path
     */
    void delRuleByPath(String workflowDefinitionId, String path);

}
