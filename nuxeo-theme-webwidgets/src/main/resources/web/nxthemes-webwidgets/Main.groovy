package webwidgets

import java.io.*
import javax.ws.rs.*
import javax.ws.rs.core.*
import javax.ws.rs.core.Response.ResponseBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern
import net.sf.json.JSONObject
import org.nuxeo.ecm.core.rest.*
import org.nuxeo.ecm.webengine.forms.*
import org.nuxeo.ecm.webengine.model.*
import org.nuxeo.ecm.webengine.model.impl.*
import org.nuxeo.ecm.webengine.model.exceptions.*
import org.nuxeo.ecm.webengine.*
import org.nuxeo.runtime.api.Framework
import org.nuxeo.theme.*
import org.nuxeo.theme.elements.*
import org.nuxeo.theme.formats.*
import org.nuxeo.theme.formats.widgets.*
import org.nuxeo.theme.formats.styles.*
import org.nuxeo.theme.formats.layouts.*
import org.nuxeo.theme.events.*
import org.nuxeo.theme.fragments.*
import org.nuxeo.theme.presets.*
import org.nuxeo.theme.properties.*
import org.nuxeo.theme.templates.*
import org.nuxeo.theme.themes.*
import org.nuxeo.theme.types.*
import org.nuxeo.theme.perspectives.*
import org.nuxeo.theme.uids.*
import org.nuxeo.theme.views.*
import org.nuxeo.theme.webwidgets.*

@WebObject(type="nxthemes-webwidgets", guard="user=Administrator")

@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {
    
     @GET
     @Path("webWidgetFactory")
     public Object renderPerspectiveSelector(@QueryParam("org.nuxeo.theme.application.path") String path) {
        return getTemplate("webWidgetFactory.ftl").arg(
              "widget_categories", getWidgetCategories()).arg(
              "widget_types", getWidgetTypes()).arg(
              "selected_category", getSelectedWidgetCategory())
     }
    
    @GET
    @Path("get_panel_data")
    public String getPanelData(@QueryParam("provider") String providerName, @QueryParam("region") String regionName, @QueryParam("mode") String mode) {
       return org.nuxeo.theme.webwidgets.Manager.getPanelData(providerName, regionName, mode)
    }
    
    @POST
    @Path("add_widget")
    public void addWidget() {
        FormData form = ctx.getForm()
        String providerName = form.getString("provider")
        String widgetName = form.getString("widget_name")
        String regionName = form.getString("region")
        int order = form.getString("order") as Integer
        org.nuxeo.theme.webwidgets.Manager.addWidget(providerName, widgetName, regionName, order)
    }
    
    @POST
    @Path("move_widget")
    public String moveWidget() {
        FormData form = ctx.getForm()
        String srcProviderName = form.getString("src_provider")
        String destProviderName = form.getString("dest_provider")
        String srcUid = form.getString("src_uid")
        String srcRegionName = form.getString("src_region")
        String destRegionName = form.getString("dest_region")
        int destOrder = form.getString("dest_order") as Integer        
        return org.nuxeo.theme.webwidgets.Manager.moveWidget(srcProviderName, destProviderName, srcUid, srcRegionName, destRegionName, destOrder)
    }

    @POST
    @Path("remove_widget")
    public void removeWidget() {
        FormData form = ctx.getForm()
        String providerName = form.getString("provider")
        String widgetUid = form.getString("widget_uid")
        org.nuxeo.theme.webwidgets.Manager.removeWidget(providerName, widgetUid)
    }
    
    @POST
    @Path("set_widget_state")
    public void setWidgetState() {
        FormData form = ctx.getForm()
        String providerName = form.getString("provider")
        String widgetUid = form.getString("widget_uid")
        String state = form.getString("state")
        org.nuxeo.theme.webwidgets.Manager.setWidgetState(providerName, widgetUid, state)
    }
    
    @POST
    @Path("set_widget_category")
    public void setWidgetCategory() {
        FormData form = ctx.getForm()
        String category = form.getString("category")        
        SessionManager.setWidgetCategory(category)
    }
    
    @GET
    @Path("get_widget_data_info")
    public String getWidgetDataInfo(@QueryParam("provider") String providerName, @QueryParam("widget_uid") String widgetUid, @QueryParam("name") String dataName) {   
        return org.nuxeo.theme.webwidgets.Manager.getWidgetDataInfo(providerName,  widgetUid, dataName)
    }
    
    @POST
    @Path("upload_file")
    public String uploadFile(@QueryParam("provider") String providerName, @QueryParam("widget_uid") String widgetUid, @QueryParam("data") String dataName) {
        def req = ctx.getRequest()
        String res = org.nuxeo.theme.webwidgets.Manager.uploadFile(req, providerName, widgetUid, dataName)
        String timestamp = new Date().getTime()
        String src = String.format("nxwebwidgets://data/%s/%s/%s/%s", providerName, widgetUid, dataName, timestamp);
        org.nuxeo.theme.webwidgets.Manager.setWidgetPreference(providerName, widgetUid, dataName, src)
        return res
    }
    
    @GET
    @Path("render_widget_data")
    public Response renderWidgetData(@QueryParam("widget_uid") String widgetUid, @QueryParam("data") String dataName, @QueryParam("provider") String providerName) {
        WidgetData data = org.nuxeo.theme.webwidgets.Manager.getWidgetData(providerName, widgetUid, dataName)
        ResponseBuilder builder = Response.ok(data.getContent())
        builder.type(data.getContentType())
        return builder.build();
    }
    
    @POST
    @Path("update_widget_preferences")
    public void updateWidgetPreferences() {
        FormData form = ctx.getForm()
        String providerName = form.getString("provider")
        String widgetUid = form.getString("widget_uid")
        String preferences_map = form.getString("preferences")
        Map preferencesMap = JSONObject.fromObject(preferences_map)
        org.nuxeo.theme.webwidgets.Manager.updateWidgetPreferences(providerName, widgetUid, preferencesMap)
    }
        
    @GET
    @Path("get_widget_decoration")
    public String getWidgetDecoration(@QueryParam("decoration") String decorationName) {
        return org.nuxeo.theme.webwidgets.Manager.getWidgetDecoration(decorationName)
    }
    
    @GET
    @Path("render_widget_icon")
    public Response renderWidgetIcon(@QueryParam("name") String widgetTypeName) {
        byte[] content = org.nuxeo.theme.webwidgets.Manager.getWidgetIconContent(widgetTypeName)
        ResponseBuilder builder = Response.ok(content)
        // builder.type(???)
        return builder.build();
      }
    
    /* API */
  
    public static String getSelectedWidgetCategory() {
        String category = SessionManager.getWidgetCategory()
        if (category == null) {
            category = ""
        }
        return category
    }
    
    public static Set<String> getWidgetCategories() {
        Service service = Framework.getRuntime().getComponent("org.nuxeo.theme.webwidgets.Service")
        return service.getWidgetCategories()
    }
    
    public static List<WidgetType> getWidgetTypes() {
        String widgetCategory = getSelectedWidgetCategory()
        Service service = Framework.getRuntime().getComponent("org.nuxeo.theme.webwidgets.Service")
        return service.getWidgetTypes(widgetCategory)
    }
        
}

