/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * Checks that API is forbidden when stream metrics are disabled.
 * 
 * @since 2025.10
 */
@WithFrameworkProperty(name = StreamObject.ENABLED_OPTION, value = "false")
public class TestStreamObjectWithoutMetrics extends ManagementBaseTest {

    @Test
    public void testListStreams() {
        httpClient.buildGetRequest("/management/stream/streams")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void testStreamIntrospection() {
        httpClient.buildGetRequest("/management/stream")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void testScale() {
        httpClient.buildGetRequest("/management/stream/scale")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

}
