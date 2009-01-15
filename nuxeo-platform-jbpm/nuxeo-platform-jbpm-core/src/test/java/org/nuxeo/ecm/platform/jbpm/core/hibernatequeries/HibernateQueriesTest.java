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
package org.nuxeo.ecm.platform.jbpm.core.hibernatequeries;

import java.net.URL;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmService;

/**
 * @author arussel
 *
 */
public class HibernateQueriesTest extends TestCase {

    private static final String DEMO = "demo";

    private static final String _1 = "1";

    private static final String GET_TI = JbpmService.HibernateQueries.NuxeoHibernateQueries_getTaskInstancesForDoc.name();

    private static SessionFactory factory;

    private static final String DOC_ID = JbpmService.VariableName.documentId.name();

    private static final String REP_ID = JbpmService.VariableName.documentRepositoryName.name();

    private static final String GET_PI = JbpmService.HibernateQueries.NuxeoHibernateQueries_getProcessInstancesForDoc.name();

    private Session session;

    @Override
    public void setUp() throws Exception {
        URL url = getClass().getResource("/config/test-jbpm-hibernate.cfg.xml");
        assertNotNull(url);
        factory = new Configuration().configure(url).buildSessionFactory();
        session = factory.openSession();
        assertNotNull(session);
    }

    @SuppressWarnings("unchecked")
    public void testGetProcessInstancesForDoc() {
        ProcessDefinition pd = new ProcessDefinition();
        List<ProcessInstance> list = session.getNamedQuery(GET_PI).setParameter(
                "docId", _1).setParameter("repoId", DEMO).list();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        ProcessInstance pi = new ProcessInstance();
        pi.setProcessDefinition(pd);
        Token token = new Token();
        pi.setRootToken(token);
        token.setProcessInstance(pi);
        pi.getContextInstance().setVariable(
                JbpmService.VariableName.documentId.name(), _1);
        pi.getContextInstance().setVariable(
                JbpmService.VariableName.documentRepositoryName.name(), DEMO);
        session.save(pi.getContextInstance());
        session.save(token);
        session.save(pi);
        session.save(pd);
        session.flush();
        list = session.getNamedQuery(GET_PI).setParameter("docId", _1).setParameter(
                "repoId", DEMO).list();
        assertEquals(1, list.size());
        // pi.end();
        pi.setEnd(new Date());
        session.save(pi.getContextInstance());
        session.save(token);
        session.save(pi);
        session.save(pd);
        session.flush();
        list = session.getNamedQuery(GET_PI).setParameter("docId", _1).setParameter(
                "repoId", DEMO).list();
        assertNotNull(pi.getEnd());
        assertEquals(0, list.size());

    }

    @SuppressWarnings("unchecked")
    public void testGetTaskInstancesForDoc() {
        ProcessDefinition pd = new ProcessDefinition();
        ProcessInstance pi = new ProcessInstance(pd);
        pi.setRootToken(new Token(pi));
        ContextInstance ci = pi.getContextInstance();
        ci.setVariable(DOC_ID, _1);
        ci.setVariable(REP_ID, DEMO);
        TaskMgmtInstance tmi = pi.getTaskMgmtInstance();
        TaskInstance ti1 = tmi.createTaskInstance();
        ti1.setCreate(new Date());
        TaskInstance ti = new TaskInstance();
        ti.setVariable(DOC_ID, _1);
        ti.setVariable(REP_ID, DEMO);
        session.save(ci);
        session.save(pd);
        session.save(ti);
        session.save(pi);
        session.save(ti1);
        session.save(tmi);
        session.flush();
        List<TaskInstance> list = session.getNamedQuery(GET_TI).setParameter(
                "docId", _1).setParameter("repoId", DEMO).list();
        assertEquals(2, list.size());
    }

    @Override
    protected void tearDown() throws Exception {
        session.close();
    }

}
