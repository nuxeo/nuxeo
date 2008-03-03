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
 * $Id: TestEventConfService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestEventConfService extends NXRuntimeTestCase {

    private NXAuditEvents service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("nxaudit-test-definitions.xml");

        service = NXAudit.getNXAuditEventsService();
        assertNotNull(service);
    }

    public void testEventsRegistration() {
        assertEquals(4, service.getAuditableEventNames().size());
    }

}
