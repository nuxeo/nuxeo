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

/**
 * JSNI tools for call JS function in parent application
 * 
 * @author 10044826
 */
public class JsLibrary {

    public static native void loadingShow() /*-{
            $wnd.loading_show();
          }-*/;

    public static native void loadingHide() /*-{
            $wnd.loading_remove();
          }-*/;

    public static native void log(String msg) /*-{
           if($wnd.console)
             $wnd.console.log(msg);
         }-*/;

    public static native void error(String msg) /*-{
             if($wnd.console)
               $wnd.console.error(msg);
           }-*/;

    public static native void updateIframe(String id, String renderUrl) /*-{
             $wnd.document.getElementById(id).src = renderUrl;
           }-*/;

    public static native String getSearchBrowserUrl(String ref, String prefName) /*-{
          if($wnd.browser){
          $wnd.browser.selectDocument = function(id, type, url, contextPath,from){
               if(type=="Picturebook" || type=="Univers"){
                 $wnd.jQuery("#"+prefName).val($wnd.lmportal.getNuxeoServerBase() + contextPath);
                 @org.nuxeo.opensocial.container.client.JsLibrary::close(Ljava/lang/String;)(ref);
               }
             };
             var type = prefName.split("_")[1];
             var url = $wnd.document.location.href.split("?")[0];
             var space = "VIEW";
             if(type=="Picturebook")
               space = "WORK";
             return url + "@@browseserver?width=850&height=450&type=" + type + "&workspace=" + space;
            }
            return "";
           }-*/;

    public static native void closeBrowserDoc() /*-{
                if($wnd.browser){
                       $wnd.browser.selectDocument = null;
                   }
               }-*/;

    public static void close(String ref) {
        ContainerEntryPoint.getContainerPortal().getGadgetPortlet(ref).getTools().getGadgetForm().closeSearchBrowser();
    };

    public static native void showSearchBrowser(String id, String url) /*-{
             $wnd.jQuery("#"+id).load(url);
           }-*/;

    public static native void hideGwtContainerMask() /*-{
              $wnd.jQuery("#gwtContainerMask").hide();
          }-*/;

    public static native void removePossibleColumn() /*-{
           $wnd.jQuery(".x-column-possible").removeClass("x-column-possible");
         }-*/;

    public static native void showGwtContainerMask() /*-{
            $wnd.jQuery("#gwtContainerMask").show();
          }-*/;

    public static native void reduceGhostPanel() /*-{
             var pos = $wnd.jQuery(".x-panel-ghost").position();
             var posx = 0;
             $wnd.jQuery($wnd.document).one("mousemove",function(e){
               if (e.pageX || e.pageY)
                 posx = e.pageX;
               else if (e.clientX || e.clientY)
                 posx = e.clientX + $wnd.document.body.scrollLeft + $wnd.document.documentElement.scrollLeft;
               var newPos = posx - 70 - pos.left;
               $wnd.jQuery(".x-panel-ghost").animate({
                   marginLeft: newPos+"px"
                 }, 100 );
             });
             $wnd.jQuery(".x-panel-ghost").width("140px").height("25px");
             $wnd.jQuery(".x-column").addClass("x-column-possible");
            }-*/;

    public static native void updateFrameHeight()/*-{
            $wnd.jQuery(".x-panel-body").attr("style","width:100%");
            $wnd.jQuery(".gwt-Frame").attr("style","width:100%");
            $wnd.jQuery(".gwt-Frame").attr("style","height:200px");
          }-*/;

    public static native void updateColumnStyle() /*-{
             $wnd.jQuery(".x-portal-column ").attr("style","padding: 5px 5px 0px;");
             $wnd.jQuery(".x-portlet ").attr("style","width:100%");
           }-*/;

    public static native void hideAndShowGadget(String id) /*-{
         $wnd.jQuery("#"+id).hide();
         $wnd.jQuery("#"+id).fadeIn("slow");
        }-*/;

    public static native String getNuxeoClientSideUrl()/*-{
                return top.nxBaseUrl;
          }-*/;
}
