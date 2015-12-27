/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client.rest.api;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;

/**
 * @since 5.8
 */
public class RestClientTest {

    @Test
    public void itComputesAPIEndpointBasedOnAutomationEndpoint() throws Exception {
        String expectedTransformations[][] = new String[][] { new String[] { "/automation/", "/api/v1/" },
                new String[] { "/nuxeo/site/automation/", "/nuxeo/api/v1/" },
                new String[] { "/nuxeo/api/v1/automation/", "/nuxeo/api/v1/" },
                new String[] { "/api/v1/automation/", "/api/v1/" } };

        for (String[] expected : expectedTransformations) {
            HttpAutomationClient client = new HttpAutomationClient(expected[0]);
            URI uri = client.getRestClient().service.getURI();
            assertEquals(expected[1], uri.toString());
        }
    }
}
