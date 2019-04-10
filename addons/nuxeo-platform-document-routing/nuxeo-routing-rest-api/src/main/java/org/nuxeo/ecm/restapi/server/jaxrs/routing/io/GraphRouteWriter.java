/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.routing.io;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.platform.routing.core.impl.jsongraph.JsonGraphRoute;

@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class GraphRouteWriter extends EntityWriter<JsonGraphRoute>  {

    @Context
    JsonFactory factory;

    @Context
    protected HttpHeaders headers;

    @Context
    protected HttpServletRequest request;

    @Override
    protected String getEntityType() {
        return "graph";
    }

    @Override
    protected void writeEntityBody(JsonGenerator jg, JsonGraphRoute item) throws IOException {
        for (Entry<String, Object> e : item.getGraphElements().entrySet()) {
            jg.writeObjectField(e.getKey(), e.getValue());
        }
    }

}
