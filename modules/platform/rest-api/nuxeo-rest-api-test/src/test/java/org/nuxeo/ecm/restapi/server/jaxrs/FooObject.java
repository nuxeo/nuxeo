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

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.3
 */
@WebObject(type = "foo")
public class FooObject extends DefaultObject {

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

}
