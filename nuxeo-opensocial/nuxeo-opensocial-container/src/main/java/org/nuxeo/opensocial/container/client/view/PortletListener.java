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

  private GadgetPortlet portlet;

  public PortletListener(GadgetPortlet portlet) {
    this.portlet = portlet;
  }

  @Override
  public boolean doBeforeCollapse(Panel panel, boolean animate) {
    GadgetBean gadget = portlet.getGadgetBean();
    if (gadget.isCollapse()) {
      portlet.unCollapseGadget();
    } else {
      portlet.collapseGadget();
    }
    saveCollapsed(gadget.isCollapse());
    return false;
  }

  private void saveCollapsed(boolean collapsed) {
    GadgetBean gadget = portlet.getGadgetBean();
    gadget.setCollapse(collapsed);
    ContainerEntryPoint.getService()
        .saveGadgetCollapsed(gadget, ContainerEntryPoint.getGwtParams(),
            new SaveGadgetAsyncCallback());
  }
}
