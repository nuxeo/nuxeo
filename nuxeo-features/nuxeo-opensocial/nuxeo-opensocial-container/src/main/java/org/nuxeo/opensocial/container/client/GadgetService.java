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

import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.view.ContainerPortal;
import org.nuxeo.opensocial.container.client.view.SavePreferenceAsyncCallback;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

/**
 * JSNI implementation of opensocial container
 *
 * @author 10044826
 */
public class GadgetService {

  /**
   * Registering avaible service
   */
  public static native void registerService()/*-{
      $wnd.gadgets.rpc.register('resize_iframe', @org.nuxeo.opensocial.container.client.GadgetService::resizeIframe(I));
      $wnd.gadgets.rpc.register('set_pref', @org.nuxeo.opensocial.container.client.GadgetService::setPref(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
      $wnd.gadgets.rpc.register('set_title', @org.nuxeo.opensocial.container.client.GadgetService::setTitle(Ljava/lang/String;));
      $wnd.gadgets.rpc.register('show_image', @org.nuxeo.opensocial.container.client.GadgetService::showImage(Lcom/google/gwt/core/client/JsArray;I));
    }-*/;

  /**
   * Resize gadget
   *
   * @param height
   */
  public static native void resizeIframe(int height) /*-{
     @org.nuxeo.opensocial.container.client.GadgetService::setHeight(Ljava/lang/String;I)(this.t,height);
   }-*/;

  public static void setHeight(String ref, int height) {
    ContainerPortal portal = ContainerEntryPoint.getContainerPortal();
    portal.getGadgetPortlet(ref)
        .setHeight(height + 20);
    portal.incrementLoading();
  };

  /**
   * Set new preference
   *
   * @param editToken
   * @param name
   * @param value
   */
  public static native void setPref(String editToken, String name, String value) /*-{
      for ( var i = 1, j = arguments.length; i < j; i += 2) {
        if(arguments[i]!="refresh") {
          @org.nuxeo.opensocial.container.client.GadgetService::setUserPref(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(this.t,arguments[i],arguments[i+1]);
        }
      }
      @org.nuxeo.opensocial.container.client.GadgetService::saveUserPref(Ljava/lang/String;)(this.t);
    }-*/;

  public static void saveUserPref(String ref) {
    JsLibrary.log("save user pref " + ref);
    GadgetBean bean = ContainerEntryPoint.getContainerPortal()
        .getGadgetPortlet(ref)
        .getGadgetBean();
    ContainerEntryPoint.getService()
        .saveGadgetPreferences(bean, null, ContainerEntryPoint.getGwtParams(),
            new SavePreferenceAsyncCallback<GadgetBean>(bean));
  };

  public static void setUserPref(String ref, String key, String value) {
    ContainerEntryPoint.getContainerPortal()
        .getGadgetPortlet(ref)
        .getGadgetBean()
        .setPref(key, value);
  };

  /**
   * Service : Set new title
   *
   * @param title
   */
  public static native void setTitle(String title) /*-{
      @org.nuxeo.opensocial.container.client.GadgetService::setTitleToGadget(Ljava/lang/String;Ljava/lang/String;)(this.t,title);
    }-*/;

  public static void setTitleToGadget(String ref, String title) {
    ContainerEntryPoint.getContainerPortal()
        .getGadgetPortlet(ref)
        .setTitle(title);
  };

  /**
   * Service : Show List of image in Container
   *
   * @param childs
   * @param current
   */
  public static native void showImage(JsArray<JavaScriptObject> childs,
      int current) /*-{
     var container = $wnd.$("#gadget-fancy");
     if(container.length == 0){
       container = $wnd.$("<div></div>").attr("id","gadget-fancy");
       $wnd.$($wnd.document.body).append(container);
     }
     container.html("");

     $wnd.$.each(childs, function(index, child) {
        var a = $wnd.$("<a></a>").attr('href',child.path.value+"@@viewVersion?v=Original").attr('class','fancyboxImage').attr('rel','photoGroup');
        container.append(a);
     });

     $wnd.$($wnd.$("a.fancyboxImage")).fancybox({
      'zoomSpeedIn': 500,
      'zoomSpeedOut': 500,
      'overlayShow': false,
      'forceImage': true,
      'hideOnContentClick': false
     });

     $wnd.$($wnd.$("a.fancyboxImage")[current]).click();
   }-*/;

  /**
   * Security : Setter of rpc relay
   *
   * @param iframeId
   * @param rpcToken
   */
  public static native void setRelayRpc(String iframeId, String serverBase)/*-{
        $wnd.gadgets.rpc.setRelayUrl(iframeId, serverBase + "files/container/rpc_relay.html");
      }-*/;

  /**
   * Security : Setter of Auth token
   *
   * @param iframeId
   * @param rpcToken
   */
  public static native void setAuthToken(String iframeId, String rpcToken)/*-{
        $wnd.gadgets.rpc.setAuthToken(iframeId, rpcToken);
      }-*/;

}
