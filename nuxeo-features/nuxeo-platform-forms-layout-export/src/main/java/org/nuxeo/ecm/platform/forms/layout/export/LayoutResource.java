package org.nuxeo.ecm.platform.forms.layout.export;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.webengine.WebException;
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

    public LayoutResource(String category) throws Exception {
        this.category = category;
        try {
            service = Framework.getService(LayoutStore.class);
            registeredLayoutNames = service.getLayoutDefinitionNames(category);
            // sort so that order is deterministic
            Collections.sort(registeredLayoutNames);
        } catch (Exception e) {
            throw WebException.wrap("Failed to initialize WebLayoutsManager", e);
        }
    }

    protected TemplateView getTemplate(String name, UriInfo uriInfo) {
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return new TemplateView(this, name).arg("baseURL", baseURL).arg(
                "layoutNames", registeredLayoutNames);
    }

    @GET
    public Object doGet(@QueryParam("layoutName")
    String layoutName, @Context
    UriInfo uriInfo) {
        TemplateView tpl = getTemplate("layouts.ftl", uriInfo);
        if (layoutName != null) {
            LayoutDefinition layoutDef = service.getLayoutDefinition(category,
                    layoutName);
            tpl.arg("layoutDefinition", layoutDef);
        }
        return tpl;
    }

    @GET
    @Path("json")
    public String getAsJson(@QueryParam("layoutName")
    String layoutName, @QueryParam("lang")
    String lang, @QueryParam("convertCat")
    String conversionCategory) {
        if (layoutName != null) {
            if (StringUtils.isBlank(lang)) {
                lang = DEFAULT_LANGUAGE;
            }
            if (StringUtils.isBlank(conversionCategory)) {
                conversionCategory = DEFAULT_CONVERSION_CATEGORY;
            }
            LayoutDefinition layoutDef = service.getLayoutDefinition(category,
                    layoutName);
            if (layoutDef != null) {
                LayoutConversionContext ctx = new LayoutConversionContext(lang,
                        null);
                List<LayoutDefinitionConverter> layoutConverters = service.getLayoutConverters(conversionCategory);
                // pass layout converters now
                for (LayoutDefinitionConverter conv : layoutConverters) {
                    layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
                }
                if (layoutDef != null) {
                    List<WidgetDefinitionConverter> widgetConverters = service.getWidgetConverters(conversionCategory);
                    JSONObject json = JSONLayoutExporter.exportToJson(category,
                            layoutDef, ctx, widgetConverters);
                    return json.toString(2);
                }
            }
        }
        return "No layout found";
    }

    @GET
    @Path("docType/{docType}")
    public String getLayoutsForTypeAsJson(@PathParam("docType")
    String docType, @QueryParam("mode")
    String mode, @QueryParam("lang")
    String lang, @QueryParam("convertCat")
    String conversionCategory) {

        if (StringUtils.isBlank(mode)) {
            mode = DEFAULT_DOCUMENT_LAYOUT_MODE;
        }
        if (StringUtils.isBlank(lang)) {
            lang = DEFAULT_LANGUAGE;
        }
        if (StringUtils.isBlank(conversionCategory)) {
            conversionCategory = DEFAULT_CONVERSION_CATEGORY;
        }
        TypeManager tm = Framework.getLocalService(TypeManager.class);
        String[] layoutNames = tm.getType(docType).getLayouts(mode);

        JSONArray jsonLayouts = new JSONArray();
        for (String layoutName : layoutNames) {
            LayoutDefinition layoutDef = service.getLayoutDefinition(category,
                    layoutName);
            LayoutConversionContext ctx = new LayoutConversionContext(lang,
                    null);
            List<LayoutDefinitionConverter> layoutConverters = service.getLayoutConverters(conversionCategory);
            // pass layout converters now
            for (LayoutDefinitionConverter conv : layoutConverters) {
                layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
            }
            if (layoutDef != null) {
                JSONObject jsonLayout = JSONLayoutExporter.exportToJson(
                        category, layoutDef);
                jsonLayouts.add(jsonLayout);
            }
        }
        return jsonLayouts.toString(2);
    }
}
