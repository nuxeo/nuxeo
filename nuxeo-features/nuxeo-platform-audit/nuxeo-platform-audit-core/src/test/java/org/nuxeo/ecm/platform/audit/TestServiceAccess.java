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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestServiceAccess extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.audit.api");
        deployBundle("org.nuxeo.ecm.platform.audit");
        fireFrameworkStarted();
    }

    public void testFullAccess() {
        NXAuditEvents fullService = Framework.getLocalService(NXAuditEvents.class);
        assertNotNull(fullService);

        if (!(fullService instanceof NXAuditEventsService)) {
            fail("");
        }
    }

    public void testReadAccess() {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        assertNotNull(reader);

        if (!(reader instanceof NXAuditEventsService)) {
            fail("");
        }
    }

    public void testWriteAccess() {
        AuditLogger writer = Framework.getLocalService(AuditLogger.class);
        assertNotNull(writer);

        if (!(writer instanceof NXAuditEventsService)) {
            fail("");
        }
    }

}
