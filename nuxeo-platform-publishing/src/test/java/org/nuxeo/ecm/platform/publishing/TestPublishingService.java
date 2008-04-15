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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: TestPublishingService.java 28583 2008-01-08 20:00:27Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing;

import javax.naming.CommunicationException;

import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.publishing.rules.DefaultValidatorsRule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Publishing service test case.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestPublishingService extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("OSGI-INF/nuxeo-platform-publishing-framework.xml");
        deployContrib("OSGI-INF/nuxeo-platform-publishing-contrib.xml");
    }

    /**
     * Lookup here will be successful since local lookup.
     */
    public void testLocalRuntimeServiceLookup() {
        assertNotNull(Framework.getLocalService(PublishingService.class));
    }

    /**
     * Remote lookup will fail since the service is defined at platform level
     * using an EJB name.
     *
     * @throws Exception
     */
    public void xtestRemoteRuntimeServiceLookup() throws Exception {
        boolean failed = false;
        try {
            Framework.getService(PublishingService.class);
        } catch (CommunicationException ce) {
            failed = true;
        }
        assertTrue(failed);
    }

    public void testDefaultValidatorsRule() throws Exception {
        PublishingService service = Framework.getLocalService(
                PublishingService.class);
        assertTrue(service.getValidatorsRule() instanceof DefaultValidatorsRule);

        // Override
        deployContrib("test-nuxeo-platform-publishing-contrib.xml");

        assertTrue(service.getValidatorsRule() instanceof FakeValidatorsRule);

    }

    public void testDefaultValidDate() {
        PublishingService service = Framework.getLocalService(
                PublishingService.class);

        assertEquals("dc", service.getValidDateFieldSchemaPrefixName());
        assertEquals("valid", service.getValidDateFieldName());

        // Override
        deployContrib("test-nuxeo-platform-publishing-contrib.xml");

        assertEquals("foo", service.getValidDateFieldSchemaPrefixName());
        assertEquals("bar", service.getValidDateFieldName());

    }

}
