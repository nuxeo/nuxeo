/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.export;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

@Path("layout-manager")
public class RootResource {

    protected TemplateView getTemplate(String name, UriInfo uriInfo) {
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return new TemplateView(this, name).arg("baseURL", baseURL);
    }

    @GET
    public Object doGet(@Context UriInfo uriInfo) {
        LayoutStore service = Framework.getService(LayoutStore.class);
        // XXX: use hard coded "jsf" category for now
        int nbWidgetTypes = service.getWidgetTypeDefinitions("jsf").size();
        int nbLayoutTypes = service.getLayoutTypeDefinitions("jsf").size();
        int nbLayouts = service.getLayoutDefinitionNames("jsf").size();
        return getTemplate("index.ftl", uriInfo).arg("nbWidgetTypes", Integer.valueOf(nbWidgetTypes)).arg("nbLayouts",
                Integer.valueOf(nbLayouts)).arg("nbLayoutTypes", Integer.valueOf(nbLayoutTypes));
    }

    @Path("layouts")
    public Object getLayouts() {
        // XXX: use hard coded "jsf" category for now
        return new LayoutResource("jsf");
    }

    @Path("widget-types")
    public Object getWidgetTypes(@QueryParam("widgetTypeCategory") String widgetTypeCategory) {
        if (StringUtils.isBlank(widgetTypeCategory)) {
            widgetTypeCategory = "jsf";
        }
        return new WidgetTypeResource(widgetTypeCategory);
    }

    @Path("layout-types")
    public Object getLayoutTypes(@QueryParam("layoutTypeCategory") String layoutTypeCategory) {
        if (StringUtils.isBlank(layoutTypeCategory)) {
            layoutTypeCategory = "jsf";
        }
        return new LayoutTypeResource(layoutTypeCategory);
    }

}
