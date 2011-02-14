/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets.ui;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.theme.webwidgets.Manager;
import org.nuxeo.theme.webwidgets.WidgetData;
import org.nuxeo.theme.webwidgets.WidgetType;

@Path("/nxthemes-webwidgets")
@WebObject(type = "nxthemes-webwidgets")
@Produces("text/html")
public class Main extends ModuleRoot {

    @GET
    @Path("webWidgetFactory")
    public Object renderPerspectiveSelector(
            @QueryParam("org.nuxeo.theme.application.path") String path) {
        return getTemplate("webWidgetFactory.ftl").arg("widget_categories",
                getWidgetCategories()).arg("widget_types", getWidgetTypes()).arg(
                "selected_category", getSelectedWidgetCategory());
    }

    @GET
    @Path("get_panel_data")
    public String getPanelData(@QueryParam("area") int area,
            @QueryParam("mode") String mode) {
        try {
            return Manager.getPanelData(area, mode);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("add_widget")
    public void addWidget(@FormParam("area") int area,
            @FormParam("widget_name") String widgetName,
            @FormParam("order") int order) {
        Editor.addWidget(area, widgetName, order);
    }

    @POST
    @Path("move_widget")
    public String moveWidget(@FormParam("src_area") int srcArea,
            @FormParam("src_uid") String srcUid,
            @FormParam("dest_area") int destArea,
            @FormParam("dest_order") int destOrder) {
        return Editor.moveWidget(srcArea, srcUid, destArea, destOrder);
    }

    @POST
    @Path("remove_widget")
    public void removeWidget(@FormParam("provider") String providerName,
            @FormParam("widget_uid") String widgetUid) {
        Editor.removeWidget(providerName, widgetUid);
    }

    @POST
    @Path("set_widget_state")
    public void setWidgetState(@FormParam("provider") String providerName,
            @FormParam("widget_uid") String widgetUid,
            @FormParam("state") String state) {
        Editor.setWidgetState(providerName, widgetUid, state);
    }

    @POST
    @Path("set_widget_category")
    public void setWidgetCategory(@FormParam("category") String category) {
        SessionManager.setWidgetCategory(category);
    }

    @GET
    @Path("get_widget_data_info")
    public String getWidgetDataInfo(
            @QueryParam("provider") String providerName,
            @QueryParam("widget_uid") String widgetUid,
            @QueryParam("name") String dataName) {
        try {
            return Manager.getWidgetDataInfo(providerName, widgetUid, dataName);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
    }

    @POST
    @Path("upload_file")
    public String uploadFile(@QueryParam("provider") String providerName,
            @QueryParam("widget_uid") String widgetUid,
            @QueryParam("data") String dataName) {
        HttpServletRequest req = ctx.getRequest();
        String res = Editor.uploadFile(req, providerName, widgetUid, dataName);
        long timestamp = new Date().getTime();
        String dataUrl = String.format("nxwebwidgets://data/%s/%s/%s/%s",
                providerName, widgetUid, dataName, timestamp);
        Editor.setWidgetPreference(providerName, widgetUid, dataName, dataUrl);
        return res;
    }

    @GET
    @Path("render_widget_data")
    public Response renderWidgetData(
            @QueryParam("widget_uid") String widgetUid,
            @QueryParam("data") String dataName,
            @QueryParam("provider") String providerName,
            @QueryParam("timestamp") String timestamp) {

        HttpServletRequest request = ctx.getRequest();
        String etag = request.getHeader("If-None-Match");
        if (timestamp.equals(etag)) {
            return Response.notModified().build();
        }

        WidgetData data = null;
        try {
            data = Manager.getWidgetData(providerName, widgetUid, dataName);
        } catch (Exception e) {
            throw new WidgetEditorException(e.getMessage(), e);
        }
        ResponseBuilder builder = Response.ok(data.getContent());
        builder.tag(timestamp);
        builder.type(data.getContentType());
        return builder.build();
    }

    @POST
    @Path("update_widget_preferences")
    @SuppressWarnings("unchecked")
    public void updateWidgetPreferences(@FormParam("provider") String providerName,  @FormParam("widget_uid") String widgetUid, @FormParam("preferences") String preferences_map ) {
        Map<String, String> preferencesMap = JSONObject.fromObject(preferences_map);
        Editor.updateWidgetPreferences(providerName, widgetUid, preferencesMap);
    }

    @GET
    @Path("render_widget_icon")
    public Response renderWidgetIcon(@QueryParam("name") String widgetTypeName) {
        byte[] content = Manager.getWidgetIconContent(widgetTypeName);
        ResponseBuilder builder = Response.ok(content);
        CacheControl cc = new CacheControl();
        // Set a default max-age of 1 day.
        cc.setMaxAge(86400);
        builder.cacheControl(cc);
        // builder.type(???)
        return builder.build();
    }

    /* API */

    public static String getSelectedWidgetCategory() {
        String category = SessionManager.getWidgetCategory();
        if (category == null) {
            category = "";
        }
        return category;
    }

    public static Set<String> getWidgetCategories() {
        return Manager.getService().getWidgetCategories();
    }

    public static List<WidgetType> getWidgetTypes() {
        String widgetCategory = getSelectedWidgetCategory();
        return Manager.getService().getWidgetTypes(widgetCategory);
    }

}
