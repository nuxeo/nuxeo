package org.nuxeo.opensocial.container.client.service.api;

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.ContainerServiceException;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("gwtcontainer")
public interface ContainerService extends RemoteService {

  /**
   * Retrieve a specific container
   *
   * @param gwtParams
   * @return Container
   * @throws ContainerServiceException
   */
  Container getContainer(Map<String, String> gwtParams)
      throws ContainerServiceException;

  /**
   * Save layout of container
   *
   * @param gwtParams
   * @param layoutName
   * @return
   * @throws ContainerServiceException
   */
  Container saveLayout(Map<String, String> gwtParams,
      String layout) throws ContainerServiceException;

  /**
   * Save preferences of gadget with form parameter
   *
   * @param gadget
   * @param form
   *          : new preferences
   * @param gwtParams
   * @return GadgetBean
   * @throws ContainerServiceException
   */
  GadgetBean saveGadgetPreferences(GadgetBean gadget, String form,
      Map<String, String> gwtParams) throws ContainerServiceException;

  /**
   * Remove gadget
   *
   * @param gadget
   * @param gwtParams
   * @return GadgetBean removed
   * @throws ContainerServiceException
   */
  GadgetBean removeGadget(GadgetBean gadget, Map<String, String> gwtParams)
      throws ContainerServiceException;

  /**
   * Add gadget
   *
   * @param gadgetName
   * @param gwtParams
   * @return GadgetBean added
   * @throws ContainerServiceException
   */
  GadgetBean addGadget(String gadgetName, Map<String, String> gwtParams)
      throws ContainerServiceException;

  /**
   * Save gadget position
   *
   * @param beans
   * @param gwtParams
   * @throws ContainerServiceException
   */
  void saveGadgetPosition(ArrayList<GadgetBean> beans,
      Map<String, String> gwtParams) throws ContainerServiceException;

  /**
   * Save collapsed
   *
   * @param gadget
   * @param gwtParams
   * @return Gadget bean saved
   * @throws ContainerServiceException
   */
  GadgetBean saveGadgetCollapsed(GadgetBean gadget,
      Map<String, String> gwtParams) throws ContainerServiceException;

  /**
   * Get collection of gadget name sorted by category
   *
   * @param gwtParams
   * @return key is category - value is list of gadget name
   * @throws ContainerServiceException
   */
  Map<String, ArrayList<String>> getGadgetList(Map<String, String> gwtParams)
      throws ContainerServiceException;

}
