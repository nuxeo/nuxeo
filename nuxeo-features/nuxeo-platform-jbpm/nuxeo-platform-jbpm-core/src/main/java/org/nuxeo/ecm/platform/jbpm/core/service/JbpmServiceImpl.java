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
package org.nuxeo.ecm.platform.jbpm.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.jbpm.JbpmActorsListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmRuntimeException;
import org.nuxeo.ecm.platform.jbpm.ProcessStartDateComparator;
import org.nuxeo.ecm.platform.jbpm.TaskCreateDateComparator;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 */
public class JbpmServiceImpl implements JbpmService {

    private static final Log log = LogFactory.getLog(JbpmServiceImpl.class);

    protected Map<String, List<String>> typeFilters;

    private EventProducer eventProducer;

    private JbpmConfiguration configuration;

    private UserManager userManager;

    public static final ThreadLocal<JbpmContext> contexts = new ThreadLocal<JbpmContext>();

    private final Map<String, JbpmSecurityPolicy> securityPolicies = new HashMap<String, JbpmSecurityPolicy>();

    @Override
    public Serializable executeJbpmOperation(JbpmOperation operation)
            throws NuxeoJbpmException {
        JbpmContext context = getContext();
        Serializable result = null;
        try {
            result = operation.run(context);
        } finally {
            if (isTransactionEnabled(context)) {
                context.close();
            }
        }
        return result;
    }

    // 2 situations:
    // - first, you have an outside transaction (then isTransactionEnable is
    // false), you open the context
    // on first call of the thread and close it when the transaction is
    // commited.
    // - second, you don't have an outside transaction, jbpm uses a
    // jdbctransaction and isTransactionEnable is true.
    // we open and close the context for each call.
    protected JbpmContext getContext() {
        JbpmContext context = contexts.get();
        if (context == null || !context.getSession().isConnected()) {
            context = configuration.createJbpmContext();
            if (!isTransactionEnabled(context)) {
                contexts.set(context);
                // context will be closed by the jbpm synchronization
                context.getSession().getTransaction().registerSynchronization(
                        new JbpmSynchronization(context));
            }
        }
        return context;
    }

    @Override
    public JbpmConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isTransactionEnabled(JbpmContext context) {
        DbPersistenceServiceFactory factory = ((DbPersistenceServiceFactory) context.getServiceFactory(Services.SERVICENAME_PERSISTENCE));
        return factory.isTransactionEnabled();
    }

    protected void setConfiguration(JbpmConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskInstance> getCurrentTaskInstances(
            final NuxeoPrincipal currentUser, final JbpmListFilter filter)
            throws NuxeoJbpmException {
        return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<TaskInstance> run(JbpmContext context)
                    throws NuxeoJbpmException {
                return getCurrentTaskInstancesInternal(currentUser, filter,
                        context);
            }

        });
    }

    private ArrayList<TaskInstance> getCurrentTaskInstancesInternal(
            final NuxeoPrincipal currentUser, final JbpmListFilter filter,
            JbpmContext context) {
        if (currentUser == null) {
            throw new IllegalStateException("Null current user");
        }
        context.setActorId(NuxeoPrincipal.PREFIX + currentUser.getName());
        List<String> groups = getActorsAndGroup(currentUser);
        ArrayList<TaskInstance> tis = getPooledAndActorTaskInstances(context,
                groups);
        // filter
        if (filter != null) {
            tis = filter.filter(context, null, tis, currentUser);
        }
        // sort to ensure deterministic order
        Collections.sort(tis, new TaskCreateDateComparator());
        return tis;
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<TaskInstance> getPooledAndActorTaskInstances(
            JbpmContext context, List<String> groups) {
        Set<TaskInstance> tis = new HashSet<TaskInstance>();
        tis.addAll(context.getTaskMgmtSession().findTaskInstances(groups));
        tis.addAll(context.getTaskMgmtSession().findPooledTaskInstances(groups));
        eagerLoadTaskInstances(tis);
        return toArrayList(tis);
    }

    protected void eagerLoadTaskInstances(Collection<TaskInstance> tis) {
        for (TaskInstance ti : tis) {
            eagerLoadTaskInstance(ti);
        }
    }

    protected <T> ArrayList<T> toArrayList(Collection<T> list) {
        ArrayList<T> arrayList = new ArrayList<T>();
        for (T t : list) {
            arrayList.add(t);
        }
        return arrayList;
    }

    @Override
    public ProcessInstance createProcessInstance(final NuxeoPrincipal user,
            final String processDefinitionName, final DocumentModel dm,
            final Map<String, Serializable> variables,
            final Map<String, Serializable> transientVariables)
            throws NuxeoJbpmException {
        return (ProcessInstance) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ProcessInstance run(JbpmContext context)
                    throws NuxeoJbpmException {
                String initiatorActorId = NuxeoPrincipal.PREFIX;
                if (user != null) {
                    initiatorActorId += user.getName();
                    context.setActorId(initiatorActorId);
                }
                ProcessInstance pi = context.newProcessInstance(processDefinitionName);
                if (initiatorActorId != null) {
                    pi.getContextInstance().setVariable(
                            JbpmService.VariableName.initiator.name(),
                            initiatorActorId);
                }
                if (variables != null) {
                    pi.getContextInstance().addVariables(variables);
                }
                if (transientVariables != null) {
                    pi.getContextInstance().setTransientVariables(
                            transientVariables);
                }
                if (dm != null) {
                    pi.getContextInstance().setVariable(
                            JbpmService.VariableName.documentId.name(),
                            dm.getId());
                    pi.getContextInstance().setVariable(
                            JbpmService.VariableName.documentRepositoryName.name(),
                            dm.getRepositoryName());
                }
                TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
                if (ti == null) {
                    pi.signal();
                } else {
                    ti.end();
                }
                return pi;
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstance> getCurrentProcessInstances(
            final NuxeoPrincipal principal, final JbpmListFilter filter)
            throws NuxeoJbpmException {
        return (List<ProcessInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<ProcessInstance> run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (principal == null) {
                    throw new IllegalStateException("Null principal");
                }
                List<String> actorsName = getActorsAndGroup(principal);
                ArrayList<ProcessInstance> initiatorPD = getProcessInstances(
                        context, actorsName);
                if (filter != null) {
                    initiatorPD = filter.filter(context, null, initiatorPD,
                            principal);
                }
                return toArrayList(initiatorPD);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<ProcessInstance> getProcessInstances(
            JbpmContext context, List<String> actorsName) {
        ArrayList<ProcessInstance> initiatorPD = new ArrayList<ProcessInstance>();
        if (actorsName == null) {
            return initiatorPD;
        }
        Session session = context.getSession();
        List<ProcessInstance> list = session.getNamedQuery(
                JbpmService.HibernateQueries.NuxeoHibernateQueries_getProcessInstancesForInitiator.name()).setParameterList(
                "initiators", actorsName).list();
        initiatorPD.addAll(list);
        eagerLoadProcessInstances(initiatorPD);
        // sort to ensure deterministic order
        Collections.sort(list, new ProcessStartDateComparator());
        return initiatorPD;
    }

    protected static List<String> getActorsAndGroup(NuxeoPrincipal principal) {
        List<String> actors = new ArrayList<String>();
        String name = principal.getName();
        if (!name.startsWith(NuxeoPrincipal.PREFIX)) {
            name = NuxeoPrincipal.PREFIX + name;
        }
        actors.add(name);
        for (String group : principal.getAllGroups()) {
            if (!group.startsWith(NuxeoGroup.PREFIX)) {
                group = NuxeoGroup.PREFIX + group;
            }
            actors.add(group);
        }
        return actors;
    }

    @SuppressWarnings("unchecked")
    protected List<ProcessDefinition> getProcessDefinitions(NuxeoPrincipal user)
            throws NuxeoJbpmException {
        return (List<ProcessDefinition>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<ProcessDefinition> run(JbpmContext context) {
                GraphSession session = context.getGraphSession();
                return toArrayList((List<ProcessDefinition>) session.findLatestProcessDefinitions());
            }
        });
    }

    @Override
    public DocumentModel getDocumentModel(final TaskInstance ti,
            final NuxeoPrincipal user) throws NuxeoJbpmException {
        return (DocumentModel) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (user != null) {
                    context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                }
                String docId;
                String repoId;
                TaskInstance sessionedTi = context.getTaskInstance(ti.getId());
                ProcessInstance pi = sessionedTi.getProcessInstance();
                if (pi == null) {// task created outside a process
                    docId = (String) sessionedTi.getVariable(JbpmService.VariableName.documentId.name());
                    repoId = (String) sessionedTi.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                } else {
                    ProcessInstance sessionedPi = context.getProcessInstance(pi.getId());
                    ContextInstance ci = sessionedPi.getContextInstance();
                    docId = (String) ci.getVariable(JbpmService.VariableName.documentId.name());
                    repoId = (String) ci.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                }
                return getDocumentModel(user, docId, repoId);
            }
        });

    }

    protected DocumentModel getDocumentModel(NuxeoPrincipal user, String docId,
            String repoId) throws NuxeoJbpmException {
        CoreSession session = getCoreSession(repoId, user);
        DocumentModel result;
        try {
            result = session.getDocument(new IdRef(docId));
            result.detach(true);
        } catch (ClientException e) {
            throw new NuxeoJbpmException(e);
        } finally {
            closeCoreSession(session);
        }
        return result;
    }

    @Override
    public DocumentModel getDocumentModel(final ProcessInstance pi,
            final NuxeoPrincipal user) throws NuxeoJbpmException {
        return (DocumentModel) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                ProcessInstance sessionedPi = context.getProcessInstance(pi.getId());
                ContextInstance ci = sessionedPi.getContextInstance();
                String docId = (String) ci.getVariable(JbpmService.VariableName.documentId.name());
                String repoId = (String) ci.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                return getDocumentModel(user, docId, repoId);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstance> getProcessInstances(final DocumentModel dm,
            final NuxeoPrincipal user, final JbpmListFilter jbpmListFilter)
            throws NuxeoJbpmException {
        return (List<ProcessInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<ProcessInstance> run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (user != null) {
                    context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                }
                ArrayList<ProcessInstance> result = new ArrayList<ProcessInstance>();
                Session session = context.getSession();
                List<ProcessInstance> list = session.getNamedQuery(
                        JbpmService.HibernateQueries.NuxeoHibernateQueries_getProcessInstancesForDoc.name()).setParameter(
                        "docId", dm.getId()).setParameter("repoId",
                        dm.getRepositoryName()).list();
                for (ProcessInstance pi : list) {
                    if (Boolean.TRUE.equals(getPermission(pi,
                            JbpmSecurityPolicy.Action.read, dm, user))) {
                        pi.getProcessDefinition();
                        result.add(pi);
                        pi.getContextInstance().getVariables().size();
                    }
                }
                if (jbpmListFilter != null) {
                    result = jbpmListFilter.filter(context, dm, result, user);
                }
                eagerLoadProcessInstances(result);
                // sort to ensure deterministic order
                Collections.sort(result, new ProcessStartDateComparator());
                return result;
            }

        });
    }

    private void eagerLoadProcessInstances(Collection<ProcessInstance> pis) {
        for (ProcessInstance pi : pis) {
            if (pi == null) {
                continue;
            }
            pi.getProcessDefinition().getName();
            pi.getContextInstance().getVariables();
            for (Object ti : pi.getTaskMgmtInstance().getTaskInstances()) {
                ((TaskInstance) ti).getName();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskInstance> getTaskInstances(final DocumentModel dm,
            final NuxeoPrincipal user, final JbpmListFilter filter)
            throws NuxeoJbpmException {
        assert dm != null;
        return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<TaskInstance> run(JbpmContext context)
                    throws NuxeoJbpmException {
                Set<TaskInstance> tisSet = new HashSet<TaskInstance>();
                if (user != null) {
                    List<TaskInstance> tis = getCurrentTaskInstancesInternal(
                            user, null, context);
                    tisSet.addAll(tis);
                } else {
                    List<TaskInstance> tis = new ArrayList<TaskInstance>();
                    tis.addAll(context.getSession().getNamedQuery(
                            JbpmService.HibernateQueries.NuxeoHibernateQueries_getTaskInstancesForDoc_byTaskMgmt.name()).setParameter(
                            "docId", dm.getId()).setParameter("repoId",
                            dm.getRepositoryName()).list());
                    tis.addAll(context.getSession().getNamedQuery(
                            JbpmService.HibernateQueries.NuxeoHibernateQueries_getTaskInstancesForDoc_byTask.name()).setParameter(
                            "docId", dm.getId()).setParameter("repoId",
                            dm.getRepositoryName()).list());
                    // sort to ensure deterministic order
                    Collections.sort(tis, new TaskCreateDateComparator());
                    tisSet.addAll(tis);
                }
                ArrayList<TaskInstance> result = getTaskInstancesForDocument(
                        dm, tisSet);
                if (filter != null) {
                    result = filter.filter(context, dm, result, user);
                }
                return result;
            }

        });
    }

    protected void eagerLoadTaskInstance(TaskInstance ti) {
        if (ti.getPooledActors() != null) {
            ti.getPooledActors().size();
        }
        if (ti.getVariableInstances() != null) {
            ti.getVariableInstances().size();
        }
        if (ti.getComments() != null) {
            ti.getComments().size();
        }
        if (ti.getToken() != null) {
            ti.getToken().getId();
        }
    }

    protected ArrayList<TaskInstance> getTaskInstancesForDocument(
            final DocumentModel dm, Set<TaskInstance> tisSet) {
        // we need to look at the variables of the process instance of
        // the task. If there is no process instance we check the
        // variable of the task itself. If there is a process instance,
        // we check the variable, we add the process donePi to not check
        // the variable again. If it belongs to our document, we add
        // the process id to useDocument.
        List<Long> donePi = new ArrayList<Long>();
        List<Long> useDocument = new ArrayList<Long>();
        ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();
        for (TaskInstance ti : tisSet) {
            ProcessInstance pi = ti.getProcessInstance();
            if (pi == null) {
                // task created outside a process
                String docId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
                String repoId = (String) ti.getVariable(JbpmService.VariableName.documentRepositoryName.name());
                if (docId.equals(dm.getId())
                        && repoId.equals(dm.getRepositoryName())) {
                    eagerLoadTaskInstance(ti);
                    result.add(ti);
                }
            } else if (!donePi.contains(Long.valueOf(pi.getId()))) {
                // process instance hasn't been checked yet
                String docId = (String) pi.getContextInstance().getVariable(
                        JbpmService.VariableName.documentId.name());
                String repoId = (String) pi.getContextInstance().getVariable(
                        JbpmService.VariableName.documentRepositoryName.name());
                Long pid = Long.valueOf(pi.getId());
                donePi.add(pid);
                // check if it uses our document, and if so, add it to the list
                if (docId.equals(dm.getId())
                        && repoId.equals(dm.getRepositoryName())) {
                    useDocument.add(pid);
                }
                if (useDocument.contains(pid)) {
                    eagerLoadTaskInstance(ti);
                    result.add(ti);
                }
            } else {
                // we have checked this process instance
                if (useDocument.contains(Long.valueOf(pi.getId()))) {
                    // if it uses our document, add it to the list
                    eagerLoadTaskInstance(ti);
                    result.add(ti);
                }
            }
        }
        return result;
    }

    protected CoreSession getCoreSession(String repositoryName,
            NuxeoPrincipal principal) throws NuxeoJbpmException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("principal", principal);
        try {
            return CoreInstance.getInstance().open(repositoryName, context);
        } catch (ClientException e) {
            throw new NuxeoJbpmException(e);
        }
    }

    protected void closeCoreSession(CoreSession session) {
        CoreInstance.getInstance().close(session);
    }

    @Override
    public void endProcessInstance(final Long processId)
            throws NuxeoJbpmException {
        executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                context.getProcessInstance(processId.longValue()).end();
                return null;
            }
        });
    }

    @Override
    public void endTask(final Long taskInstanceId, final String transition,
            final Map<String, Serializable> taskVariables,
            final Map<String, Serializable> variables,
            final Map<String, Serializable> transientVariables,
            final NuxeoPrincipal principal) throws NuxeoJbpmException {
        executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (principal != null) {
                    context.setActorId(NuxeoPrincipal.PREFIX
                            + principal.getName());
                }
                TaskInstance ti = context.getTaskInstance(taskInstanceId.longValue());
                if (taskVariables != null) {
                    for (String k : taskVariables.keySet()) {
                        ti.setVariableLocally(k, taskVariables.get(k));
                    }
                }
                boolean hasProcess = ti.getProcessInstance() != null;
                if (variables != null) {
                    if (hasProcess) {
                        ti.getProcessInstance().getContextInstance().addVariables(
                                variables);
                    } else {
                        log.error("Cannot put variables on an isolated "
                                + "task without process: " + variables);
                    }
                }
                if (transientVariables != null) {
                    if (hasProcess) {
                        ti.getProcessInstance().getContextInstance().setTransientVariables(
                                transientVariables);
                    } else {
                        log.error("Cannot put transient variables on an isolated "
                                + "task without process: " + transientVariables);
                    }
                }
                if (transition == null || transition.equals("")) {
                    ti.end();
                } else {
                    ti.end(transition);
                }
                return null;
            }
        });
    }

    protected NuxeoPrincipal getPrincipal(String user)
            throws NuxeoJbpmException {
        try {
            return getUserManager().getPrincipal(user);
        } catch (ClientException e) {
            throw new NuxeoJbpmException(e);
        }
    }

    protected UserManager getUserManager() {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new NuxeoJbpmRuntimeException(e);
            }
        }
        return userManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getAvailableTransitions(final Long taskInstanceId,
            final NuxeoPrincipal principal) throws NuxeoJbpmException {
        return (List<String>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (principal != null) {
                    context.setActorId(NuxeoPrincipal.PREFIX
                            + principal.getName());
                }
                // jbpm code returns an array list.
                List<Transition> transitions = (List<Transition>) context.getTaskInstance(
                        taskInstanceId.longValue()).getAvailableTransitions();
                List<String> result = new ArrayList<String>();
                for (Transition transition : transitions) {
                    result.add(transition.getName());
                }
                return (Serializable) result;
            }

        });
    }

    @Override
    public ProcessInstance getProcessInstance(final Long processInstanceId)
            throws NuxeoJbpmException {
        return (ProcessInstance) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                ProcessInstance pi = context.getProcessInstance(processInstanceId.longValue());
                ;
                eagerLoadProcessInstances(Collections.singletonList(pi));
                return pi;
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskInstance> getTaskInstances(final Long processInstanceId,
            final NuxeoPrincipal principal, final JbpmListFilter filter)
            throws NuxeoJbpmException {
        return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (principal != null) {
                    context.setActorId(NuxeoPrincipal.PREFIX
                            + principal.getName());
                }
                Collection<TaskInstance> tis = context.getProcessInstance(
                        processInstanceId.longValue()).getTaskMgmtInstance().getTaskInstances();
                ArrayList<TaskInstance> result = new ArrayList<TaskInstance>();
                for (TaskInstance ti : tis) {
                    eagerLoadTaskInstance(ti);
                    result.add(ti);
                }

                if (filter != null) {
                    result = filter.filter(context, null, result, principal);
                }
                // sort to ensure deterministic order
                Collections.sort(result, new TaskCreateDateComparator());

                return result;
            }
        });
    }

    @Override
    public void saveTaskInstances(final List<TaskInstance> taskInstances)
            throws NuxeoJbpmException {
        executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                Session session = context.getSession();

                for (TaskInstance ti : taskInstances) {
                    session.merge(ti);
                }
                return null;
            }

        });
    }

    protected void addSecurityPolicy(String processDefinitionName,
            JbpmSecurityPolicy securityPolicy) {
        securityPolicies.put(processDefinitionName, securityPolicy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessDefinition> getProcessDefinitions(
            final NuxeoPrincipal user, final DocumentModel dm,
            final JbpmListFilter filter) throws NuxeoJbpmException {
        return (List<ProcessDefinition>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<ProcessDefinition> run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (user != null) {
                    context.setActorId(NuxeoPrincipal.PREFIX + user.getName());
                }
                List<ProcessDefinition> pds = context.getGraphSession().findLatestProcessDefinitions();
                ArrayList<ProcessDefinition> result = new ArrayList<ProcessDefinition>(
                        pds);
                if (filter != null) {
                    result = filter.filter(context, dm, result, user);
                }
                return result;
            }
        });
    }

    @Override
    public Boolean getPermission(final ProcessInstance pi,
            final JbpmSecurityPolicy.Action action, final DocumentModel dm,
            final NuxeoPrincipal principal) throws NuxeoJbpmException {
        return (Boolean) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context) {
                ProcessInstance process = context.getProcessInstance(pi.getId());
                String pdName = process.getProcessDefinition().getName();
                if (securityPolicies.containsKey(pdName)) {
                    JbpmSecurityPolicy pm = securityPolicies.get(pdName);
                    Boolean perm = pm.checkPermission(process, action, dm,
                            principal);
                    if (perm != null) {
                        return perm;
                    }
                }
                return Boolean.TRUE;
            }
        });

    }

    @Override
    @SuppressWarnings("unchecked")
    public ProcessInstance persistProcessInstance(final ProcessInstance pi)
            throws NuxeoJbpmException {
        return (ProcessInstance) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                ProcessInstance sessionedPi = context.getProcessInstance(pi.getId());
                ContextInstance ci = sessionedPi.getContextInstance();
                Map<String, Object> attrs = pi.getContextInstance().getVariables();
                for (String k : attrs.keySet()) {
                    ci.setVariable(k, attrs.get(k));
                }
                Session session = context.getSession();
                session.saveOrUpdate(sessionedPi);
                return context.getProcessInstance(pi.getId());
            }
        });
    }

    @Override
    public Map<String, List<String>> getTypeFilterConfiguration() {
        return typeFilters;
    }

    protected void setTypeFilters(Map<String, List<String>> typeFilters) {
        this.typeFilters = typeFilters;
    }

    @Override
    public void deleteProcessInstance(final NuxeoPrincipal principal,
            final Long processId) throws NuxeoJbpmException {
        executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            @SuppressWarnings("unchecked")
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (principal != null) {
                    context.setActorId(principal.getName());
                }
                ProcessInstance pi = context.getProcessInstance(processId.longValue());
                Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getTaskInstances();
                List<TaskInstance> toRemove = new ArrayList<TaskInstance>();
                for (TaskInstance ti : tis) {
                    if (!ti.hasEnded()) {
                        toRemove.add(ti);
                    }
                }
                for (TaskInstance ti : toRemove) {
                    context.getSession().delete(ti);
                }
                context.getSession().delete(pi);
                return null;
            }
        });
    }

    @Override
    public void deleteTaskInstance(final NuxeoPrincipal principal,
            final Long taskId) throws NuxeoJbpmException {
        executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                if (principal != null) {
                    context.setActorId(principal.getName());
                }
                TaskInstance taskInstance = context.getTaskInstance(taskId.longValue());
                List<TaskInstance> toRemove = new ArrayList<TaskInstance>();
                toRemove.add(taskInstance);
                context.getSession().delete(taskInstance);
                return null;
            }
        });
    }

    @Override
    public ProcessDefinition getProcessDefinitionByName(final String name)
            throws NuxeoJbpmException {
        return (ProcessDefinition) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                return context.getGraphSession().findLatestProcessDefinition(
                        name);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessDefinition> getProcessDefinitionsByType(final String type)
            throws NuxeoJbpmException {
        return (List<ProcessDefinition>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context)
                    throws NuxeoJbpmException {
                List<String> pdsName = typeFilters.get(type);
                if (pdsName == null) {
                    return new ArrayList<ProcessDefinition>();
                }
                ArrayList<ProcessDefinition> pds = new ArrayList<ProcessDefinition>();
                for (String name : pdsName) {
                    pds.add(getProcessDefinitionByName(name));
                }
                return pds;
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstance> getCurrentProcessInstances(
            final List<String> actors, final JbpmActorsListFilter filter)
            throws NuxeoJbpmException {
        return (List<ProcessInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public Serializable run(JbpmContext context) {
                ArrayList<ProcessInstance> processes = getProcessInstances(
                        context, actors);
                if (filter != null) {
                    processes = filter.filter(context, null, processes, actors);
                }
                return processes;
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskInstance> getCurrentTaskInstances(
            final List<String> actors, final JbpmActorsListFilter filter)
            throws NuxeoJbpmException {
        return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<TaskInstance> run(JbpmContext context)
                    throws NuxeoJbpmException {
                return getCurrentTaskInstancesInternal(actors, filter, context);
            }

        });
    }

    private ArrayList<TaskInstance> getCurrentTaskInstancesInternal(
            final List<String> actors, final JbpmActorsListFilter filter,
            JbpmContext context) {
        ArrayList<TaskInstance> tis = getPooledAndActorTaskInstances(context,
                actors);
        // filter
        if (filter != null) {
            tis = filter.filter(context, null, tis, actors);
        }
        // sort to ensure deterministic order
        Collections.sort(tis, new TaskCreateDateComparator());
        return tis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TaskInstance> getTaskInstances(final DocumentModel dm,
            final List<String> actors, final JbpmActorsListFilter filter)
            throws NuxeoJbpmException {
        assert dm != null;
        return (List<TaskInstance>) executeJbpmOperation(new JbpmOperation() {
            private static final long serialVersionUID = 1L;

            @Override
            public ArrayList<TaskInstance> run(JbpmContext context)
                    throws NuxeoJbpmException {
                Set<TaskInstance> tisSet = new HashSet<TaskInstance>();
                List<TaskInstance> tis = getCurrentTaskInstancesInternal(
                        actors, null, context);
                tisSet.addAll(tis);
                ArrayList<TaskInstance> result = getTaskInstancesForDocument(
                        dm, tisSet);
                if (filter != null) {
                    result = filter.filter(context, dm, result, actors);
                }
                return result;
            }

        });
    }

    protected EventProducer getEventProducer() throws Exception {
        if (eventProducer == null) {
            eventProducer = Framework.getService(EventProducer.class);
        }
        return eventProducer;
    }

    @Override
    public void notifyEventListeners(String name, String comment,
            String[] recipients, CoreSession session, NuxeoPrincipal principal,
            DocumentModel doc) throws ClientException {
        DocumentEventContext ctx = new DocumentEventContext(session, principal,
                doc);
        ctx.setProperty("recipients", recipients);
        ctx.getProperties().put("comment", comment);
        try {
            getEventProducer().fireEvent(ctx.newEvent(name));
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
