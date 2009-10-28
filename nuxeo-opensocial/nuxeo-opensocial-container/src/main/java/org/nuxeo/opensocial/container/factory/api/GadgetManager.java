package org.nuxeo.opensocial.container.factory.api;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
/**
* @author Guillaume Cusnieux
*/
public interface GadgetManager {

  void savePosition(GadgetBean gadget, Map<String, String> gwtParams)
      throws ClientException;

  void removeGadget(GadgetBean gadget, Map<String, String> gwtParams)
      throws ClientException;

  void savePreferences(GadgetBean gadget, Map<String, String> updatePrefs,
      Map<String, String> gwtParams) throws Exception;

  void saveCollapsed(GadgetBean gadget, Map<String, String> gwtParams)
      throws ClientException;

}
