/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.webengine.WebException;
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

    public LayoutTypeResource(String category) throws Exception {
        this.category = category;
        try {
            service = Framework.getService(LayoutStore.class);
            layoutTypes = service.getLayoutTypeDefinitions(category);
            // sort so that order is deterministic
            Collections.sort(layoutTypes, new LayoutTypeDefinitionComparator());
        } catch (Exception e) {
            throw WebException.wrap("Failed to initialize WebLayoutManager", e);
        }
    }

    @GET
    @Path("layoutTypes")
    public Object getLayoutTypeDefinitions(@Context
    HttpServletRequest request, @QueryParam("all")
    Boolean all) {
        return new LayoutTypeDefinitions(layoutTypes);
    }

    @GET
    @Path("layoutType/{name}")
    public Object getLayoutTypeDefinition(@Context
    HttpServletRequest request, @PathParam("name")
    String name) {
        LayoutTypeDefinition def = service.getLayoutTypeDefinition(category,
                name);
        if (def != null) {
            return def;
        } else {
            return Response.status(401).build();
        }
    }

    public TemplateView getTemplate(@Context
    UriInfo uriInfo) {
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
    public Object doGet(@QueryParam("layoutType")
    String layoutTypeName, @Context
    UriInfo uriInfo) {
        if (layoutTypeName == null) {
            return getTemplate(uriInfo);
        } else {
            LayoutTypeDefinition wType = service.getLayoutTypeDefinition(
                    category, layoutTypeName);
            if (wType == null) {
                throw new WebResourceNotFoundException(
                        "No layout type found with name: " + layoutTypeName);
            }
            TemplateView tpl = getTemplate(uriInfo);
            tpl.arg("layoutType", wType);
            return tpl;
        }
    }

}
