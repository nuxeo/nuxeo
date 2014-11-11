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
package org.nuxeo.ecm.platform.jbpm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService.VariableName;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract Class providing helpers methods to manipulates document.
 *
 * @author arussel
 *
 */
public abstract class AbstractJbpmHandlerHelper implements ActionHandler,
        AssignmentHandler, DecisionHandler, TaskControllerHandler {

    private static final long serialVersionUID = 1L;

    protected transient JbpmService jbpmService;

    protected ExecutionContext executionContext;

    public void execute(ExecutionContext executionContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void assign(Assignable assignable, ExecutionContext executionContext)
            throws Exception {
        throw new UnsupportedOperationException();
    }

    public String decide(ExecutionContext executionContext) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void initializeTaskVariables(TaskInstance taskInstance,
            ContextInstance contextInstance, Token token) {
        throw new UnsupportedOperationException();
    }

    public void submitTaskVariables(TaskInstance taskInstance,
            ContextInstance contextInstance, Token token) {
        throw new UnsupportedOperationException();
    }

    public JbpmService getJbpmService() throws Exception {
        if (jbpmService == null) {
            jbpmService = Framework.getService(JbpmService.class);
        }
        return jbpmService;
    }

    // get standard variables values

    protected String getDocumentRepositoryName() throws Exception {
        return getStringVariable(VariableName.documentRepositoryName.name());
    }

    protected String getDocumentId() throws Exception {
        return getStringVariable(VariableName.documentId.name());
    }

    protected DocumentRef getDocumentRef() throws Exception {
        return new IdRef(getDocumentId());
    }

    protected String getInitiator() throws Exception {
        return getSwimlaneUser(VariableName.initiator.name());
    }

    protected String getEndLifecycleTransition() throws Exception {
        return getStringVariable(VariableName.endLifecycleTransition.name());
    }

    @SuppressWarnings("unchecked")
    protected List<String> getParticipants() throws Exception {
        return (List<String>) executionContext.getContextInstance().getVariable(
                VariableName.participants.name());
    }

    protected CoreSession getCoreSession(NuxeoPrincipal principal)
            throws Exception {
        String repositoryName = getDocumentRepositoryName();
        Map<String, Serializable> context = new HashMap<String, Serializable>();
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

    protected void followTransition(NuxeoPrincipal principal,
            DocumentRef docRef, String transition) throws Exception {
        CoreSession coreSession = getCoreSession(principal);
        coreSession.followTransition(docRef, transition);
        coreSession.save();
        closeCoreSession(coreSession);
    }

    protected String getStringVariable(String name) {
        return (String) executionContext.getContextInstance().getVariable(name);
    }

    protected boolean nuxeoHasStarted() throws Exception {
        return Framework.getRuntime() != null;
    }

    protected String getACLName() {
        Long pid = executionContext.getProcessInstance().getId();
        return getProcessACLName(pid);
    }

    public static String getProcessACLName(Long pid) {
        return JbpmService.ACL_PREFIX + pid;
    }

    public Object getTransientVariable(String name) {
        return executionContext.getContextInstance().getTransientVariable(name);
    }
}
