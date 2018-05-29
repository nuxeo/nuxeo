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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-incorrect-listener-contrib.xml")
public class DocumentCreateIncorrectListenerTest extends BaseTest {

    @Test
    public void testDocumentCreationFails() throws IOException {
        // Given a folder and a Rest Creation request
        DocumentModel folder = RestServerInit.getFolder(0, session);

        String data = "{\"entity-type\": \"document\",\"type\": \"TestIncorectListener\",\"name\":\"newName\",\"properties\": {\"dc:title\":\"My title\",\"dc:description\":\" \"}}";

        try (CloseableClientResponse response = getResponse(RequestType.POST, "path" + folder.getPathAsString(),
                data)) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        }
    }

}
