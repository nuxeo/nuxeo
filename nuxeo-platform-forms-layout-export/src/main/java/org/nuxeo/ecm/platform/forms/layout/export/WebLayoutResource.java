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

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutManager;
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
public class WebLayoutResource {

    protected LayoutManager service;

    protected List<String> registredLayoutNames;

    public WebLayoutResource() throws Exception {
        try {
            service = Framework.getService(LayoutManager.class);
            registredLayoutNames = service.getLayoutDefinitionNames();
            // sort so that order is deterministic
            Collections.sort(registredLayoutNames);
        } catch (Exception e) {
            throw WebException.wrap("Failed to initialize WebLayoutsManager", e);
        }
    }

    protected TemplateView getTemplate(String name, UriInfo uriInfo) {
        String baseURL = uriInfo.getAbsolutePath().toString();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return new TemplateView(this, name).arg("baseURL", baseURL).arg("layoutNames", registredLayoutNames);
    }

    @GET
    public Object doGet(@QueryParam("layoutName")
    String layoutName, @Context
    UriInfo uriInfo) {
        TemplateView tpl =  getTemplate("layouts.ftl", uriInfo);
        if (layoutName!=null) {
            LayoutDefinition layoutDef = service.getLayoutDefinition(layoutName);
            tpl.arg("layoutDefinition", layoutDef);
        }
        return tpl;
    }

    @GET
    @Path("json")
    public String getAsJson(@QueryParam("layoutName") String layoutName, @QueryParam("lang") String lang) {
        if (layoutName!=null) {
            if (lang==null) {
                lang="en";
            }
            LayoutDefinition layoutDef = service.getLayoutDefinition(layoutName);
            JSONObject json = JSONLayoutExporter.exportToJson(layoutDef, lang);
            return json.toString(2);
        }
        return "No layout found";
    }


    @GET
    @Path("docType/{docType}")
    public String getLayoutsForTypeAsJson(@PathParam("docType") String docType, @QueryParam("mode") String mode, @QueryParam("lang") String lang) {

        if (mode==null) {
            mode="edit";
        }
        if (lang==null) {
            lang="en";
        }
        TypeManager tm = Framework.getLocalService(TypeManager.class);
        String[] layoutNames = tm.getType(docType).getLayouts(mode);

        JSONArray jsonLayouts = new JSONArray();
        for (String layoutName : layoutNames) {
            LayoutDefinition layoutDef = service.getLayoutDefinition(layoutName);
            JSONObject jsonLayout = JSONLayoutExporter.exportToJson(layoutDef, lang);
            jsonLayouts.add(jsonLayout);
        }
        return jsonLayouts.toString(2);
    }
}
