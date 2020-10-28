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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.restapi.test.versioning.DummyObject;
import org.nuxeo.ecm.restapi.test.versioning.DummyObjectV2;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.3
 */
@WebObject(type = "foo")
public class FooObject extends DefaultObject {

    @Context
    protected APIVersion apiVersion;

    @Context
    protected HttpServletRequest request;

    @GET
    @Path("unauthenticated")
    public Object doGetUnauthenticated() {
        return Blobs.createJSONBlob("{ \"foo\": \"bar\" }");
    }

    @GET
    @Path("rollback")
    public Object doGetRollback() {
        TransactionHelper.setTransactionRollbackOnly();
        return Blobs.createJSONBlob("{ \"foo\": \"bar\" }");
    }

    @GET
    @Path("exception")
    public Object doException() {
        throw new NuxeoException("foo");
    }

    @GET
    @Path("bad-request")
    public Object doBadRequestException() {
        throw new NuxeoException("bad request", SC_BAD_REQUEST);
    }

    /**
     * Endpoint behaving differently in v1 and v2+.
     */
    @GET
    @Path("path1")
    public Object doPath1() {
        if (apiVersion.eq(APIVersion.V1)) {
            return Response.ok("foo").build();
        }

        // other than REST API v1
        return Response.ok("bar").build();
    }

    /**
     * Endpoint only available in v2+.
     */
    @GET
    @Path("path2")
    public Object doPath2() {
        if (apiVersion.lt(APIVersion.V11)) {
            return Response.status(SC_NOT_FOUND).build();
        }

        // API version >= 2
        return Response.ok("bar").build();
    }

    /**
     * Endpoint returning a {@link DummyObject}.
     */
    @GET
    @Path("dummy")
    public Object doGetDummy() {
        return new DummyObject();
    }

    /**
     * Endpoint returning a {@link DummyObjectV2}.
     */
    @GET
    @Path("dummy2")
    public Object doGetDummy2() {
        return new DummyObjectV2();
    }

    /**
     * Endpoint reading a {@link DummyObject}.
     * <p>
     * Returns all fields read by the {@link org.nuxeo.ecm.restapi.test.versioning.DummyReader}.
     */
    @POST
    @Path("dummy")
    public Object doPostDummy2(DummyObject dummyObject) {
        String r = String.format("%s - %s", dummyObject.fieldV1, dummyObject.fieldV2);
        return Response.ok(r).build();
    }

}
