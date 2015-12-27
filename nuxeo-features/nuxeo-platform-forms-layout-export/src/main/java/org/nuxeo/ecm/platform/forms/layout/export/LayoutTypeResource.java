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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeDefinitionComparator;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
public class LayoutTypeResource {

    protected final String category;

    protected LayoutStore service;

    protected final List<LayoutTypeDefinition> layoutTypes;

    public LayoutTypeResource(String category) {
        this.category = category;
        service = Framework.getService(LayoutStore.class);
        layoutTypes = service.getLayoutTypeDefinitions(category);
        // sort so that order is deterministic
        Collections.sort(layoutTypes, new LayoutTypeDefinitionComparator());
    }

    @GET
    @Path("layoutTypes")
    public Object getLayoutTypeDefinitions(@Context HttpServletRequest request, @QueryParam("all") Boolean all) {
        return new LayoutTypeDefinitions(layoutTypes);
    }

    @GET
    @Path("layoutType/{name}")
    public Object getLayoutTypeDefinition(@Context HttpServletRequest request, @PathParam("name") String name) {
        LayoutTypeDefinition def = service.getLayoutTypeDefinition(category, name);
        if (def != null) {
            return def;
        } else {
            return Response.status(401).build();
        }
    }

    public TemplateView getTemplate(@Context UriInfo uriInfo) {
        return getTemplate("layout-types.ftl", uriInfo);
    }

    protected TemplateView getTemplate(String name, UriInfo uriInfo) {
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        TemplateView tv = new TemplateView(this, name);
        tv.arg("layoutTypeCategory", category);
        tv.arg("layoutTypes", layoutTypes);
        tv.arg("baseURL", baseURL);
        return tv;
    }

    @GET
    public Object doGet(@QueryParam("layoutType") String layoutTypeName, @Context UriInfo uriInfo) {
        if (layoutTypeName == null) {
            return getTemplate(uriInfo);
        } else {
            LayoutTypeDefinition wType = service.getLayoutTypeDefinition(category, layoutTypeName);
            if (wType == null) {
                throw new WebResourceNotFoundException("No layout type found with name: " + layoutTypeName);
            }
            TemplateView tpl = getTemplate(uriInfo);
            tpl.arg("layoutType", wType);
            return tpl;
        }
    }

}
