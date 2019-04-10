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
package org.nuxeo.template.jaxrs;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ResourceService extends AbstractResourceService {

    public ResourceService(CoreSession session) {
        super(session);
    }

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName() {
        return super.getName();
    }

    @Context
    protected HttpServletRequest request;

    public String getRoot() {
        CoreSession session = getCoreSession();
        TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);
        List<TemplateSourceDocument> templates = tps.getAvailableTemplates(session, null);
        StringBuffer sb = new StringBuffer();

        sb.append("[");
        for (TemplateSourceDocument t : templates) {
            sb.append("{");
            sb.append("\"label\":" + "\"" + t.getLabel() + "\",");
            sb.append("\"name\":" + "\"" + t.getName() + "\",");
            sb.append("\"id\":" + "\"" + t.getId() + "\"");
            sb.append("},");
        }

        String result = sb.toString();
        result = result.substring(0, result.length() - 2) + "]";

        return result;
    }
}
