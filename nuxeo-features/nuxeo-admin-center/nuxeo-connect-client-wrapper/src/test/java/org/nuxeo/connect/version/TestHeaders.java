/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.connect.version;

import org.junit.Before;
import org.junit.Test;

import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestHeaders extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.connect.client");
        deployBundle("org.nuxeo.connect.client.wrapper");
    }

    @Test
    public void testVersion() {
        String buildVersion = NuxeoConnectClient.getBuildVersion();
        System.out.println("Build version=" + buildVersion);
        String version = NuxeoConnectClient.getVersion();
        System.out.println("Version=" + version);
    }

}
