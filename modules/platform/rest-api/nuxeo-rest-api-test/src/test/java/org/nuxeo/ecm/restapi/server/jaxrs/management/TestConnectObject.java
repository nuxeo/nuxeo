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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.junit.Assume.assumeFalse;

import org.junit.Test;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2025.5
 */
@Deploy("org.nuxeo.connect.client")
@Deploy("org.nuxeo.connect.client.wrapper")
public class TestConnectObject extends ManagementBaseTest {

    @Test
    public void testUnregisteredConnectStatus() {
        assumeFalse(ConnectStatusHolder.instance().isRegistered());
        httpClient.buildGetRequest("/management/connect/status")
                  .executeAndConsume(new JsonNodeHandler(), ThrowableConsumer.asConsumer(node -> {
                      JsonAssert jAssert = JsonAssert.on(node.toString());
                      jAssert.has("entity-type").isEquals("connectStatus");
                      jAssert.has("registered").isBool().isEquals(false);
                  }));
    }

}
