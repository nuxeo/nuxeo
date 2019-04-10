/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.drive.NuxeoDriveConstants;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

/**
 * Tests the {@link NuxeoDriveGetClientUpdateInfo} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@Deploy({ "org.nuxeo.drive.core", "org.nuxeo.drive.operations", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.platform.login.token" })
@Jetty(port = 18080)
public class TestGetClientUpdateInfo {

    @Inject
    protected Session clientSession;

    protected ObjectMapper mapper;

    @Before
    public void init() throws Exception {
        mapper = new ObjectMapper();

        // Set Framework properties required for the client update
        Framework.getProperties().put(Environment.DISTRIBUTION_VERSION, "5.9.3");
        Framework.getProperties().put(NuxeoDriveConstants.UPDATE_SITE_URL_PROP_KEY,
                "https://community.nuxeo.com/static/drive-updates/");
    }

    @Test
    public void testGetClientUpdateInfo() throws Exception {

        Blob clientUpdateInfoJSON = (Blob) clientSession.newRequest(NuxeoDriveGetClientUpdateInfo.ID).execute();
        assertNotNull(clientUpdateInfoJSON);

        NuxeoDriveClientUpdateInfo clientUpdateInfo = mapper.readValue(clientUpdateInfoJSON.getStream(),
                NuxeoDriveClientUpdateInfo.class);

        assertEquals("5.9.3", clientUpdateInfo.getServerVersion());
        assertEquals("https://community.nuxeo.com/static/drive-updates/", clientUpdateInfo.getUpdateSiteURL());

    }

}
