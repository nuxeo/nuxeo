/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import okhttp3.Response;

import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.api.ConstantsV1;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestSimpleClient extends TestBase {

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanGET() throws IOException {
        Response response = nuxeoClient.get(baseURL + ConstantsV1.API_PATH + "path/");
        assertNotNull(response);
        assertEquals(true, response.isSuccessful());
        String json = response.body().string();
        assertFalse(Strings.EMPTY.equals(json));
        Document document = nuxeoClient.getConverterFactory().readJSON(json, Document.class);
        assertNotNull(document);
        assertEquals("Root", document.getType());
    }

    @Test
    public void itCanPUT() throws IOException {
        Response response = nuxeoClient.put(baseURL + ConstantsV1.API_PATH + "path/",
                "{\"entity-type\": \"document\",\"properties\": {\"dc:title\": \"new title\"}}");
        assertNotNull(response);
        assertEquals(true, response.isSuccessful());
        String json = response.body().string();
        assertFalse(Strings.EMPTY.equals(json));
        Document document = nuxeoClient.getConverterFactory().readJSON(json, Document.class);
        assertNotNull(document);
        assertEquals("new title", document.getTitle());
    }

}