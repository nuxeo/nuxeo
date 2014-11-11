/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.publisher.test;

import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.runtime.api.Framework;

public class TestServiceRegistration extends SQLRepositoryTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.platform.query.api");
        deployBundle("org.nuxeo.ecm.platform.publisher.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.publisher.core");

        openSession();
        fireFrameworkStarted();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testMainService() throws Exception {
        PublisherService service = Framework.getLocalService(PublisherService.class);
        assertNotNull(service);
    }

    @Test
    public void testTreeService() throws Exception {
        RemotePublicationTreeManager service = Framework.getLocalService(RemotePublicationTreeManager.class);
        assertNotNull(service);
    }

    @Test
    public void testContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.publisher.core",
                "OSGI-INF/publisher-contrib.xml");
        PublisherService service = Framework.getLocalService(PublisherService.class);
        List<String> treeNames = service.getAvailablePublicationTree();
        assertEquals(1, treeNames.size());
    }

}
