/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 11.3
 */
public class TestManagementHttpPortFilter extends ManagementBaseTest {

    @Test
    public void testDefaultHttpPortConfiguration() {
        try (CloseableClientResponse response = httpClientRule.get("/management/distribution")) {
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        }
    }

    @Test
    @WithFrameworkProperty(name = ManagementObject.MANAGEMENT_API_HTTP_PORT_PROPERTY, value = "10")
    public void testCustomHttpPortConfiguration() {
        try (CloseableClientResponse response = httpClientRule.get("/management/distribution")) {
            assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        }
    }

}
