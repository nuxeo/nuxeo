/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public abstract class AbstractJbpmHandlerHelper implements ActionHandler,
        AssignmentHandler, DecisionHandler {

    private static final long serialVersionUID = 1L;

    protected transient JbpmService jbpmService;

    protected ExecutionContext executionContext;

    public void execute(ExecutionContext executionContext) throws Exception {
    }

    public void assign(Assignable assignable, ExecutionContext executionContext)
            throws Exception {
    }

    public String decide(ExecutionContext executionContext) throws Exception {
        return "";
    }

    public JbpmService getJbpmService() throws Exception {
        if (jbpmService == null) {
            jbpmService = Framework.getService(JbpmService.class);
        }
        return jbpmService;
    }

    /**
     * @param user
     * @return
     * @throws Exception
     */
    protected CoreSession getCoreSession(String user) throws Exception {
        String repositoryName = (String) executionContext.getContextInstance().getVariable(
                JbpmService.VariableName.documentRepositoryName.name());
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(user);
        context.put("principal", principal);
        try {
            return CoreInstance.getInstance().open(repositoryName, context);
        } catch (ClientException e) {
            throw new NuxeoJbpmException(e);
        }
    }

    protected String getSwimlaneUser(String swimlaneName) {
        return executionContext.getTaskMgmtInstance().getSwimlaneInstance(
                swimlaneName).getActorId();
    }

    protected void closeCoreSession(CoreSession session) {
        CoreInstance.getInstance().close(session);
    }

    protected void followTransition(String user, String transition)
            throws Exception {
        CoreSession coreSession = getCoreSession(user);
        String docId = (String) executionContext.getContextInstance().getVariable(
                JbpmService.VariableName.documentId.name());
        DocumentRef docRef = new IdRef(docId);
        DocumentModel model = coreSession.getDocument(docRef);
        model.followTransition(transition);
        coreSession.saveDocument(model);
        coreSession.save();
    }

    protected String getStringVariable(String name) {
        return (String) executionContext.getContextInstance().getVariable(name);
    }

    protected boolean nuxeoHasStarted() throws Exception {
        return Framework.getRuntime() != null;
    }
}
