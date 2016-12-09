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
 *      Estele Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * @since 9.1
 */
public class TestResponseHelper {

    @Test
    public void shouldGetBlobResponseWithHeaders() {
        Blob blob = Blobs.createBlob("my doc content", "text/html", "ISO-8859-1");
        blob.setFilename("My doc name");
        Response response = ResponseHelper.blob(blob);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getMetadata().containsKey("Content-Disposition"));
        assertEquals("attachment; filename=My doc name", response.getMetadata().get("Content-Disposition").get(0));
        assertTrue(response.getMetadata().containsKey("Content-Type"));
        assertEquals("text/html; charset=ISO-8859-1", response.getMetadata().get("Content-Type").get(0).toString());

        Blob blob2 = Blobs.createBlob("my doc 2 content");
        Response response2 = ResponseHelper.blob(blob2);
        assertEquals(StringBlob.TEXT_PLAIN + "; charset=" + StringBlob.UTF_8,
                response2.getMetadata().get("Content-Type").get(0).toString());
    }

}
