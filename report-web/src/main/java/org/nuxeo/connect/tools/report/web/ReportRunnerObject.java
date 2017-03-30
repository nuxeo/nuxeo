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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.connect.tools.report.ReportRunner;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * The report runner exposed as a resource itself.
 *
 * @since 8.3
 */
@WebObject(type = "report")
public class ReportRunnerObject extends DefaultObject {

    ReportRunner runner = Framework.getService(ReportRunner.class);

    List<String> list = new ArrayList<>(runner.list());

    List<String> selection = new ArrayList<>(list);

    @GET
    @Produces("text/html")
    public Template index(@Context UriInfo info) {
        return getView("index");
    }

    @POST
    @Path("run")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/json")
    public Response run(@FormParam("reports") List<String> reports) throws IOException {
        selection = reports;

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream sink) throws IOException, WebApplicationException {
                runner.run(sink, new HashSet<>(selection));
            }
        };
        return Response.ok(stream)
                .header("Content-Disposition",
                        "attachment; filename=nuxeo-connect-tools-report.json")
                .build();
    }

    public List<String> availables() {
        return list;
    }

    public boolean isSelected(String report) {
        return selection.contains(report);
    }

    @Override
    public String toString() {
        return "Report runner of " + selection;
    }
}
