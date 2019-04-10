/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.xdocreport.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@WebObject(type = "xdocRestRoot")
@Path("/xdoctemplates")
public class RootResource extends ModuleRoot {

    @GET
    public String index() {
        String sid = getContext().getCoreSession().getSessionId();
        return "ok :sid =" + sid;
    }

    protected CoreSession getCoreSession() {
        TransactionHelper.startTransaction();

        return getContext().getCoreSession();
    }

    @GET
    @Path("ping")
    public String getPong() {
        return "pong";
    }

    @Path("resources")
    public ResourceService getResourceService() {
        return new ResourceService(getContext().getCoreSession());
    }

    @Path("xdocresources")
    public XDocReportResourceService getXDocResourceService() {
        return new XDocReportResourceService(getContext().getCoreSession());
    }

    @Path("reports")
    public ReportService getReportService() {
        return new ReportService();
    }

}
