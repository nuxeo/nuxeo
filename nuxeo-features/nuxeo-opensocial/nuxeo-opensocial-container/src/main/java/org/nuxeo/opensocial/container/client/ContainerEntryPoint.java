/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client;

import java.util.HashMap;

import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.service.api.ContainerService;
import org.nuxeo.opensocial.container.client.service.api.ContainerServiceAsync;
import org.nuxeo.opensocial.container.client.view.AddGadgetAsyncCallback;
import org.nuxeo.opensocial.container.client.view.ContainerPortal;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.layout.FitLayout;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 *
 * @author Guillaume Cusnieux
 */
public class ContainerEntryPoint implements EntryPoint {

    private static final String SERVICE_ENTRY_POINT = getContextPath()
            + "/gwtcontainer";

    private static final String GWT_WINDOW_WIDTH = "windowWidth";

    public static final int PANEL_WIDTH = 970;

    private static final int MARGIN_FROM_FULL_WIDTH = 70;

    private static final String GWT_CONTAINER_ID = "gwtContainer";

    private static final String CONTAINER_PANEL_ID = "containerPanel";

    private final static ContainerServiceAsync SERVICE = GWT.create(ContainerService.class);

    private final static ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

    private static final HashMap<String, String> GWT_PARAMS = new HashMap<String, String>();

    ServiceDefTarget endpoint = (ServiceDefTarget) SERVICE;

    private static ContainerPortal portal;

    private int windowWidth = PANEL_WIDTH; // for variable sizing

    public void onModuleLoad() {
        JsLibrary.loadingShow();
        endpoint.setServiceEntryPoint(SERVICE_ENTRY_POINT);
        JSONObject objects = JSONParser.parse(getInitialisationParams()).isObject();
        for (String key : objects.keySet()) {
            GWT_PARAMS.put(key, getGwtParam(objects, key));
        }

        if (!GWT_PARAMS.containsKey("locale")) {
            // Add the current GWT locale
            GWT_PARAMS.put("locale", LocaleInfo.getCurrentLocale().getLocaleName());
        }

        windowWidth = getWindowWidth(objects);

        SERVICE.getContainer(GWT_PARAMS, new AsyncCallback<Container>() {
            public void onFailure(Throwable object) {
                ContainerPortal.showErrorMessage(CONSTANTS.error(),
                        CONSTANTS.loadingError());
                JsLibrary.loadingHide();
            }

            public void onSuccess(final Container container) {
                GadgetService.registerService();
                Panel panel = new Panel();
                panel.setBorder(false);
                panel.setId(CONTAINER_PANEL_ID);
                panel.setLayout(new FitLayout());
                RootPanel.get(GWT_CONTAINER_ID).add(panel);
                portal = new ContainerPortal(container, panel);
                panel.setWidth(windowWidth);
                panel.setHeight("100%");
                JsLibrary.updateFrameWidth();
                JsLibrary.updateColumnStyle();
                createGwtContainerMask();

                Timer t = new Timer() {
                    @Override
                    public void run() {
                        portal.showPortlets();
                        ContainerEntryPoint.attachLayoutManager(
                                container.getLayout(), container.getStructure());
                        JsLibrary.loadingHide();
                    }
                };

                t.schedule(200);

            }
        });
    }

    private int getWindowWidth(JSONObject objects) {
        JSONValue value = objects.get(GWT_WINDOW_WIDTH);
        if (value != null) {
            JSONNumber width = value.isNumber();
            if (width != null) {
                windowWidth = (int) width.doubleValue();
                // we want a little border
                windowWidth -= MARGIN_FROM_FULL_WIDTH;
            }
        }
        return windowWidth;
    }

    private static String getGwtParam(JSONObject object, String key) {
        String value = object.get(key).toString();
        return value.substring(1, value.length() - 1);
    }

    public static ContainerServiceAsync getService() {
        return SERVICE;
    }

    public static ContainerPortal getContainerPortal() {
        return portal;
    }

    public static HashMap<String, String> getGwtParams() {
        return GWT_PARAMS;
    }

    static void chooseLayout(String name) {
        ContainerEntryPoint.getService().saveLayout(getGwtParams(), name,
                new AsyncCallback<Container>() {

                    Container container = portal.getContainer();

                    final String oldLayout = container.getLayout();

                    final int oldStructure = container.getStructure();

                    public void onFailure(Throwable arg0) {
                        JsLibrary.log("save layout Failed");
                    }

                    public void onSuccess(Container c) {
                        portal.updateColumnClassName(oldLayout, c.getLayout(),
                                oldStructure, c.getStructure());
                    }

                });
    };

    static void addGadget(String name) {
        SERVICE.addGadget(name, GWT_PARAMS,
                new AddGadgetAsyncCallback<GadgetBean>());
    };

    private static native String getInitialisationParams()
    /*-{
      if($wnd.getGwtParams)
        return $wnd.getGwtParams();
      return null;
    }-*/;

    private static native void createGwtContainerMask()
    /*-{
      $wnd.jQuery("#gwtContainer").append($wnd.jQuery("<div></div>").attr("id","gwtContainerMask"));
      $wnd.jQuery("#gwtContainerMask").hide();
    }-*/;

    private static native void attachLayoutManager(String layout,
            Integer boxSelected)
    /*-{
       //Initialisation
       $wnd.jQuery("a[box='"+boxSelected+"']").parent().removeClass("invisible").addClass("visible");
       $wnd.jQuery("#listBoxes>div").removeClass("selected");
       $wnd.jQuery("#listBoxes>div>button").removeClass("selected");
       $wnd.jQuery("button[box='"+boxSelected+"']").addClass("selected");
       $wnd.jQuery("button[box='"+boxSelected+"']").parent().addClass("selected");
       $wnd.jQuery("#"+layout).addClass("selected");

       //Choix du layout
       $wnd.jQuery(".typeLayout").click(function(){
         @org.nuxeo.opensocial.container.client.ContainerEntryPoint::chooseLayout(Ljava/lang/String;)($wnd.jQuery(this).attr("name"));
         return false
       });

       $wnd.jQuery(".directAdd>a").click(function(){
         @org.nuxeo.opensocial.container.client.ContainerEntryPoint::addGadget(Ljava/lang/String;)($wnd.jQuery(this).attr("name"));
         return false
       });
     }-*/;

    private static native String getContextPath()
    /*-{
      return top.nxContextPath;
    }-*/;

    public static boolean waitForGadgetsValidation() {
        String dndValidation = GWT_PARAMS.get("dndValidation");
        return dndValidation != null
                && "true".equals(dndValidation.toLowerCase());
    }

    /**
     * Returns {@code true} if the Gadgets preferences need to be displayed
     * after a gadget was added.
     * <p>
     * If the {@code showPreferences} parameter is not defined, default to
     * {@code true}
     */
    public static boolean showPreferencesAfterGadgetAddition() {
        String showPreferences = GWT_PARAMS.get("showPreferences");
        return showPreferences == null
                || "true".equals(showPreferences.toLowerCase());
    }

}
