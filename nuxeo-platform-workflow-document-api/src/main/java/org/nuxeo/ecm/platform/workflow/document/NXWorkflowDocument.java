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
 * $Id: NXWorkflowDocument.java 21445 2007-06-26 14:47:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document;

import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Facade for services provided by NXWorkflowDocument module.
 * <p>
 * This is the main entry point to the workflow services.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXWorkflowDocument {

    private NXWorkflowDocument() {
    }

    /**
     * Returns the workflow rules service.
     *
     * @see org.nuxeo.ecm.plarform.ec.workflow.model.WorkflowService
     *
     * @return the workflow rules service.
     * @deprecated use {@link Framework#getService(Class)}
     */
    @Deprecated
    public static WorkflowRulesManager getWorkflowRulesService() {
        return (WorkflowRulesManager) Framework.getRuntime().getComponent(
                WorkflowRulesManager.NAME);
    }

    /**
     * Returns the workflow document rights policy service.
     *
     * @return the workflow document rights policy service
     * @deprecated use {@link Framework#getService(Class)}
     */
    @Deprecated
    public static WorkflowDocumentSecurityPolicyManager getWorkflowDocumentRightsPolicyService() {
        return (WorkflowDocumentSecurityPolicyManager) Framework.getRuntime()
                .getComponent(WorkflowDocumentSecurityPolicyManager.NAME);
    }

}
