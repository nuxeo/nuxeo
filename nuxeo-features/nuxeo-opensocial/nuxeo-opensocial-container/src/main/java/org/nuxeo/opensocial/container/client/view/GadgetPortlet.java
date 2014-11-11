/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.GadgetService;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetView;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Frame;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.portal.Portlet;

/**
 * @author Guillaume Cusnieux
 */
public class GadgetPortlet extends Portlet {

    private static final String NONE_PROPERTY = "none";

    private static final String PREFIX_PORTLET_ID = "portlet-";

    static final String PREFIX_FRAME_ID = "gadget-";

    public static final String CANVAS_VIEW = "canvas";

    public static final String DEFAULT_VIEW = "default";

    private GadgetBean gadget;

    private GadgetTools tools;

    private Frame frame;

    private GadgetForm form;

    private String view;

    public GadgetPortlet(GadgetBean gadget, String view) {
        super();
        this.gadget = gadget;
        this.view = view;
        buildPortlet();
        this.form = new GadgetForm(this);
        this.tools.setGadgetForm(form);
        this.setVisible(false);
    }

    public GadgetPortlet(GadgetBean bean) {
        this(bean, DEFAULT_VIEW);
    }

    private void buildPortlet() {
        this.setLayout(new FitLayout());
        this.setTitle(this.gadget.getTitle());
        if (!this.view.equals(DEFAULT_VIEW)) {
            this.setDraggable(false);
            this.setHideCollapseTool(true);
        } else {
            this.setDraggable(gadget.isEditable());
            if (!gadget.isConfigurable())
                this.setHideCollapseTool(true);
        }
        this.setHeight(this.gadget.getHeight());
        this.addListener(new PortletListener(this));
        this.frame = buildFrame();
        this.add(frame);
        this.setId(getIdWithRefAndView(gadget.getRef(), view));
        this.tools = new GadgetTools(this);
        this.setTools(tools.getButtons());
        GadgetService.setAuthToken(getIframeId(), this.gadget.getRef());
        GadgetService.setRelayRpc(getIframeId(), this.gadget.getRef());
    }

    static enum DEFAULT_PREFS {
        COLOR_header, COLOR_font, COLOR_border;

        public static boolean isHeader(String name) {
            return COLOR_header.name()
                    .equals(name);
        }

        public static boolean isFont(String name) {
            return COLOR_font.name()
                    .equals(name);
        }

        public static boolean isBorder(String name) {
            return COLOR_border.name()
                    .equals(name);
        }

    }

    void renderDefaultPreferences() {
        for (PreferencesBean p : this.gadget.getDefaultPrefs()) {
            renderPreference(p.getName(), (p.getValue() != null) ? p.getValue()
                    : p.getDefaultValue());
        }
    }

    public void renderPreference(String name, String value) {
        if (DEFAULT_PREFS.isBorder(name)) {
            if (!NONE_PROPERTY.equals(value))
                changeBorderColor(this.getId(), value);
            else
                removeBorderColor(this.getId());
        } else if (DEFAULT_PREFS.isFont(name)) {
            if (!NONE_PROPERTY.equals(value))
                changeTitleColor(this.getId(), value);
            else
                removeTitleColor(this.getId());
        } else if (DEFAULT_PREFS.isHeader(name)) {
            if (NONE_PROPERTY.equals(value))
                removeHeaderColor(this.getId());
            else
                changeHeaderColor(this.getId(), value);
        }
    }

    static String getIdWithRefAndView(String ref, String view) {
        if (view == null)
            view = DEFAULT_VIEW;
        return PREFIX_PORTLET_ID + view + "-" + ref;
    }

    static String getIdWithIframeId(String iframeId) {
        return iframeId.replace(PREFIX_FRAME_ID, PREFIX_PORTLET_ID);
    }

    private Frame buildFrame() {
        reloadRenderUrl();
        Frame f = new Frame(this.gadget.getRenderUrl());
        f.setHeight("100%");
        f.setWidth("100%");
        Element elem = f.getElement();
        elem.setId(getIframeId());
        elem.setAttribute("name", getIframeId());
        elem.setAttribute("overflow", "hidden");
        return f;
    }

    @Override
    public void setTitle(String title) {
        if (title != null) {
            super.setTitle(title);
            this.gadget.setTitle(title);
            if (this.form != null)
                this.form.setTitle(title);
            if (this.tools != null)
                this.tools.setTitle(title);
        }
    }

    public void setPortletTitle(String title) {
        if (title != null) {
            super.setTitle(title);
            if (this.form != null)
                this.form.setTitle(title);
        }
    }

    public void reloadRenderUrl() {
        String url = gadget.getRenderUrl();
        if (url == null) {
            return;
        }
        gadget.setRenderUrl(buildUrl(url, view));
    }

    private static native String buildUrl(String url, String view)
    /*-{
       var reg = new RegExp("view=[a-zA-Z]*&?");
       return url.replace(reg,"view="+view+"&");
    }-*/;

    String getIframeId() {
        return PREFIX_FRAME_ID + view + "-" + this.gadget.getRef();
    }

    public void doLayoutFrame() {
        JsLibrary.updateIframe(getIframeId(), this.gadget.getRenderUrl());
    }

    public void updateGadgetPortlet() {
        reloadRenderUrl();
        this.setGadgetBean(gadget);
        this.frame = buildFrame();
    }

    void setGadgetBean(GadgetBean bean) {
        this.gadget = bean;
        this.form.setGadget(bean);
    }

    public GadgetBean getGadgetBean() {
        return gadget;
    }

    @Override
    protected void afterRender() {
        if (this.gadget.isCollapsed())
            collapse(getIdWithRefAndView(gadget.getRef(), view),
                    "x-tmp-collapsed");
        super.afterRender();
        updateFrameHeightIfContentTypeIsUrl();
        Timer t = new Timer() {

            @Override
            public void run() {
                renderDefaultPreferences();
            }

        };
        t.schedule(200);

    }

    private void updateFrameHeightIfContentTypeIsUrl() {
        GadgetView v = this.gadget.getView(view);
        if (v != null && "URL".equals(v.getContentType()
                .toUpperCase())) {
            this.setHeight(600);
        }
    }

    static native void collapse(String id, String className)
    /*-{
      var p = $wnd.jQuery("#"+id);
      $wnd.jQuery(p).addClass("x-panel-collapsed "+ className);
      $wnd.jQuery(p.children()[1]).hide();
    }-*/;

    static native void unCollapse(String id, String idFrame, String url)
    /*-{
      var p = $wnd.jQuery("#"+id);
      $wnd.jQuery(p).removeClass("x-panel-collapsed");
      var f = $wnd.jQuery(p).children()[1];
      $wnd.jQuery(f).show();
      if($wnd.jQuery(p).hasClass("x-tmp-collapsed")) {
        $wnd.jQuery(p).removeClass("x-tmp-collapsed");
        $wnd.document.getElementById(idFrame).src = "";
        setTimeout(function(){
          $wnd.document.getElementById(idFrame).src = url;
          $wnd.jQuery($wnd.jQuery(p).children(".x-panel-body")).attr("style","overflow-x:auto;overflow-y:auto;");
        },50);
      }
    }-*/;

    public GadgetTools getTools() {
        return tools;
    }

    public void unCollapseGadget() {
        unCollapse(this.getId(), this.getIframeId(), this.gadget.getRenderUrl());
        this.gadget.setCollapsed(false);
    }

    public void collapseGadget() {
        collapse(this.getId(), "");
        this.gadget.setCollapsed(true);
    }

    public String getView() {
        return view;
    }

    public GadgetForm getGadgetForm() {
        return form;
    }

    public void setView(String view) {
        this.view = view;
    }

    private static native void removeHeaderColor(String id)
    /*-{
      $wnd.jQuery("#"+id).find("div.x-panel-tl").css("background","");
    }-*/;

    private static native void changeHeaderColor(String id, String color)
    /*-{
      $wnd.jQuery("#"+id).find("div.x-panel-tl").css("background-image","-webkit-gradient(linear,center top , #"+color+", #FFFFFF)");
      $wnd.jQuery("#"+id).find("div.x-panel-tl").css("background-image","-moz-linear-gradient(center top , #"+color+", #FFFFFF)");
      $wnd.jQuery("#"+id).find("div.x-panel-tl").css("background-color","#"+color);
    }-*/;

    static native void changeBorderColor(String id, String color)
    /*-{
      $wnd.jQuery("#"+id).find("div.x-panel-tl").css("border-bottom","1px solid #"+color);
      $wnd.jQuery("#"+id).attr("style","border:1px solid #"+color);
    }-*/;

    static native void removeBorderColor(String id)
    /*-{
      $wnd.jQuery("#"+id).find("div.x-panel-tl").css("border-bottom","");
      $wnd.jQuery("#"+id).attr("style","");
    }-*/;

    static native void removeBorder(String id)
    /*-{
      $wnd.jQuery("#"+id).find("div.x-panel-bwrap").css("border","0px");
    }-*/;

    static native void changeTitleColor(String id, String color)
    /*-{
      $wnd.jQuery("#"+id).find("span.x-panel-header-text").css("color","#"+color);
    }-*/;

    static native void removeTitleColor(String id)
    /*-{
      $wnd.jQuery("#"+id).find("span.x-panel-header-text").css("color","");
    }-*/;

    public void renderTitle() {
        this.setTitle(this.gadget.getTitle());
    }

    public void removeStyle() {
        _removeStyle(this.id);
    };

    private native static void _removeStyle(String id)
    /*-{
        $wnd.jQuery("#"+id).attr("style","");
    }-*/;

}
