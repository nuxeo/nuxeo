/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.adobe.cc;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.adobe.cc.NuxeoAdobeConnectorService.ADOBE_CC_CLIENT_ID;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.oauth", "org.nuxeo.adobe.cc.nuxeo-adobe-connector-core" })
public class TestNuxeoAdobeConnectorService {

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testService() {
        try (Session session = directoryService.open(OAuth2ClientService.OAUTH2CLIENT_DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("clientId", ADOBE_CC_CLIENT_ID);
            assertEquals(1, session.query(filter).size());
        }
    }
}
