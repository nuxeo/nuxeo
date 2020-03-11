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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * Exports and presents documentation about layouts definitions
 *
 * @author Tiry
 * @since 5.5
 */
public class LayoutResource {

    public static final String DEFAULT_DOCUMENT_LAYOUT_MODE = BuiltinModes.EDIT;

    public static final String DEFAULT_CONVERSION_CATEGORY = "standalone";

    public static final String DEFAULT_LANGUAGE = "en";

    protected final String category;

    protected LayoutStore service;

    protected List<String> registeredLayoutNames;

    public LayoutResource(String category) {
        this.category = category;
        service = Framework.getService(LayoutStore.class);
        registeredLayoutNames = service.getLayoutDefinitionNames(category);
        // sort so that order is deterministic
        Collections.sort(registeredLayoutNames);
    }

    protected TemplateView getTemplate(String name, UriInfo uriInfo) {
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return new TemplateView(this, name).arg("baseURL", baseURL).arg("layoutNames", registeredLayoutNames);
    }

    @GET
    public Object doGet(@QueryParam("layoutName") String layoutName, @Context UriInfo uriInfo) {
        TemplateView tpl = getTemplate("layouts.ftl", uriInfo);
        if (layoutName != null) {
            LayoutDefinition layoutDef = service.getLayoutDefinition(category, layoutName);
            tpl.arg("layoutDefinition", layoutDef);
        }
        return tpl;
    }

    @GET
    @Path("json")
    public String getAsJson(@QueryParam("layoutName") String layoutName, @QueryParam("lang") String lang,
            @QueryParam("convertCat") String conversionCategory) throws IOException {
        if (layoutName != null) {
            if (StringUtils.isBlank(lang)) {
                lang = DEFAULT_LANGUAGE;
            }
            if (StringUtils.isBlank(conversionCategory)) {
                conversionCategory = DEFAULT_CONVERSION_CATEGORY;
            }
            LayoutDefinition layoutDef = service.getLayoutDefinition(category, layoutName);
            if (layoutDef != null) {
                LayoutConversionContext ctx = new LayoutConversionContext(lang, null);
                List<LayoutDefinitionConverter> layoutConverters = service.getLayoutConverters(conversionCategory);
                // pass layout converters now
                for (LayoutDefinitionConverter conv : layoutConverters) {
                    layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
                }
                if (layoutDef != null) {
                    List<WidgetDefinitionConverter> widgetConverters = service.getWidgetConverters(conversionCategory);
                    RenderingContext renderingCtx = CtxBuilder.param(LayoutExportConstants.CATEGORY_PARAMETER, category)
                                                              .param(LayoutExportConstants.LAYOUT_CONTEXT_PARAMETER,
                                                                      ctx)
                                                              .paramList(
                                                                      LayoutExportConstants.WIDGET_CONVERTERS_PARAMETER,
                                                                      widgetConverters)
                                                              .get();
                    return MarshallerHelper.objectToJson(layoutDef, renderingCtx);
                }
            }
        }
        return "No layout found";
    }

    @GET
    @Path("docType/{docType}")
    public String getLayoutsForTypeAsJson(@PathParam("docType") String docType, @QueryParam("mode") String mode,
            @QueryParam("lang") String lang, @QueryParam("convertCat") String conversionCategory) throws IOException {

        if (StringUtils.isBlank(mode)) {
            mode = DEFAULT_DOCUMENT_LAYOUT_MODE;
        }
        if (StringUtils.isBlank(lang)) {
            lang = DEFAULT_LANGUAGE;
        }
        if (StringUtils.isBlank(conversionCategory)) {
            conversionCategory = DEFAULT_CONVERSION_CATEGORY;
        }
        TypeManager tm = Framework.getService(TypeManager.class);
        String[] layoutNames = tm.getType(docType).getLayouts(mode);

        LayoutConversionContext ctx = new LayoutConversionContext(lang, null);
        List<LayoutDefinitionConverter> layoutConverters = service.getLayoutConverters(conversionCategory);
        List<WidgetDefinitionConverter> widgetConverters = service.getWidgetConverters(conversionCategory);

        LayoutDefinitions layoutDefinitions = new LayoutDefinitions();
        for (String layoutName : layoutNames) {
            LayoutDefinition layoutDef = service.getLayoutDefinition(category, layoutName);

            // pass layout converters now
            for (LayoutDefinitionConverter conv : layoutConverters) {
                layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
            }
            if (layoutDef != null) {
                layoutDefinitions.add(layoutDef);
            }
        }
        RenderingContext renderingCtx = CtxBuilder.param(LayoutExportConstants.CATEGORY_PARAMETER, category)
                                                  .param(LayoutExportConstants.LAYOUT_CONTEXT_PARAMETER, ctx)
                                                  .paramList(LayoutExportConstants.WIDGET_CONVERTERS_PARAMETER,
                                                          widgetConverters)
                                                  .get();
        return MarshallerHelper.objectToJson(layoutDefinitions, renderingCtx);
    }
}
