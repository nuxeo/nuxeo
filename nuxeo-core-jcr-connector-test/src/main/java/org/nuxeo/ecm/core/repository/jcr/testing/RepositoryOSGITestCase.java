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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.testing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class RepositoryOSGITestCase extends NXRuntimeTestCase {

    public static final String REPOSITORY_NAME = "demo";

    protected Repository repository;

    protected CoreSession coreSession;

    protected RepositoryOSGITestCase() {
    }

    protected RepositoryOSGITestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the core bundles
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        // backend
        deployBundle("org.nuxeo.ecm.core.jcr");
//      assertNotNull(Framework.getService(CoreEventListenerService.class));
    }

    @Override
    protected void tearDown() throws Exception {
        releaseCoreSession();
        releaseRepository();
        super.tearDown();
    }

    public void openRepository() throws Exception {
        if (repository == null) {
            // the repository should be deployed the last
            // after any other bundle that is doctypes
            // this is nuxeo-core-jcr-connector-test
            deployBundle("org.nuxeo.ecm.core.jcr-connector");
            repository = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                    REPOSITORY_NAME);
        }
        openCoreSession("Administrator");
    }

    public CoreSession getCoreSession() {
        return coreSession;
    }

    protected Repository getRepository() {
        return repository;
    }

    protected void openCoreSession(String userName) throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", userName);
        coreSession = CoreInstance.getInstance().open(REPOSITORY_NAME, ctx);
        assertNotNull(coreSession);
    }

    public void changeUser(String newUser) throws ClientException {
        releaseCoreSession();
        openCoreSession(newUser);
    }

    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }

    public void releaseCoreSession() {
        if (coreSession != null) {
            CoreInstance.getInstance().close(coreSession);
            coreSession = null;
        }
    }

    protected Session getSession() throws DocumentException {
        return repository.getSession(null);
    }

}
