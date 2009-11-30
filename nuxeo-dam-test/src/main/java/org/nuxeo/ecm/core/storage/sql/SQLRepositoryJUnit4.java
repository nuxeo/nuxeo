/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.junit.Before;
import org.junit.After;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public abstract class SQLRepositoryJUnit4 {

    protected SQLRepositoryTestCase sqlRepositoryTestCase;

    protected CoreSession session;

    public SQLRepositoryJUnit4(String name) {
        sqlRepositoryTestCase = new SQLRepositoryTestCase(name) {
        };
    }

    @Before
    public final void initializeRepository() throws Exception {
        sqlRepositoryTestCase.setUp();
    }

    @After
    public final void tearDownRepository() throws Exception {
        sqlRepositoryTestCase.tearDown();
    }

    public void openSession() throws ClientException {
        sqlRepositoryTestCase.openSession();
        session = sqlRepositoryTestCase.session;
    }

    public CoreSession openSessionAs(String username) throws ClientException {
        return sqlRepositoryTestCase.openSessionAs(username);
    }

    public void closeSession() {
        sqlRepositoryTestCase.closeSession();
    }

    public void closeSession(CoreSession session) {
        sqlRepositoryTestCase.closeSession(session);
    }

    public void deployBundle(String bundle) throws Exception {
        sqlRepositoryTestCase.deployBundle(bundle);
    }

    public void deployContrib(String bundle, String contrib) throws Exception {
        sqlRepositoryTestCase.deployContrib(bundle, contrib);
    }

    public void undeployContrib(String bundle, String contrib) throws Exception {
        sqlRepositoryTestCase.undeployContrib(bundle, contrib);
    }

}
