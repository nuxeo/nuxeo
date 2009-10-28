package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.PanelListenerAdapter;

/**
 * Portlet Listener serve for catch portlet event's and save collapsed
 *
 * @author Guillaume Cusnieux
 */
public class PortletListener extends PanelListenerAdapter {

  private GadgetBean gadget;

  public PortletListener(GadgetBean gadget) {
    this.gadget = gadget;
  }

  @Override
  public boolean doBeforeCollapse(Panel panel, boolean animate) {
    if (gadget.isCollapse()) {
      GadgetPortlet.unCollapse(
          GadgetPortlet.GADGET_CONTAINER + gadget.getRef(),
          GadgetPortlet.GADGET + gadget.getRef(), gadget.getRenderUrl());
      gadget.setCollapse(false);
    } else {
      GadgetPortlet.collapse(GadgetPortlet.GADGET_CONTAINER + gadget.getRef());
      gadget.setCollapse(true);
    }
    saveCollapsed(gadget.isCollapse());
    return false;
  }

  private void saveCollapsed(boolean collapsed) {
    gadget.setCollapse(collapsed);
    ContainerEntryPoint.getService()
        .saveGadgetCollapsed(gadget, ContainerEntryPoint.getGwtParams(),
            new SaveGadgetAsyncCallback());
  }

}
