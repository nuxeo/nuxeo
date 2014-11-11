/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client.rest.api;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;

/**
 *
 *
 * @since 5.8
 */
public class RestClientTest {

    @Test
    public void itComputesAPIEndpointBasedOnAutomationEndpoint()
            throws Exception {
        String expectedTransformations[][] = new String[][] {
                new String[] { "/automation/", "/api/v1/" },
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
