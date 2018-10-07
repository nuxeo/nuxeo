/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.seam;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link NuxeoDriveActions}.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.automation.server:OSGI-INF/auth-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.login.token")
public class TestNuxeoDriveActions {

    @Inject
    protected CoreSession session;

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    protected NuxeoDriveActions nuxeoDriveActions;

    protected NuxeoPrincipal principal;

    @Before
    public void setUp() {
        nuxeoDriveActions = new NuxeoDriveActions();
        principal = session.getPrincipal();
    }

    @Test
    public void testHasOneDriveToken() throws UnsupportedEncodingException {
        assertFalse(nuxeoDriveActions.hasOneDriveToken(principal));

        // Acquire a non Nuxeo Drive token
        String token = acquireToken("myApp");
        assertFalse(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);

        // Acquire a Nuxeo Drive token with applicationName not encoded
        token = acquireToken("Nuxeo Drive");
        assertTrue(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);

        // Acquire a Nuxeo Drive token with applicationName percent encoded (testing backward compatibility)
        token = acquireToken("Nuxeo%20Drive");
        assertTrue(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);

        // Acquire a Nuxeo Drive token with applicationName form-urlencoded (testing backward compatibility)
        token = acquireToken("Nuxeo+Drive");
        assertTrue(nuxeoDriveActions.hasOneDriveToken(principal));
        revokeToken(token);
    }

    protected String acquireToken(String applicationName) {
        return tokenAuthenticationService.acquireToken("Administrator", applicationName, "myDeviceId",
                "myDeviceDescription", "rw");
    }

    protected void revokeToken(String token) {
        tokenAuthenticationService.revokeToken(token);
    }

}
