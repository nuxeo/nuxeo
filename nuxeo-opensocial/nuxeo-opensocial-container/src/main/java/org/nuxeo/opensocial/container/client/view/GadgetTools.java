package org.nuxeo.opensocial.container.client.view;

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

  public GadgetTools(GadgetPortlet portlet) {
    this.portlet = portlet;
  }

  public Tool[] getButtons() {
    if (portlet.getView()
        .equals(GadgetPortlet.CANVAS_VIEW)) {
      return getCanvasButtons();
    } else {
      return getDefaultButtons();
    }
  }

  private Tool[] getCanvasButtons() {
    final Tool min = new Tool(Tool.MINIMIZE, new Function() {
      public void execute() {
        ContainerPortal portal = ContainerEntryPoint.getContainerPortal();
        PortalColumn maximizedCol = portal.getMaximizedCol();
        maximizedCol.remove(portlet.getId(), true);
        showManager();
        for (PortalColumn col : portal.getPortalColumns()) {
          col.show();
          col.doLayout();
        }
        ;
        maximizedCol.hide();
        maximizedCol.doLayout();
      }

    });
    return new Tool[] { min };

  }

  private Tool[] getDefaultButtons() {
    final GadgetBean gadget = portlet.getGadgetBean();
    if (gadget.getPermission()) {
      Tool gear = new Tool(Tool.GEAR, new Function() {
        public void execute() {
         launchGear();
        }
      });

      Tool close = new Tool(Tool.CLOSE, new Function() {
        public void execute() {
          if (Window.confirm(MSG.askedDeleteGadget(gadget.getTitle()))) {
            ContainerEntryPoint.getService()
                .removeGadget(gadget, ContainerEntryPoint.getGwtParams(),
                    new AsyncCallback<GadgetBean>() {
                      public void onFailure(Throwable arg0) {
                        ContainerPortal.showErrorMessage(CST.error(),
                            CST.deleteError());
                      }

                      public void onSuccess(GadgetBean gadget) {
                        ContainerEntryPoint.getContainerPortal()
                            .removeGadgetPortlet(portlet.getId());
                      }
                    });
          }
        }

      });
      Tool max = new Tool(Tool.MAXIMIZE, new Function() {

        public void execute() {
          ContainerPortal portal = ContainerEntryPoint.getContainerPortal();
          final GadgetBean gadget = portlet.getGadgetBean();
          PortalColumn maximizedCol = portal.getMaximizedCol();
          GadgetPortlet canvas = new GadgetPortlet(gadget,
              GadgetPortlet.CANVAS_VIEW);
          maximizedCol.add(canvas);
          hideManager();
          for (PortalColumn col : portal.getPortalColumns()) {
            col.doLayout();
            col.hide();
          }
          ;
          maximizedCol.show();
          canvas.updateGadgetPortlet(gadget);
          canvas.doLayout();
          maximizedCol.doLayout();
          if (!portal.isCollapsed())
            canvas.unCollapseGadget();
        }

      });

      return new Tool[] { max, gear, close };
    }
    return new Tool[] {};

  }

  public GadgetForm getGadgetForm() {
    return form;
  }

  private static native void hideManager()
  /*-{
    $wnd.jQuery(".manager").slideUp("fast");
    $wnd.jQuery(".getManager").slideUp("fast");
  }-*/;

  private static native void showManager()
  /*-{
   $wnd.jQuery(".getManager").slideDown("fast");
  }-*/;

  public void launchGear() {
    form = new GadgetForm(portlet);
    form.showForm();
  }
}