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
 * $Id: WorkflowDocumentSecurityPolicyBusinessDelegate.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Workflow document rights policy business delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkflowDocumentSecurityPolicyBusinessDelegate implements
        Serializable {

    private static final long serialVersionUID = -7424773431222737772L;

    private static final Log log = LogFactory.getLog(WorkflowDocumentRelationBusinessDelegate.class);

    protected WorkflowDocumentSecurityPolicyManager wDocRightsPolicyBean;

    public WorkflowDocumentSecurityPolicyManager getWorkflowDocumentRightsPolicyManager()
            throws Exception {
        log.debug("getWorkflowDocument()");
        if (wDocRightsPolicyBean == null) {
            wDocRightsPolicyBean = Framework.getService(WorkflowDocumentSecurityPolicyManager.class);
        }
        log.debug("WorkflowDocumentRelationManager bean found :"
                + wDocRightsPolicyBean.getClass().toString());
        return wDocRightsPolicyBean;
    }

}
