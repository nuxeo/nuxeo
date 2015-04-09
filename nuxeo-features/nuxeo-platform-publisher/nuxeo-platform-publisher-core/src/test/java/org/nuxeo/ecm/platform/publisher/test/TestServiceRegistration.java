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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.platform.versioning.api", //
        "org.nuxeo.ecm.platform.versioning", //
        "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.platform.publisher.core.contrib", //
        "org.nuxeo.ecm.platform.publisher.core", //
})
public class TestServiceRegistration {

    @Inject
    protected CoreSession session;

    @Inject
    protected PublisherService publisherService;

    @Inject
    RemotePublicationTreeManager remotePublicationTreeManager;

    @Test
    public void testMainService() throws Exception {
        assertNotNull(publisherService);
    }

    @Test
    public void testTreeService() throws Exception {
        assertNotNull(remotePublicationTreeManager);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-contrib.xml")
    public void testContrib() throws Exception {
        List<String> treeNames = publisherService.getAvailablePublicationTree();
        assertEquals(1, treeNames.size());
    }

}
