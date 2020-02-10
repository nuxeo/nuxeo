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

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.drive.NuxeoDriveConstants;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link NuxeoDriveGetClientUpdateInfo} operation.
 *
 * @author Antoine Taillefer
 * @deprecated since 10.3, see {@link NuxeoDriveGetClientUpdateInfo}
 */
@Deprecated
@RunWith(FeaturesRunner.class)
@Features(NuxeoDriveAutomationFeature.class)
public class TestGetClientUpdateInfo {

    @Inject
    protected HttpAutomationSession clientSession;

    @Before
    public void init() {
        // Set Framework properties required for the client update
        Framework.getProperties().put(Environment.DISTRIBUTION_VERSION, "5.9.3");
        Framework.getProperties()
                 .put(NuxeoDriveConstants.UPDATE_SITE_URL_PROP_KEY,
                         "https://community.nuxeo.com/static/drive-updates/");
    }

    @Test
    public void testGetClientUpdateInfo() throws IOException {

        NuxeoDriveClientUpdateInfo clientUpdateInfo = clientSession.newRequest(NuxeoDriveGetClientUpdateInfo.ID)
                                                                   .executeReturning(NuxeoDriveClientUpdateInfo.class);

        assertEquals("5.9.3", clientUpdateInfo.getServerVersion());
        assertEquals("https://community.nuxeo.com/static/drive-updates/", clientUpdateInfo.getUpdateSiteURL());

    }

}
