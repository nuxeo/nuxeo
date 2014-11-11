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
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class SavePreferenceAsyncCallback<T> implements
    AsyncCallback<GadgetBean> {

  private GadgetBean gadget;
  private final static ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

  public SavePreferenceAsyncCallback(GadgetBean gadget) {
    super();
    this.gadget = gadget;
  }

  public void onFailure(Throwable tr) {
    JsLibrary.loadingHide();
    ContainerPortal.showErrorMessage(CONSTANTS.error(),
        CONSTANTS.savePreferencesError());
  }

  public void onSuccess(GadgetBean bean) {
    ContainerPortal c = ContainerEntryPoint.getContainerPortal();
    c.getGadgetPortlet(gadget.getRef())
        .updateGadgetPortlet(bean);
    c.loader(ContainerEntryPoint.DEFAULT_TIMEOUT);
    JsLibrary.loadingHide();
  }

}
