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

package org.nuxeo.opensocial.container.client.service.api;

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.ContainerServiceException;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ContainerServiceAsync {

  /**
   * Retrieve a specific container
   *
   * @param gwtParams
   * @param AsyncCallback
   *          <Container>
   * @throws ContainerServiceException
   */
  void getContainer(Map<String, String> gwtParams,
      AsyncCallback<Container> callback);

  /**
   * Save layout of container
   *
   * @param gwtParams
   * @param layoutName
   * @param AsyncCallback
   *          <Container>
   */
  void saveLayout(Map<String, String> gwtParams,
      String layout, AsyncCallback<Container> callback);

  /**
   * Save preferences of gadget with form parameter
   *
   * @param gadget
   * @param form
   *          : new preferences
   * @param gwtParams
   * @param AsyncCallback
   *          <GadgetBean>
   */
  void saveGadgetPreferences(GadgetBean gadget, String form,
      Map<String, String> gwtParams, AsyncCallback<GadgetBean> callback);

  /**
   * Remove gadget
   *
   * @param gadget
   * @param gwtParams
   * @param AsyncCallback
   *          <GadgetBean>
   */
  void removeGadget(GadgetBean gadget, Map<String, String> gwtParams,
      AsyncCallback<GadgetBean> callback);

  /**
   * Add gadget
   *
   * @param gadgetName
   * @param gwtParams
   * @param AsyncCallback
   *          <GadgetBean>
   */
  void addGadget(String gadgetName, Map<String, String> gwtParams,
      AsyncCallback<GadgetBean> callback);

  /**
   * Save gadget position
   *
   * @param beans
   * @param gwtParams
   * @param AsyncCallback
   *          <GadgetBean>
   */
  void saveGadgetPosition(ArrayList<GadgetBean> beans,
      Map<String, String> gwtParams, AsyncCallback<GadgetBean> callback);

  /**
   * Save collapsed
   *
   * @param gadget
   * @param gwtParams
   * @param AsyncCallback
   *          <GadgetBean>
   */
  void saveGadgetCollapsed(GadgetBean gadgetBean,
      Map<String, String> gwtParams, AsyncCallback<GadgetBean> callback);

  /**
   * Get collection of gadget name sorted by category
   *
   * @param gwtParams
   * @param AsyncCallback
   *          <Map<String, ArrayList<String>>>
   */
  void getGadgetList(Map<String, String> gwtParams,
      AsyncCallback<Map<String, ArrayList<String>>> asyncCallback);
}
