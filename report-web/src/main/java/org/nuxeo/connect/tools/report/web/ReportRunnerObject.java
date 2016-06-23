/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.tools.report.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.connect.tools.report.ReportRunner;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Runs report
 *
 * @since 8.3
 */
@Path("/connect-tools-report")
@WebObject(type = "root", administrator = Access.GRANT)
public class ReportRunnerObject extends ModuleRoot {

    ReportRunner runner = Framework.getService(ReportRunner.class);

    List<String> availables = new ArrayList<>(runner.list());

    List<String> selected = new ArrayList<>(availables);

    @Path("/")
    public ReportRunnerObject setup(@Context UriInfo info) {
        return this;
    }

    @Override
    protected void initialize(Object... args) {
        MultivaluedMap<String, String> params = uriInfo.getPathParameters();
        if (params.containsKey("report")) {
            selected = new ArrayList<>(params.get("report"));
        }
    }

    @GET
    @Produces("text/html")
    public Template index(@Context UriInfo info) {
        return getView("index");
    }

    @POST
    @Path("run")
    @Produces("text/json")
    public Response run(@FormParam("report") List<String> reports) throws IOException {
        selected = reports;

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                runner.run(new ZipOutputStream(os), new HashSet<>(selected));
            }
        };
        return Response.ok(stream).header("Content-Encoding", "zip").build();
    }

    public List<String> availables() {
        return availables;
    }

    public boolean isSelected(String report) {
        return selected.contains(report);
    }

    class Builder {


        Builder(UriInfo info) {
            this.info = info;
        }

        final UriInfo info;

        final MultivaluedMap<String, String> values = new MultivaluedMapImpl();

        Builder with(String key, String value) {
            values.add(key, value);
            return this;
        }

        Builder with(String key, List<String> value) {
            values.put(key, value);
            return this;
        }

        URI build() {
            StringBuilder builder = new StringBuilder(info.getBaseUri().toASCIIString());
            for (String key : values.keySet()) {
                for (String value : values.get(key)) {
                    builder.append(';').append(key).append('=').append(value);
                }
            }
            return URI.create(builder.toString());
        }
    }

    @Override
    public String toString() {
        return "Report runner of " + selected;
    }
}
