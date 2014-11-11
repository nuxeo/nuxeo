package org.nuxeo.opensocial.container.client.view;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.ContainerMessages;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.core.Function;
import com.gwtext.client.widgets.Tool;
import com.gwtext.client.widgets.portal.PortalColumn;

/**
 * @author Guillaume Cusnieux
 */
public class GadgetTools {

    private final static ContainerConstants CST = GWT.create(ContainerConstants.class);

    private final static ContainerMessages MSG = GWT.create(ContainerMessages.class);

    private GadgetForm form;

    private GadgetPortlet portlet;

    private String title;

    public GadgetTools(GadgetPortlet portlet) {
        this.portlet = portlet;
        this.title = portlet.getTitle();
    }

    public Tool[] getButtons() {
        if (portlet.getView().equals(GadgetPortlet.CANVAS_VIEW)) {
            return getCanvasButtons();
        } else {
            return getDefaultButtons();
        }
    }

    private Tool[] getCanvasButtons() {
        final Tool min = new Tool(Tool.MINIMIZE, new Function() {
            public void execute() {
                minimize();
            }

        });
        return new Tool[] { min };

    }

    public void minimize() {
        ContainerPortal portal = ContainerEntryPoint.getContainerPortal();
        PortalColumn maximizedCol = portal.getMaximizedCol();
        maximizedCol.remove(portlet.getId(), true);
        showManager();
        for (PortalColumn col : portal.getPortalColumns()) {
            col.show();
        }
        ;
        maximizedCol.hide();
        updateLayoutSizeForMin(ContainerEntryPoint.PANEL_WIDTH + "px");
    }

    private native void updateLayoutSizeForMin(String width)
    /*-{
      $wnd.jQuery("#containerPortal").width(width);
      $wnd.jQuery(".containerPortal").width(width);
      $wnd.jQuery(".x-column-inner").width(width);
      $wnd.jQuery("#containerPanel").width(width);
    }-*/;

    private Tool[] getDefaultButtons() {
        final GadgetBean gadget = portlet.getGadgetBean();
        Tool gear = new Tool(Tool.GEAR, new Function() {
            public void execute() {
                launchGear();
            }
        });

        Tool close = new Tool(Tool.CLOSE, new Function() {
            public void execute() {
                if (Window.confirm(MSG.askedDeleteGadget((title != null) ? title
                        : ""))) {
                    portlet.hide();
                    ContainerEntryPoint.getService().removeGadget(gadget,
                            ContainerEntryPoint.getGwtParams(),
                            new AsyncCallback<GadgetBean>() {
                                public void onFailure(Throwable arg0) {
                                    ContainerPortal.showErrorMessage(
                                            CST.error(), CST.deleteError());
                                }

                                public void onSuccess(GadgetBean gadget) {
                                    ContainerEntryPoint.getContainerPortal().removeGadgetPortlet(
                                            portlet.getId());
                                }
                            });
                }
            }

        });

        Tool max = new Tool(Tool.MAXIMIZE, new Function() {

            public void execute() {
                maximize(GadgetPortlet.CANVAS_VIEW);
            }

        });

        List<Tool> tools = new ArrayList<Tool>();

        if (gadget.getView(GadgetPortlet.CANVAS_VIEW) != null)
            tools.add(max);

        if (gadget.isConfigurable())
            tools.add(gear);

        if (gadget.isEditable())
            tools.add(close);

        Tool[] array = tools.toArray(new Tool[tools.size()]);
        return array;
    }

    public void maximize(String view) {
        GadgetBean gadget = portlet.getGadgetBean();
        ContainerPortal portal = ContainerEntryPoint.getContainerPortal();
        PortalColumn maximizedCol = portal.getMaximizedCol();
        GadgetPortlet canvas = new GadgetPortlet(gadget, view);
        ContainerPortal.setMaximizedPortlet(canvas);
        maximizedCol.add(canvas);
        hideManager();
        for (PortalColumn col : portal.getPortalColumns()) {
            col.hide();
        }
        ;
        maximizedCol.show();
        canvas.show();
        canvas.updateGadgetPortlet();
        canvas.doLayout();
        maximizedCol.doLayout();
        updateLayoutSizeForMax(canvas.getId());
        if (gadget.isCollapsed()) {
            canvas.unCollapseGadget();
            gadget.setCollapsed(true);
        }
    }

    private native void updateLayoutSizeForMax(String id)
    /*-{
    $wnd.jQuery("#containerPortal").width("100%");
    $wnd.jQuery(".containerPortal").width("100%");
    $wnd.jQuery("#containerPanel").width("100%");
    $wnd.jQuery(".x-column-inner").width("100%");
    $wnd.jQuery("#maximizedCol").attr("style","width:100%;padding:0;margin:0;");
    $wnd.jQuery("#"+id).attr("style","width:100%;paddinf:0;");
    }-*/;

    public void setGadgetForm(GadgetForm form) {
        this.form = form;
    }

    private static native void hideManager()
    /*-{
      $wnd.jQuery(".managerContainer").slideUp();
      $wnd.jQuery(".managerContainer").hide();
    }-*/;

    private static native void showManager()
    /*-{
      $wnd.jQuery(".managerContainer").slideDown();
    }-*/;

    public void launchGear() {
        if (form != null)
            form.showForm();
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
