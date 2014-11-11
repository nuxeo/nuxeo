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
 * $Id: WorkflowDocumentSecurityPolicyBean.java 22225 2007-07-10 11:24:41Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.platform.workflow.document.NXWorkflowDocument;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkflowDocumentSecurityPolicyLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkflowDocumentSecurityPolicyRemote;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;

/**
 * Workflow document security policy session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Remote(WorkflowDocumentSecurityPolicyRemote.class)
@Local(WorkflowDocumentSecurityPolicyLocal.class)
public class WorkflowDocumentSecurityPolicyBean implements
        WorkflowDocumentSecurityPolicyManager {

    private static final long serialVersionUID = -6777616544968818675L;

    protected WorkflowDocumentSecurityPolicyManager getService() {
        return NXWorkflowDocument.getWorkflowDocumentRightsPolicyService();
    }

    public WorkflowDocumentSecurityPolicy getWorkflowDocumentSecurityPolicyByName(
            String name) {
        return getService().getWorkflowDocumentSecurityPolicyByName(name);
    }

    public WorkflowDocumentSecurityPolicy getWorkflowDocumentSecurityPolicyFor(
            String workflowName) {
        return getService().getWorkflowDocumentSecurityPolicyFor(workflowName);
    }

}
