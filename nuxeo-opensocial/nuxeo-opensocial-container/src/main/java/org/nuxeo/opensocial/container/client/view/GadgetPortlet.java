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
  private static final String GADGET_CONTAINER = "portlet-";
  public static final String CANVAS_VIEW = "canvas";
  public static final String DEFAULT_VIEW = "default";
  private static final String VIEW_KEY = "&view=";

  private GadgetBean gadget;
  private GadgetTools tools;
  private Frame frame;
  private String view;

  public GadgetPortlet(GadgetBean gadget, String view) {
    super();
    this.gadget = gadget;
    this.view = view;
    buildPortlet();
  }

  public GadgetPortlet(GadgetBean bean) {
    this(bean, DEFAULT_VIEW);
  }

  private void buildPortlet() {
    this.setLayout(new FitLayout());
    this.setTitle(this.gadget.getTitle());
    if (this.view.equals(CANVAS_VIEW)) {
      this.setDraggable(false);
      this.setHideCollapseTool(true);
    } else {
      this.setDraggable(gadget.getPermission());
      this.setHideCollapseTool(!this.gadget.getPermission());
    }
    this.addListener(new PortletListener(this));
    this.frame = buildFrame();
    this.add(frame);
    this.setId(getIdWithRefAndView(gadget.getRef(), view));
    this.tools = new GadgetTools(this);
    this.setTools(tools.getButtons());
    GadgetService.setAuthToken(getIframeId(), this.gadget.getRef());
    GadgetService.setRelayRpc(getIframeId(), this.gadget.getRef());
  }

  public static String getIdWithRefAndView(String ref, String view) {
    if (view == null)
      view = DEFAULT_VIEW;
    return GADGET_CONTAINER + ref + "-" + view;
  }

  private Frame buildFrame() {
    Frame f = new Frame(this.gadget.getRenderUrl());
    String urlView = gadget.getRenderUrl();
    if (view.equals(CANVAS_VIEW)) {
      urlView = urlView.replaceAll(VIEW_KEY + DEFAULT_VIEW, VIEW_KEY
          + CANVAS_VIEW);
    } else {
      urlView = urlView.replaceAll(VIEW_KEY + CANVAS_VIEW, VIEW_KEY
          + DEFAULT_VIEW);
    }
    gadget.setRenderUrl(urlView);
    f.setHeight("100%");
    f.setWidth("100%");
    Element elem = f.getElement();
    elem.setId(getIframeId());
    elem.setAttribute("overflow", "hidden");
    return f;
  }

  private String getIframeId() {
    return GADGET + view + "-" + this.gadget.getRef();
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

  @Override
  protected void afterRender() {
    if (this.gadget.isCollapse())
      collapse(getIdWithRefAndView(gadget.getRef(), view));
    super.afterRender();
    JsLibrary.updateFrameHeight();
  }

  static native void collapse(String id)
  /*-{
    var p = $wnd.jQuery("#"+id);
    $wnd.jQuery(p).addClass("x-panel-collapsed");
    $wnd.jQuery(p.children()[1]).hide();
  }-*/;

  static native void unCollapse(String id, String idFrame, String url)
  /*-{
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

  public GadgetTools getTools() {
    return tools;
  }

  public void unCollapseGadget() {
    unCollapse(this.getId(), this.getIframeId(), this.gadget.getRenderUrl());
    this.gadget.setCollapse(false);

  }

  public void collapseGadget() {
    collapse(this.getId());
    this.gadget.setCollapse(true);
  }

  public String getView() {
    return view;
  }

}