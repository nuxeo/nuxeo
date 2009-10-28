package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.GadgetService;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Frame;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.portal.Portlet;
/**
* @author Guillaume Cusnieux
*/
public class GadgetPortlet extends Portlet {

  static final String GADGET = "gadget-";
  public static final String GADGET_CONTAINER = "gadget-container-";
  public static final String FRAME_CONTAINER = "frame-container-";

  private GadgetBean gadget;
  private GadgetTools tools;
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

  private Frame buildFrame() {
    Frame f = new Frame(this.gadget.getRenderUrl());
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
    JsLibrary.updateIframe(getIframeId(), bean.getRenderUrl());
    this.setGadgetBean(bean);
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

  static native void collapse(String id)
  /*-{
    var p = $wnd.$("#"+id);
    $wnd.$(p).addClass("x-panel-collapsed");
    $wnd.$(p.children()[1]).hide();
  }-*/;

  static native void unCollapse(String id, String idFrame, String url)
  /*-{
    var p = $wnd.$("#"+id);
    $wnd.$(p).removeClass("x-panel-collapsed");
    var f = $wnd.$(p).children()[1];
    $wnd.$(f).show();
    if($wnd.$(f).height() < 20) {
      $wnd.document.getElementById(idFrame).src = "";
      setTimeout(function(){
        $wnd.document.getElementById(idFrame).src = url;
        $wnd.$($wnd.$(p).children(".x-panel-body")).attr("style","overflow-x:auto;overflow-y:auto;");
      },50);
    }
  }-*/;

}