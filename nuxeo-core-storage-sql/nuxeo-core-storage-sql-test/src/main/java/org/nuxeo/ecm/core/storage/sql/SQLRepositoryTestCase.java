/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.api.ConnectionHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLRepositoryTestCase extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(SQLRepositoryTestCase.class);

    /**
     * @deprecated since 5.5: use {@link #database.repositoryName} instead
     */
    @Deprecated
    public static final String REPOSITORY_NAME = DatabaseHelper.DATABASE.repositoryName;

    public CoreSession session;

    /**
     * Initial number of registered session at setup, to be compared with the
     * number of sessions at tear down.
     */
    protected int initialOpenSessions;

    protected int initialSingleConnections;

    public DatabaseHelper database = DatabaseHelper.DATABASE;

    public SQLRepositoryTestCase() {
    }

    public SQLRepositoryTestCase(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        initialOpenSessions = CoreInstance.getInstance().getNumberOfSessions();
        initialSingleConnections = ConnectionHelper.countConnectionReferences();
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        database.setUp();
        deployRepositoryContrib();
    }

    protected void deployRepositoryContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                database.getDeploymentContrib());
    }

    @After
    public void tearDown() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        super.tearDown();
        database.tearDown();
        final CoreInstance core = CoreInstance.getInstance();
        int finalOpenSessions = core.getNumberOfSessions();
        int leakedOpenSessions = finalOpenSessions - initialOpenSessions;
        if (leakedOpenSessions != 0) {
            log.error(String.format(
                    "There are %s open session(s) at tear down; it seems "
                            + "the test leaked %s session(s).",
                    Integer.valueOf(finalOpenSessions),
                    Integer.valueOf(leakedOpenSessions)));
            for (CoreInstance.RegistrationInfo info:core.getRegistrationInfos()) {
                log.warn("Leaking session", info);
            }
        }
        int finalSingleConnections = ConnectionHelper.countConnectionReferences();
        int leakedSingleConnections = finalSingleConnections - initialSingleConnections;
        if (leakedSingleConnections > 0) {
            log.error(String.format(
                    "There are %s single datasource connection(s) open at tear down; "
                            + "the test leaked %s connection(s).",
                    Integer.valueOf(finalSingleConnections),
                    Integer.valueOf(leakedSingleConnections)));
        }
        ConnectionHelper.clearConnectionReferences();
    }

    public void openSession() throws ClientException {
        if (session != null) {
            log.warn("Closing session for you");
            closeSession();
        }
        session = openSessionAs(ADMINISTRATOR);
        assertNotNull(session);
    }

    public CoreSession openSessionAs(String username) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", username);
        return CoreInstance.getInstance().open(database.repositoryName, context);
    }

    public CoreSession openSessionAs(NuxeoPrincipal principal)
            throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("principal", principal);
        return CoreInstance.getInstance().open(database.repositoryName, context);
    }

    public void closeSession() {
        closeSession(session);
        session = null;
    }

    public void closeSession(CoreSession session) {
        if (session != null) {
            CoreInstance.getInstance().close(session);
        }
    }

}
