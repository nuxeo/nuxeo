/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.drive.NuxeoDriveConstants;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the {@link NuxeoDriveGetClientUpdateInfo} operation.
 *
 * @author Antoine Taillefer
 */
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
@ServletContainer(port = 18080)
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
                "http://community.nuxeo.com/static/drive/");
    }

    @Test
    public void testGetClientUpdateInfo() throws Exception {

        Blob clientUpdateInfoJSON = (Blob) clientSession.newRequest(NuxeoDriveGetClientUpdateInfo.ID).execute();
        assertNotNull(clientUpdateInfoJSON);

        NuxeoDriveClientUpdateInfo clientUpdateInfo = mapper.readValue(clientUpdateInfoJSON.getStream(),
                NuxeoDriveClientUpdateInfo.class);

        assertEquals("5.9.3", clientUpdateInfo.getServerVersion());
        assertEquals("http://community.nuxeo.com/static/drive/", clientUpdateInfo.getUpdateSiteURL());

    }

}
