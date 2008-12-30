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
import java.util.List;

import junit.framework.TestCase;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmService;

/**
 * @author arussel
 *
 */
public class HibernateQueriesTest extends TestCase {
    private static SessionFactory factory;

    private Session session;

    @Override
    public void setUp() throws Exception {
        if (factory == null) {
            URL url = getClass().getResource("/config/test-hibernate.cfg.xml");
            factory = new Configuration().configure(url).buildSessionFactory();
        }

        session = factory.openSession();
    }

    @SuppressWarnings("unchecked")
    public void testGetProcessInstancesForDoc() {
        assertNotNull(session);
        List<ProcessInstance> list = session.getNamedQuery(
                JbpmService.HibernateQueries.NuxeoHibernateQueries_getProcessInstancesForDoc.name()).setParameter(
                "docId", "1").setParameter("repoId", "demo").list();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        session.close();
    }
}
