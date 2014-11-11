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

package org.nuxeo.connect.version;

import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestHeaders extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.connect.client");
        deployBundle("org.nuxeo.connect.client.wrapper");
    }

    public void testVersion() {
        String buildVersion = NuxeoConnectClient.getBuildVersion();
        System.out.println("Build version=" + buildVersion);
        String version = NuxeoConnectClient.getVersion();
        System.out.println("Version=" + version);
    }

}
