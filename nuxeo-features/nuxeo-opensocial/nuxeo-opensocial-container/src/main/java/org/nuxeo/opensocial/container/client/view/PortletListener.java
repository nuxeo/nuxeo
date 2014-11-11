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

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.PanelListenerAdapter;

/**
 * Portlet Listener serve for catch portlet event's and save collapsed
 *
 * @author 10044826
 *
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
