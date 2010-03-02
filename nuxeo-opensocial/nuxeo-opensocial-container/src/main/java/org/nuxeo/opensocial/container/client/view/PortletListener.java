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

import com.google.gwt.user.client.rpc.AsyncCallback;
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
        if (gadget.isCollapsed()) {
            portlet.unCollapseGadget();
        } else {
            portlet.collapseGadget();
        }
        saveCollapsed(gadget.isCollapsed());
        return false;
    }

    private void saveCollapsed(boolean collapsed) {
        GadgetBean gadget = portlet.getGadgetBean();
        gadget.setCollapsed(collapsed);
        ContainerEntryPoint.getService().saveGadget(gadget,
                ContainerEntryPoint.getGwtParams(),
                new AsyncCallback<GadgetBean>() {

                    public void onFailure(Throwable arg0) {
                    }

                    public void onSuccess(GadgetBean arg0) {
                    }
                });
    }
}
