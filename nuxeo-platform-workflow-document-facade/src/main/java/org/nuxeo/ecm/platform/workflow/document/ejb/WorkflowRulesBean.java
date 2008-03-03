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
 * $Id: WorkflowRulesBean.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.platform.workflow.document.NXWorkflowDocument;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkflowRulesLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkflowRulesRemote;
import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;

/**
 * Workflow rules session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(WorkflowRulesLocal.class)
@Remote(WorkflowRulesRemote.class)
public class WorkflowRulesBean implements WorkflowRulesManager {

    private static final long serialVersionUID = -4275541305883730194L;

    protected WorkflowRulesManager getCoreService() {
        return NXWorkflowDocument.getWorkflowRulesService();
    }

    public void addRuleByPath(String workflowDefinitionId, String path) {
        getCoreService().addRuleByPath(workflowDefinitionId, path);
    }

    public void addRuleByType(String workflowDefinitionId, String docType) {
        getCoreService().addRuleByType(workflowDefinitionId, docType);
    }

    public void delRuleByPath(String workflowDefinitionId, String path) {
        getCoreService().delRuleByPath(workflowDefinitionId, path);
    }

    public void delRuleByType(String workflowDefinitionId, String docType) {
        getCoreService().delRuleByType(workflowDefinitionId, docType);
    }

    public Set<String> getAllowedWorkflowDefinitionNames(String path,
            String docType) {
        return getCoreService().getAllowedWorkflowDefinitionNames(path, docType);
    }

    public Set<String> getAllowedWorkflowDefinitionNamesByDoctype(String docType) {
        return getCoreService().getAllowedWorkflowDefinitionNamesByDoctype(
                docType);
    }

    public Set<String> getAllowedWorkflowDefinitionNamesByPath(String path) {
        return getCoreService().getAllowedWorkflowDefinitionNamesByPath(path);
    }

}
