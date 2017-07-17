/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.test.runner.feature;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;

/**
 * Test resource that allows to simulate an invalid HTTP response for the {@link RetryPostTest}.
 *
 * @since 9.3
 */
@Path("/testRetryPost")
@WebObject(type = "testRetryPost")
public class RetryPostTestObject extends AbstractResource<ResourceTypeImpl> {

    public static final String RETRY_POST_TEST_CONTENT = "Content is much longer than the value of the Content-Length header";

    /**
     * Sends an invalid response due to the "Content-Length" header not matching the actual content length.
     */
    @GET
    public Response get() {
        return Response.ok().header("Content-Length", 2).entity(RETRY_POST_TEST_CONTENT).build();
    }

    @POST
    public Response post() {
        return Response.ok().build();
    }

}
