package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
* @author Guillaume Cusnieux
*/
public class SaveGadgetAsyncCallback implements AsyncCallback<GadgetBean> {

  public void onFailure(Throwable arg0) {
    ContainerEntryPoint.getContainerPortal()
        .loader(1);
  }

  public void onSuccess(GadgetBean g) {
    ContainerEntryPoint.getContainerPortal()
        .loader(1);
  }

}
