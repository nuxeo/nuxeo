/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLRepositoryTestCase extends NXRuntimeTestCase {

    protected String REPOSITORY_NAME = "test";

    protected CoreSession session;

    public SQLRepositoryTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
        SQLRepositoryHelper.setUpRepository();
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
                SQLRepositoryHelper.getDeploymentContrib());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        SQLRepositoryHelper.tearDownRepository();
    }

    public void openSession() throws ClientException {
        session = openSessionAs(SecurityConstants.ADMINISTRATOR);
        assertNotNull(session);
    }

    public CoreSession openSessionAs(String username) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", username);
        return CoreInstance.getInstance().open(REPOSITORY_NAME, context);
    }

    public void closeSession() throws ClientException {
        closeSession(session);
    }

    public void closeSession(CoreSession session) throws ClientException {
        CoreInstance.getInstance().close(session);
    }

}
