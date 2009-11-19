package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.GadgetService;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetView;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;

import com.google.gwt.core.client.GWT;
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
  private GadgetForm form;
  private String view;

  public GadgetPortlet(GadgetBean gadget, String view) {
    super();
    this.gadget = gadget;
    this.view = view;
    buildPortlet();
    this.form = new GadgetForm(this);
    this.tools.setGadgetForm(form);
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
      renderPreference(p.getName(), p.getValue());
    }
  }

  public void renderPreference(String name, String value) {
    if (value == null)
      return;
    else if (DEFAULT_PREFS.isBorder(name)) {
      if ("FFFFFF".equals(value))
        removeBorder(this.getId());
      else
        changeBorderColor(this.getId(), value);
    } else if (DEFAULT_PREFS.isFont(name))
      changeTitleColor(this.getId(), value);
    else if (DEFAULT_PREFS.isHeader(name)) {
      changePortletTheme(value);
    }
  }

  private void changePortletTheme(String value) {
    if (ContainerEntryPoint.imagesGadget) {
      changeHeaderImage(this.getId(), buildImageUrl("bg_", value, ".jpg"));
      changeToolsColor(this.getId(), buildImageUrl("tools_", value, ".gif"));
    } else {
      changeHeaderColor(this.getId(), value);
    }
  }

  private String buildImageUrl(String prefix, String value, String extension) {
    return GWT.getModuleBaseURL() + "images/" + prefix + value + extension;
  }

  static String getIdWithRefAndView(String ref, String view) {
    if (view == null)
      view = DEFAULT_VIEW;
    return GADGET_CONTAINER + ref + "-" + view;
  }

  private Frame buildFrame() {
    reloadRenderUrl();
    Frame f = new Frame(this.gadget.getRenderUrl());
    f.setHeight("100%");
    f.setWidth("100%");
    Element elem = f.getElement();
    elem.setId(getIframeId());
    elem.setAttribute("overflow", "hidden");
    return f;
  }

  @Override
  public void setTitle(String title) {
    if (title != null) {
      super.setTitle(title);
      if (this.form != null)
        this.form.setTitle(title);
      if (this.tools != null)
        this.tools.setTitle(title);
    }
  }

  public void reloadRenderUrl() {
    String url = gadget.getRenderUrl();
    if (url == null) {
      JsLibrary.error("Render url of " + gadget.getName() + " is null");
      return;
    } else if (view.equals(CANVAS_VIEW))
      url = url.replaceAll(VIEW_KEY + DEFAULT_VIEW, VIEW_KEY + CANVAS_VIEW);
    else
      url = url.replaceAll(VIEW_KEY + CANVAS_VIEW, VIEW_KEY + DEFAULT_VIEW);
    gadget.setRenderUrl(url);
  }

  private String getIframeId() {
    return GADGET + view + "-" + this.gadget.getRef();
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
    if (this.gadget.isCollapse())
      collapse(getIdWithRefAndView(gadget.getRef(), view));
    super.afterRender();
    JsLibrary.updateFrameWidth();
    renderDefaultPreferences();
    updateFrameHeightIfContentTypeIsUrl();
  }

  private void updateFrameHeightIfContentTypeIsUrl() {
    GadgetView v = this.gadget.getView(view);
    if (v != null && "URL".equals(v.getContentType()))
      this.setHeight(1000);
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

  public GadgetForm getGadgetForm() {
    return form;
  }

  public void setView(String view) {
    this.view = view;
  }

  private static native void changeHeaderColor(String id, String color)
  /*-{
    $wnd.jQuery("#"+id).find("div.x-panel-tl").css("background-color","#"+color);
  }-*/;

  private static native void changeHeaderImage(String id, String url)
  /*-{
    $wnd.jQuery("#"+id).find("div.x-panel-tl").css("background-image","url("+url+")");
    $wnd.jQuery("#"+id).find("div.x-panel-tr").css("background-image","url("+url+")");
    $wnd.jQuery("#"+id).find("div.x-panel-tc").css("background-image","url("+url+")");
  }-*/;

  static native void changeToolsColor(String id, String url)
  /*-{
    $wnd.jQuery("#"+id).find("div.x-tool").css("background-image","url("+url+")");
  }-*/;

  static native void changeBorderColor(String id, String color)
  /*-{
    $wnd.jQuery("#"+id).find("div.x-panel-bwrap").css("border","2px solid #"+color);
  }-*/;

  static native void removeBorder(String id)
  /*-{
    $wnd.jQuery("#"+id).find("div.x-panel-bwrap").css("border","0px");
  }-*/;

  static native void changeTitleColor(String id, String color)
  /*-{
    $wnd.jQuery("#"+id).find("span.x-panel-header-text").css("color","#"+color);
  }-*/;

}