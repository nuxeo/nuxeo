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

package org.nuxeo.opensocial.container.client;

/**
 * JSNI tools for call JS function in parent application
 *
 * @author Guillaume Cusnieux
 */
public class JsLibrary {

    public static native void loadingShow()
    /*-{
      if($wnd.loading_show)
        $wnd.loading_show();
    }-*/;

    public static native void loadingHide()
    /*-{
      if($wnd.loading_remove)
        $wnd.loading_remove();
    }-*/;

    public static native void log(String msg)
    /*-{
      if($wnd.console)
        $wnd.console.log(msg);
    }-*/;

    public static native void info(String msg)
    /*-{
      if($wnd.console)
        $wnd.console.info(msg);
    }-*/;

    public static native void error(String msg)
    /*-{
      if($wnd.console)
        $wnd.console.error(msg);
    }-*/;

    public static native void updateIframe(String id, String renderUrl)
    /*-{
      $wnd.document.getElementById(id).src = renderUrl;
    }-*/;

    public static native void showSearchBrowser(String id, String url)
    /*-{
      $wnd.jQuery("#"+id).load(url);
    }-*/;

    public static native void hideGwtContainerMask()
    /*-{
      $wnd.jQuery("#gwtContainerMask").hide();
    }-*/;

    public static native void showGwtContainerMask()
    /*-{
      $wnd.jQuery("#gwtContainerMask").show();
    }-*/;

    public static native void updateFrameWidth()
    /*-{
      $wnd.jQuery(".x-portlet").attr("style","width:100%");
      $wnd.jQuery(".x-panel-body").attr("style","width:100%");
      $wnd.jQuery(".gwt-Frame").width("100%");
    }-*/;

    public static native void updateColumnStyle()
    /*-{
      $wnd.jQuery(".x-portal-column").attr("style","padding: 5px;");
    }-*/;

    public static native String getNuxeoClientSideUrl()
    /*-{
      return top.nxBaseUrl;
    }-*/;
}
