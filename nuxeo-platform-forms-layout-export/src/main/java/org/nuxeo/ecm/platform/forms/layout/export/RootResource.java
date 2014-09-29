package org.nuxeo.ecm.platform.forms.layout.export;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
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
    public Object doGet(@Context
    UriInfo uriInfo) throws Exception {
        LayoutStore service = Framework.getService(LayoutStore.class);
        // XXX: use hard coded "jsf" category for now
        int nbWidgetTypes = service.getWidgetTypeDefinitions("jsf").size();
        int nbLayoutTypes = service.getLayoutTypeDefinitions("jsf").size();
        int nbLayouts = service.getLayoutDefinitionNames("jsf").size();
        return getTemplate("index.ftl", uriInfo).arg("nbWidgetTypes",
                Integer.valueOf(nbWidgetTypes)).arg("nbLayouts",
                Integer.valueOf(nbLayouts)).arg("nbLayoutTypes",
                Integer.valueOf(nbLayoutTypes));
    }

    @Path("layouts")
    public Object getLayouts() throws Exception {
        // XXX: use hard coded "jsf" category for now
        return new LayoutResource("jsf");
    }

    @Path("widget-types")
    public Object getWidgetTypes(@QueryParam("widgetTypeCategory")
    String widgetTypeCategory) throws Exception {
        if (StringUtils.isBlank(widgetTypeCategory)) {
            widgetTypeCategory = "jsf";
        }
        return new WidgetTypeResource(widgetTypeCategory);
    }

    @Path("layout-types")
    public Object getLayoutTypes(@QueryParam("layoutTypeCategory")
    String layoutTypeCategory) throws Exception {
        if (StringUtils.isBlank(layoutTypeCategory)) {
            layoutTypeCategory = "jsf";
        }
        return new LayoutTypeResource(layoutTypeCategory);
    }

}
