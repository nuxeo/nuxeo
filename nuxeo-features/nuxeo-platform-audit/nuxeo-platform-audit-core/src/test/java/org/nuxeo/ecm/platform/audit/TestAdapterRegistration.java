/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAdapterRegistration extends NXRuntimeTestCase{


    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.audit.api");
        deployBundle("org.nuxeo.ecm.platform.audit"); // the audit.core
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.audit.tests"); // the audit.core
        deployTestContrib("org.nuxeo.ecm.platform.audit.tests", "test-audit-contrib.xml");
        fireFrameworkStarted();
    }

    @Test
    public void testAuditContribution() throws Exception {
        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime().getComponent(NXAuditEventsService.NAME);
        assertNotNull(auditService);
        Set<AdapterDescriptor> registeredAdapters = auditService.getDocumentAdapters();
        assertEquals(1, registeredAdapters.size());

        AdapterDescriptor ad = registeredAdapters.iterator().next();
        assertEquals("myadapter", ad.getName());

    }

}
