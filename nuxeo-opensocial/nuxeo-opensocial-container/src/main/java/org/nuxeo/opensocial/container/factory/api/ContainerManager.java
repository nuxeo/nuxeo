package org.nuxeo.opensocial.container.factory.api;

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

public interface ContainerManager {

  Container createContainer(Map<String, String> gwtParams)
      throws ClientException;

  GadgetBean addGadget(String gadgetName, Map<String, String> gwtParams)
      throws ClientException;

  Map<String, ArrayList<String>> getGadgetList() throws ClientException;

  Container saveLayout(Map<String, String> gwtParams, String layout)
      throws ClientException;

}
