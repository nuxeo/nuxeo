/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.core.probes;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.management.statuses.ProbeScheduler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourcePublisher;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestProbes extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.platform.scheduler.core");
        deployBundle("org.nuxeo.ecm.platform.management");
        openSession();
        fireFrameworkStarted();
    }

    public void testScheduling() throws MalformedObjectNameException {
        ProbeScheduler scheduler = Framework.getLocalService(ProbeScheduler.class);
        assertFalse(scheduler.isEnabled());

        scheduler.enable();
        assertTrue(scheduler.isEnabled());

        scheduler.disable();
        assertFalse(scheduler.isEnabled());

        ResourcePublisher publisher = Framework.getLocalService(ResourcePublisher.class);
        assertTrue(publisher.getResourcesName().contains(new ObjectName("org.nuxeo:name=probeScheduler,type=service")));
    }

    public void testPopulateRepository() throws Exception {
       ProbeInfo info = getProbeRunner().getProbeInfo("populateRepository");
       assertNotNull(info);
       info = getProbeRunner().runProbe(info);
       assertFalse(info.isInError());
       String result = info.getStatus().getAsString();
       System.out.print("populateRepository Probe result : " + result);
   }

    public void testQueryRepository() throws Exception {
        ProbeInfo info = getProbeRunner().getProbeInfo("queryRepository");
        assertNotNull(info);
        info = getProbeRunner().runProbe(info);
        assertFalse(info.isInError());
        System.out.print(info.getStatus().getAsString());
    }

   ProbeManager getProbeRunner() throws Exception {
       return Framework.getService(ProbeManager.class);
   }

}
