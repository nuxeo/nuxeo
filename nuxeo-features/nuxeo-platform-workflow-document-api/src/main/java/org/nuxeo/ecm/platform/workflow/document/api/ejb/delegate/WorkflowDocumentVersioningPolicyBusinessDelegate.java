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
 * $Id:ContentHistoryBusinessDelegate.java 3895 2006-10-11 19:12:47Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Workflow versioning policy business delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkflowDocumentVersioningPolicyBusinessDelegate implements
        Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WorkflowDocumentVersioningPolicyBusinessDelegate.class);

    protected WorkflowDocumentVersioningPolicyManager workflowVersioningPolicy;

    public WorkflowDocumentVersioningPolicyManager getWorkflowVersioningPolicy()
            throws Exception {
        log.debug("getWorkflowVersioningPolicy()");
        if (workflowVersioningPolicy == null) {
            workflowVersioningPolicy = Framework.getService(WorkflowDocumentVersioningPolicyManager.class);
        }
        log.debug("WorkflowDocumentVersioningPolicyManager bean found :"
                + workflowVersioningPolicy.getClass().toString());
        return workflowVersioningPolicy;
    }

}
