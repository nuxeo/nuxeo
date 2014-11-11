/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeDefinitionComparator;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * Exports and presents documentation about widget type definitions
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WidgetTypeResource {

    protected final String category;

    protected LayoutStore service;

    protected final List<WidgetTypeDefinition> widgetTypes;

    protected final Map<String, List<WidgetTypeDefinition>> widgetTypesByCat;

    public WidgetTypeResource(String category) throws Exception {
        this.category = category;
        try {
            service = Framework.getService(LayoutStore.class);
            widgetTypes = service.getWidgetTypeDefinitions(category);
            // sort so that order is deterministic
            Collections.sort(widgetTypes, new WidgetTypeDefinitionComparator(
                    true));
            widgetTypesByCat = getWidgetTypesByCategory();
        } catch (Exception e) {
            throw WebException.wrap("Failed to initialize WebLayoutManager", e);
        }
    }

    protected Map<String, List<WidgetTypeDefinition>> getWidgetTypesByCategory() {
        Map<String, List<WidgetTypeDefinition>> cats = new HashMap<String, List<WidgetTypeDefinition>>();
        List<WidgetTypeDefinition> unknownCatWidgets = new ArrayList<WidgetTypeDefinition>();
        for (WidgetTypeDefinition wTypeDef : widgetTypes) {
            List<String> categories = null;
            WidgetTypeConfiguration conf = wTypeDef.getConfiguration();
            if (conf != null) {
                categories = conf.getCategories();
            }
            boolean added = false;
            if (categories != null) {
                for (String cat : categories) {
                    List<WidgetTypeDefinition> list = cats.get(cat);
                    if (list == null) {
                        list = new ArrayList<WidgetTypeDefinition>();
                    }
                    list.add(wTypeDef);
                    cats.put(cat, list);
                    added = true;
                }
            }
            if (!added) {
                unknownCatWidgets.add(wTypeDef);
            }
        }
        if (!unknownCatWidgets.isEmpty()) {
            cats.put("unknown", unknownCatWidgets);
        }
        // sort by category key
        List<String> sortedKeys = new ArrayList<String>(cats.keySet());
        Collections.sort(sortedKeys);
        Map<String, List<WidgetTypeDefinition>> res = new LinkedHashMap<String, List<WidgetTypeDefinition>>();
        for (String key : sortedKeys) {
            res.put(key, cats.get(key));
        }
        return res;
    }

    /**
     * Returns widget types definitions for given categories
     * <p>
     * If the category is null, the filter does not check the category. Widget
     * types without a configuration are included if boolean 'all' is set to
     * true. Mutliple categories are extracted from the query parameter by
     * splitting on the space character.
     * <p>
     * If not null, the version parameter will exclude all widget types that
     * did not exist before this version.
     */
    @GET
    @Path("widgetTypes")
    public Object getWidgetTypeDefinitions(@Context
    HttpServletRequest request, @QueryParam("categories")
    String categories, @QueryParam("version")
    String version, @QueryParam("all")
    Boolean all) {
        // TODO: refactor so that's cached
        List<String> catsList = new ArrayList<String>();
        if (categories != null) {
            for (String cat : categories.split(" ")) {
                catsList.add(cat);
            }
        }
        WidgetTypeDefinitions res = new WidgetTypeDefinitions();
        for (WidgetTypeDefinition def : widgetTypes) {
            WidgetTypeConfiguration conf = def.getConfiguration();
            if (!Boolean.TRUE.equals(all) && conf == null) {
                continue;
            }
            if (version != null && conf != null) {
                String confVersion = conf.getSinceVersion();
                if (confVersion != null && version.compareTo(confVersion) < 0) {
                    continue;
                }
            }
            if (catsList != null && !catsList.isEmpty()) {
                boolean hasCats = false;
                if (conf != null) {
                    // filter on category
                    List<String> confCats = conf.getCategories();
                    if (confCats != null) {
                        hasCats = true;
                        if (confCats.containsAll(catsList)) {
                            res.add(def);
                        }
                    }
                }
                if (!hasCats && catsList.size() == 1
                        && catsList.contains("unknown")) {
                    res.add(def);
                }
            } else {
                if (conf == null && !Boolean.TRUE.equals(all)) {
                    continue;
                }
                res.add(def);
            }
        }
        return res;
    }

    /**
     * Returns widget types definitions for given category.
     * <p>
     * If the category is null, the filter does not check the category. Widget
     * types without a configuration are included if boolean 'all' is set to
     * true.
     * <p>
     * If not null, the version parameter will exclude all widget types that
     * did not exist before this version.
     */
    @GET
    @Path("widgetTypes/{category}")
    public Object getWidgetTypeDefinitionsForCategory(@Context
    HttpServletRequest request, @PathParam("category")
    String category, @QueryParam("version")
    String version, @QueryParam("all")
    Boolean all) {
        return getWidgetTypeDefinitions(request, category, version, all);
    }

    @GET
    @Path("widgetType/{name}")
    public Object getWidgetTypeDefinition(@Context
    HttpServletRequest request, @PathParam("name")
    String name) {
        WidgetTypeDefinition def = service.getWidgetTypeDefinition(category,
                name);
        if (def != null) {
            return def;
        } else {
            return Response.status(401).build();
        }
    }

    public TemplateView getTemplate(@Context
    UriInfo uriInfo) {
        return getTemplate("widget-types.ftl", uriInfo);
    }

    @GET
    @Path("wiki")
    public Object getWikiDocumentation(@Context
    UriInfo uriInfo) {
        return getTemplate("widget-types-wiki.ftl", uriInfo);
    }

    protected List<String> getNuxeoVersions() {
        if ("jsf".equals(category)) {
            return Arrays.asList("5.6", "5.8", "6.0");
        } else if ("jsfAction".equals(category)) {
            return Arrays.asList("5.8", "6.0");
        }
        return Collections.emptyList();
    }

    protected TemplateView getTemplate(String name, UriInfo uriInfo) {
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        TemplateView tv = new TemplateView(this, name);
        tv.arg("categories", widgetTypesByCat);
        tv.arg("nuxeoVersions", getNuxeoVersions());
        tv.arg("widgetTypeCategory", category);
        tv.arg("widgetTypes", widgetTypes);
        tv.arg("baseURL", baseURL);
        return tv;
    }

    @GET
    public Object doGet(@QueryParam("widgetType")
    String widgetTypeName, @Context
    UriInfo uriInfo) {
        if (widgetTypeName == null) {
            return getTemplate(uriInfo);
        } else {
            WidgetTypeDefinition wType = service.getWidgetTypeDefinition(
                    category, widgetTypeName);
            if (wType == null) {
                throw new WebResourceNotFoundException(
                        "No widget type found with name: " + widgetTypeName);
            }
            TemplateView tpl = getTemplate(uriInfo);
            tpl.arg("widgetType", wType);
            return tpl;
        }
    }

    public String getWidgetTypeLabel(WidgetTypeDefinition wTypeDef) {
        if (wTypeDef != null) {
            WidgetTypeConfiguration conf = wTypeDef.getConfiguration();
            if (conf != null) {
                return conf.getTitle();
            }
            return wTypeDef.getName();
        }
        return null;
    }

    public String getWidgetTypeDescription(WidgetTypeDefinition wTypeDef) {
        if (wTypeDef != null) {
            WidgetTypeConfiguration conf = wTypeDef.getConfiguration();
            if (conf != null) {
                return conf.getDescription();
            }
        }
        return null;
    }

    public List<String> getWidgetTypeCategories(WidgetTypeDefinition wTypeDef) {
        if (wTypeDef != null) {
            WidgetTypeConfiguration conf = wTypeDef.getConfiguration();
            if (conf != null) {
                return conf.getCategories();
            }
        }
        return null;
    }

    public String getWidgetTypeCategoriesAsString(WidgetTypeDefinition wTypeDef) {
        List<String> categories = getWidgetTypeCategories(wTypeDef);
        if (categories == null) {
            return "";
        } else {
            return StringUtils.join(categories, ", ");
        }
    }

}
