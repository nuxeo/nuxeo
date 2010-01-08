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

  public static native void error(String msg)
  /*-{
    if($wnd.console)
      $wnd.console.error(msg);
  }-*/;

  public static native void updateIframe(String id, String renderUrl)
  /*-{
    $wnd.document.getElementById(id).src = renderUrl;
  }-*/;

  public static native String getSearchBrowserUrl(String ref, String prefName)
  /*-{
    if($wnd.browser){
      $wnd.browser.selectDocument = function(id, type, url, contextPath,from){
        if(type=="Picturebook" || type=="Univers"){
          $wnd.jQuery("#"+prefName).val($wnd.lmportal.getNuxeoServerBase() + contextPath);
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

  public static native void closeBrowserDoc()
  /*-{
    if($wnd.browser)
      $wnd.browser.selectDocument = null;
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
