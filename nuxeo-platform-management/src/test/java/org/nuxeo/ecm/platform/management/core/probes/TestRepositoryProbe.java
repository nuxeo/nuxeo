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

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.management.statuses.ProbeInfo;
import org.nuxeo.ecm.platform.management.statuses.ProbeRunner;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestRepositoryProbe extends SQLRepositoryTestCase {

    ProbeRunner probeRunner;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.platform.management");
        deployBundle("org.nuxeo.ecm.platform.management.test");
        openSession();
    }
    
    
    public void testRunRepositoryProbe() throws Exception {
       ProbeInfo repoProbe = getProbeRunner().getProbeInfo("repository-probe");
       probeRunner.runProbe(repoProbe);
       assertTrue(probeRunner.getProbesInSuccess().contains("repository-probe"));
   }
   
    public void testRunRepositoryTestProbe() throws Exception {
        ProbeInfo repoProbe = getProbeRunner().getProbeInfo("repositoryTest-probe");
        probeRunner.runProbe(repoProbe);
        assertTrue(probeRunner.getProbesInSuccess().contains("repositoryTest-probe"));
    }
    
   ProbeRunner getProbeRunner() throws Exception {
       if (probeRunner == null) {
           probeRunner = Framework.getService(ProbeRunner.class);
       }
       return probeRunner;
   }

}
