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
