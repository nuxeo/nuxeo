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
 * $Id: WorkflowDocumentLifeCycleBean.java 20781 2007-06-19 05:58:22Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.util.Collection;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateful;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkflowDocumentLifeCycleLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkflowDocumentLifeCycleRemote;
import org.nuxeo.ecm.platform.workflow.document.api.lifecycle.WorkflowDocumentLifeCycleException;
import org.nuxeo.ecm.platform.workflow.document.api.lifecycle.WorkflowDocumentLifeCycleManager;

/**
 * Workflow document life cycle manager session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateful
@Local(WorkflowDocumentLifeCycleLocal.class)
@Remote(WorkflowDocumentLifeCycleRemote.class)
public class WorkflowDocumentLifeCycleBean extends AbstractWorkflowDocumentManager
        implements WorkflowDocumentLifeCycleManager {

    private static final long serialVersionUID = -7094226148362648041L;

    public WorkflowDocumentLifeCycleBean() {
    }

    public WorkflowDocumentLifeCycleBean(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

    public boolean followTransition(DocumentRef docRef, String transition)
            throws WorkflowDocumentLifeCycleException {
        boolean res = false;
        try {
            res = getDocumentManager().followTransition(docRef, transition);
            getDocumentManager().save();
        } catch (ClientException ce) {
            throw new WorkflowDocumentLifeCycleException(ce);
        }
        return res;
    }

    public Collection<String> getAllowedStateTransitions(DocumentRef docRef)
            throws WorkflowDocumentLifeCycleException {
        Collection<String> allowedStateTransitions;
        try {
            allowedStateTransitions = getDocumentManager().getAllowedStateTransitions(
                    docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentLifeCycleException(ce);
        }
        return allowedStateTransitions;
    }

    public String getCurrentLifeCycleState(DocumentRef docRef)
            throws WorkflowDocumentLifeCycleException {
        String currentLifeCycleState;
        try {
            currentLifeCycleState = getDocumentManager().getCurrentLifeCycleState(
                    docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentLifeCycleException(ce);
        }
        return currentLifeCycleState;
    }

    public String getLifeCyclePolicy(DocumentRef docRef)
            throws WorkflowDocumentLifeCycleException {
        String lifeCyclePolicy;
        try {
            lifeCyclePolicy = getDocumentManager().getLifeCyclePolicy(docRef);
        } catch (ClientException ce) {
            throw new WorkflowDocumentLifeCycleException(ce);
        }
        return lifeCyclePolicy;
    }

}
