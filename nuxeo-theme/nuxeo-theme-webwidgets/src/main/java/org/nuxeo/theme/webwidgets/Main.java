package org.nuxeo.theme.webwidgets;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

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
    public String getPanelData(@QueryParam("provider") String providerName,
            @QueryParam("region") String regionName,
            @QueryParam("mode") String mode) {
        return org.nuxeo.theme.webwidgets.Manager.getPanelData(providerName,
                regionName, mode);
    }

    @POST
    @Path("add_widget")
    public void addWidget() {
        FormData form = ctx.getForm();
        String providerName = form.getString("provider");
        String widgetName = form.getString("widget_name");
        String regionName = form.getString("region");
        int order = Integer.valueOf(form.getString("order"));
        org.nuxeo.theme.webwidgets.Manager.addWidget(providerName, widgetName,
                regionName, order);
    }

    @POST
    @Path("move_widget")
    public String moveWidget() {
        FormData form = ctx.getForm();
        String srcProviderName = form.getString("src_provider");
        String destProviderName = form.getString("dest_provider");
        String srcUid = form.getString("src_uid");
        String srcRegionName = form.getString("src_region");
        String destRegionName = form.getString("dest_region");
        int destOrder = Integer.valueOf(form.getString("dest_order"));
        return org.nuxeo.theme.webwidgets.Manager.moveWidget(srcProviderName,
                destProviderName, srcUid, srcRegionName, destRegionName,
                destOrder);
    }

    @POST
    @Path("remove_widget")
    public void removeWidget() {
        FormData form = ctx.getForm();
        String providerName = form.getString("provider");
        String widgetUid = form.getString("widget_uid");
        org.nuxeo.theme.webwidgets.Manager.removeWidget(providerName, widgetUid);
    }

    @POST
    @Path("set_widget_state")
    public void setWidgetState() {
        FormData form = ctx.getForm();
        String providerName = form.getString("provider");
        String widgetUid = form.getString("widget_uid");
        String state = form.getString("state");
        org.nuxeo.theme.webwidgets.Manager.setWidgetState(providerName,
                widgetUid, state);
    }

    @POST
    @Path("set_widget_category")
    public void setWidgetCategory() {
        FormData form = ctx.getForm();
        String category = form.getString("category");
        SessionManager.setWidgetCategory(category);
    }

    @GET
    @Path("get_widget_data_info")
    public String getWidgetDataInfo(
            @QueryParam("provider") String providerName,
            @QueryParam("widget_uid") String widgetUid,
            @QueryParam("name") String dataName) {
        return org.nuxeo.theme.webwidgets.Manager.getWidgetDataInfo(
                providerName, widgetUid, dataName);
    }

    @POST
    @Path("upload_file")
    public String uploadFile(@QueryParam("provider") String providerName,
            @QueryParam("widget_uid") String widgetUid,
            @QueryParam("data") String dataName) {
        HttpServletRequest req = ctx.getRequest();
        String res = org.nuxeo.theme.webwidgets.Manager.uploadFile(req,
                providerName, widgetUid, dataName);
        long timestamp = new Date().getTime();
        String src = String.format("nxwebwidgets://data/%s/%s/%s/%s",
                providerName, widgetUid, dataName, timestamp);
        org.nuxeo.theme.webwidgets.Manager.setWidgetPreference(providerName,
                widgetUid, dataName, src);
        return res;
    }

    @GET
    @Path("render_widget_data")
    public Response renderWidgetData(
            @QueryParam("widget_uid") String widgetUid,
            @QueryParam("data") String dataName,
            @QueryParam("provider") String providerName) {
        WidgetData data = org.nuxeo.theme.webwidgets.Manager.getWidgetData(
                providerName, widgetUid, dataName);
        ResponseBuilder builder = Response.ok(data.getContent());
        builder.type(data.getContentType());
        return builder.build();
    }

    @POST
    @Path("update_widget_preferences")
    public void updateWidgetPreferences() {
        FormData form = ctx.getForm();
        String providerName = form.getString("provider");
        String widgetUid = form.getString("widget_uid");
        String preferences_map = form.getString("preferences");
        Map preferencesMap = JSONObject.fromObject(preferences_map);
        org.nuxeo.theme.webwidgets.Manager.updateWidgetPreferences(
                providerName, widgetUid, preferencesMap);
    }

    @GET
    @Path("get_widget_decoration")
    public String getWidgetDecoration(
            @QueryParam("decoration") String decorationName) {
        return org.nuxeo.theme.webwidgets.Manager.getWidgetDecoration(decorationName);
    }

    @GET
    @Path("render_widget_icon")
    public Response renderWidgetIcon(@QueryParam("name") String widgetTypeName) {
        byte[] content = org.nuxeo.theme.webwidgets.Manager.getWidgetIconContent(widgetTypeName);
        ResponseBuilder builder = Response.ok(content);
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
        return ((Service) Framework.getRuntime().getComponent(Service.ID)).getWidgetCategories();
    }

    public static List<WidgetType> getWidgetTypes() {
        String widgetCategory = getSelectedWidgetCategory();
        return ((Service) Framework.getRuntime().getComponent(Service.ID)).getWidgetTypes(widgetCategory);
    }

}
