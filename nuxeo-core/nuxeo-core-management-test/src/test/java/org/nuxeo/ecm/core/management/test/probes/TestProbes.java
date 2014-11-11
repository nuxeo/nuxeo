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

package org.nuxeo.ecm.core.management.test.probes;

import java.util.Collection;

import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.probes.AdministrativeStatusProbe;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestProbes extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.management.test");
        super.fireFrameworkStarted();
        openSession();
    }

    public void testServiceLookup() {

        ProbeManager pm = Framework.getLocalService(ProbeManager.class);
        assertNotNull(pm);

    }

    public void testService() {

        ProbeManager pm = Framework.getLocalService(ProbeManager.class);

        ProbeInfo info = pm.getProbeInfo(AdministrativeStatusProbe.class);
        assertNotNull(info);

        info = pm.getProbeInfo("administrativeStatus");
        assertNotNull(info);

        Collection<String> names = pm.getProbeNames();
        assertTrue("admin status shortcut not listed",
                names.contains("administrativeStatus"));
        assertNotNull("admin status probe not published",
                info.getQualifiedName());

        assertEquals(1, info.getRunnedCount());
        assertFalse("not a success", info.isInError());
        assertFalse("wrong success value",
                info.getStatus().getAsString().equals("[unavailable]"));
        assertEquals("wrong default value", "[unavailable]",
                info.getLastFailureStatus().getAsString());

    }

}
