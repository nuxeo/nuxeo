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

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.GadgetService;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Frame;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.portal.Portlet;

public class GadgetPortlet extends Portlet {

    static final String GADGET = "gadget-";

    public static final String GADGET_CONTAINER = "gadget-container-";

    public static final String FRAME_CONTAINER = "frame-container-";

    private GadgetBean gadget;

    private final GadgetTools tools;

    private Frame frame;

    public GadgetPortlet(GadgetBean gadget) {
        super();
        this.gadget = gadget;
        this.tools = new GadgetTools(gadget.getRef());
        buildPortlet();
    }

    private void buildPortlet() {
        this.setLayout(new FitLayout());
        this.setTools(tools.getButtons(gadget.getPermission()));
        this.setHideCollapseTool(!this.gadget.getPermission());
        this.setTitle(this.gadget.getTitle());
        this.setDraggable(gadget.getPermission());
        this.addListener(new PortletListener(gadget));
        this.frame = buildFrame();
        this.add(frame);
        this.setId(getGadgetId());
        GadgetService.setAuthToken(getIframeId(), this.gadget.getRef());
        GadgetService.setRelayRpc(getIframeId(), "");
    }

    private String getGadgetId() {
        return GADGET_CONTAINER + this.gadget.getRef();
    }

    private String getClientVirtualHostedUrl(String renderUrl) {
        // JsLibrary.log("call getClientVirtualHostedUrl on url " + renderUrl);
        String clientSiteBaseUrl = JsLibrary.getNuxeoClientSideUrl();
        if (clientSiteBaseUrl == null) {
            JsLibrary.error("unable to get Client Side url from top");
            return renderUrl;
        } else {
            // XXX this is a hack : should do better than that !!!
            String[] parts = renderUrl.split("/nuxeo/");
            String oldServerUrl = parts[0];
            String newServerUrl = clientSiteBaseUrl.replace("/nuxeo/", "");
            String newurl = renderUrl.replaceAll(oldServerUrl, newServerUrl);
            // JsLibrary.log("computed url =" + newurl);
            return newurl;
        }
    }

    private Frame buildFrame() {
        String iFrameUrl = getClientVirtualHostedUrl(gadget.getRenderUrl());
        Frame f = new Frame(iFrameUrl);
        f.setHeight("100%");
        f.setWidth("100%");
        Element elem = f.getElement();
        elem.setId(getIframeId());
        elem.setAttribute("overflow", "hidden");
        return f;
    }

    private String getIframeId() {
        return GADGET + this.gadget.getRef();
    }

    public void updateGadgetPortlet(GadgetBean bean) {
        String iFrameUrl = getClientVirtualHostedUrl(gadget.getRenderUrl());
        JsLibrary.updateIframe(getIframeId(), iFrameUrl);
        this.setGadgetBean(bean);
        this.setTitle(bean.getTitle());
        this.frame = buildFrame();
    }

    private void setGadgetBean(GadgetBean bean) {
        this.gadget = bean;
    }

    public GadgetBean getGadgetBean() {
        return gadget;
    }

    public GadgetTools getTools() {
        return tools;
    }

    @Override
    protected void afterRender() {
        if (this.gadget.isCollapse())
            collapse(GADGET_CONTAINER + this.gadget.getRef());
        super.afterRender();
        JsLibrary.updateFrameHeight();
    }

    static native void collapse(String id) /*-{
           var p = $wnd.jQuery("#"+id);
           $wnd.jQuery(p).addClass("x-panel-collapsed");
           $wnd.jQuery(p.children()[1]).hide();
         }-*/;

    static native void unCollapse(String id, String idFrame, String url) /*-{
           var p = $wnd.jQuery("#"+id);
           $wnd.jQuery(p).removeClass("x-panel-collapsed");
           var f = $wnd.jQuery(p).children()[1];
           $wnd.jQuery(f).show();
           if($wnd.jQuery(f).height() < 20) {
              $wnd.document.getElementById(idFrame).src = "";
              setTimeout(function(){
                $wnd.document.getElementById(idFrame).src = url;
                $wnd.jQuery($wnd.jQuery(p).children(".x-panel-body")).attr("style","overflow-x:auto;overflow-y:auto;");
              },50);
           }
         }-*/;

}
