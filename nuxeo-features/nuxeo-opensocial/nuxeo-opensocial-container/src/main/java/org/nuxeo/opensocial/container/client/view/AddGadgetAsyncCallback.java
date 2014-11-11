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

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * AddGadgetAsyncCallback
 * 
 * @author Guillaume Cusnieux
 */
public class AddGadgetAsyncCallback<T> implements AsyncCallback<GadgetBean> {

    private final static ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

    public void onFailure(Throwable arg0) {
        ContainerPortal.showErrorMessage(CONSTANTS.error(),
                CONSTANTS.addGadgetError());
    }

    public void onSuccess(GadgetBean bean) {
        GadgetPortlet port = ContainerEntryPoint.getContainerPortal().addGadget(
                bean);
        port.getTools().launchGear();
    }

}
