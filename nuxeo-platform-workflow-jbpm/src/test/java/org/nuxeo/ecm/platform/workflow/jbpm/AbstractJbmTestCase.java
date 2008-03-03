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
 * $Id: AbstractJbmTestCase.java 21684 2007-06-30 20:59:40Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import junit.framework.TestCase;

import org.hibernate.Session;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.MessagingSession;
import org.jbpm.db.SchedulerSession;
import org.jbpm.db.TaskMgmtSession;

/**
 * Abstract JbpmTestCase that initializes db and jbpm context.
 * <p/>
 * Inspired form the jBPM test suite.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractJbmTestCase extends TestCase {

    protected static final JbpmConfiguration jbpmConfiguration =
            JbpmConfiguration.parseResource("jbpm.cfg.xml");

    protected JbpmContext jbpmContext;

    protected Session session;
    protected GraphSession graphSession;
    protected TaskMgmtSession taskMgmtSession;
    protected ContextSession contextSession;
    protected SchedulerSession schedulerSession;
    protected LoggingSession loggingSession;
    protected MessagingSession messagingSession;

    public void setUp() throws Exception {
        super.setUp();
        createSchema();
        createJbpmContext();
        initializeMembers();
    }

    public void tearDown() throws Exception {
        resetMembers();
        closeJbpmContext();
        dropSchema();
        super.tearDown();
    }

    protected static void createSchema() {
        jbpmConfiguration.createSchema();
    }

    protected static JbpmConfiguration getJbpmConfiguration() {
        return jbpmConfiguration;
    }

    protected void createJbpmContext() {
        jbpmContext = jbpmConfiguration.createJbpmContext();
    }

    protected void initializeMembers() {
        session = jbpmContext.getSession();
        graphSession = jbpmContext.getGraphSession();
        taskMgmtSession = jbpmContext.getTaskMgmtSession();
        loggingSession = jbpmContext.getLoggingSession();
        schedulerSession = jbpmContext.getSchedulerSession();
        contextSession = jbpmContext.getContextSession();
        messagingSession = jbpmContext.getMessagingSession();
    }

    protected void resetMembers() {
        session = null;
        graphSession = null;
        taskMgmtSession = null;
        loggingSession = null;
        schedulerSession = null;
        contextSession = null;
        messagingSession = null;
    }

    protected void closeJbpmContext() {
        jbpmContext.close();
    }

    protected static void dropSchema() {
        jbpmConfiguration.dropSchema();
    }

    protected void newTransaction() {
        try {
            commitAndCloseSession();
            beginSessionTransaction();
        } catch (Throwable t) {
            throw new RuntimeException("couldn't commit and start new transaction", t);
        }
    }

    public void commitAndCloseSession() {
        closeJbpmContext();
        resetMembers();
    }

    public void beginSessionTransaction() {
        createJbpmContext();
        initializeMembers();
    }

}
