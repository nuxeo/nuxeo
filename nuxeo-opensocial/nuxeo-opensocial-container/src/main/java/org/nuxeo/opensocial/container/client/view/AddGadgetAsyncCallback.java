package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AddGadgetAsyncCallback<T> implements AsyncCallback<GadgetBean> {

  private final static ContainerConstants CONSTANTS = GWT.create(ContainerConstants.class);

  public void onFailure(Throwable arg0) {
    ContainerPortal.showErrorMessage(CONSTANTS.error(),
        CONSTANTS.addGadgetError());
  }

  public void onSuccess(GadgetBean bean) {
    ContainerEntryPoint.getContainerPortal().addGadget(bean);
  }

}
