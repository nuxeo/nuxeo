/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisServiceFactory;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepositories;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class NuxeoBindingTestCase {

    public String repositoryId;

    public String rootFolderId;

    public CmisBinding binding;

    public static class NuxeoTestCase extends SQLRepositoryTestCase {
        public String getRepositoryId() {
            return REPOSITORY_NAME;
        }

        public CoreSession getSession() {
            return session;
        }
    }

    public NuxeoTestCase nuxeotc;

    public void setUp() throws Exception {
        nuxeotc = new NuxeoTestCase();
        nuxeotc.setUp();
        // QueryMaker registration
        nuxeotc.deployBundle("org.nuxeo.ecm.core.opencmis.impl");
        nuxeotc.deployBundle("org.nuxeo.ecm.core.opencmis.tests");

        Map<String, String> params = new HashMap<String, String>();
        params.put(SessionParameter.BINDING_SPI_CLASS,
                SessionParameter.LOCAL_FACTORY);
        params.put(SessionParameter.LOCAL_FACTORY,
                NuxeoCmisServiceFactory.class.getName());

        binding = CmisBindingFactory.newInstance().createCmisLocalBinding(
                params);

        RepositoryInfo repo = binding.getRepositoryService().getRepositoryInfo(
                nuxeotc.getRepositoryId(), null);
        rootFolderId = repo.getRootFolderId();
        repositoryId = repo.getId();

        assertNotNull(repositoryId);
        assertNotNull(rootFolderId);
    }

    public void tearDown() throws Exception {
        NuxeoRepositories.clear();
        if (nuxeotc != null) {
            nuxeotc.tearDown();
        }
    }

}
