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

package org.nuxeo.opensocial.container.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.ContainerServiceException;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.service.api.ContainerService;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.runtime.api.Framework;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ContainerServiceImpl extends RemoteServiceServlet implements
    ContainerService {

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(ContainerServiceImpl.class);

  /**
   * Retrieve a specific container
   *
   * @param gwtParams
   * @return Container
   * @throws ContainerServiceException
   */
  public Container getContainer(Map<String, String> gwtParams)
      throws ContainerServiceException {
    try {
      return Framework.getService(FactoryManager.class)
          .getContainerFactory()
          .createContainer(gwtParams);
    } catch (Exception e) {
      log.error("Get container error " + e, e);
      throw new ContainerServiceException(e.getMessage(), e);
    }
  }

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
  public GadgetBean saveGadgetPreferences(GadgetBean gadget, String form,
      Map<String, String> gwtParams) throws ContainerServiceException {
    try {

      Map<String, String> updatePrefs = getParameters(form);
      Framework.getService(FactoryManager.class)
          .getGadgetFactory()
          .savePreferences(gadget, updatePrefs, gwtParams);
    } catch (Exception e) {
      log.error("saveGadgetPreferences " + e, e);
      throw new ContainerServiceException(e.getMessage(), e);
    }
    return gadget;
  }

  /**
   * Save gadget position
   *
   * @param beans
   * @param gwtParams
   * @throws ContainerServiceException
   */
  public void saveGadgetPosition(ArrayList<GadgetBean> beans,
      Map<String, String> gwtParams) throws ContainerServiceException {
    try {
      GadgetManager factory = Framework.getService(FactoryManager.class)
          .getGadgetFactory();
      for (GadgetBean gadget : beans) {
        factory.savePosition(gadget, gwtParams);
      }
    } catch (ClientException e) {
      log.error("savePosition error " + e, e);
      throw new ContainerServiceException(e.getMessage(), e);
    } catch (Exception e) {
      log.error(e);
    }
  }

  /**
   * Remove gadget
   *
   * @param gadget
   * @param gwtParams
   * @return GadgetBean removed
   * @throws ContainerServiceException
   */
  public GadgetBean removeGadget(GadgetBean gadget,
      Map<String, String> gwtParams) throws ContainerServiceException {
    try {
      Framework.getService(FactoryManager.class)
          .getGadgetFactory()
          .removeGadget(gadget, gwtParams);
    } catch (ClientException e) {
      log.error("removeGadget error : " + e, e);
      throw new ContainerServiceException(e.getMessage(), e);
    } catch (Exception e) {
      log.error(e);
    }
    return gadget;
  }

  /**
   * Save collapsed
   *
   * @param gadget
   * @param gwtParams
   * @return Gadget bean saved
   * @throws ContainerServiceException
   */
  public GadgetBean saveGadgetCollapsed(GadgetBean gadget,
      Map<String, String> gwtParams) throws ContainerServiceException {
    try {
      Framework.getService(FactoryManager.class)
          .getGadgetFactory()
          .saveCollapsed(gadget, gwtParams);
    } catch (ClientException e) {
      log.error(e.getMessage(), e);
      throw new ContainerServiceException(e.getMessage(), e);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return gadget;
  }

  /**
   * Get collection of gadget name sorted by category
   *
   * @param gwtParams
   * @return key is category - value is list of gadget name
   * @throws ContainerServiceException
   */
  public Map<String, ArrayList<String>> getGadgetList(
      Map<String, String> gwtParams) throws ContainerServiceException {
    try {
      return Framework.getService(FactoryManager.class)
          .getContainerFactory()
          .getGadgetList();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new ContainerServiceException(e.getMessage(), e);
    }
  }

  /**
   * Add gadget
   *
   * @param gadgetName
   * @param gwtParams
   * @return GadgetBean added
   * @throws ContainerServiceException
   */
  public GadgetBean addGadget(String gadgetName, Map<String, String> gwtParams)
      throws ContainerServiceException {
    try {
      return Framework.getService(FactoryManager.class)
          .getContainerFactory()
          .addGadget(gadgetName, gwtParams);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new ContainerServiceException(e.getMessage(), e);
    }
  }

  /**
   * Utility methods
   */

  public static String decode(String string) {
      try {
          string = new URI('?' + string).getQuery();
      } catch (Exception e) {
          // ignore, use unescaped stuff
      }
      return string;
  }

  /**
   * Get a map of preferences
   *
   * @param form
   *          : html form result (&name=result&...)
   * @return Map of parameters key : name, value : result
   */
  private Map<String, String> getParameters(String form) {
    if (form == null)
      return null;
    StringTokenizer params = new StringTokenizer(form, "&");
    Map<String, String> map = new HashMap<String, String>();
    while (params.hasMoreTokens()) {
      StringTokenizer st = new StringTokenizer(params.nextToken(), "=");
      String key = "", value = "";
      if (st.hasMoreTokens())
        key = decode(st.nextToken());
      while (st.hasMoreTokens()) {
        value += decode(st.nextToken());
      }
      map.put(key, value);
    }
    return map;
  }

  /**
   * Save layout of container
   *
   * @param gwtParams
   * @param layoutName
   * @return
   * @throws ContainerServiceException
   */
  public Container saveLayout(Map<String, String> gwtParams, String layout)
      throws ContainerServiceException {
    try {
      return Framework.getService(FactoryManager.class)
          .getContainerFactory()
          .saveLayout(gwtParams, layout);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new ContainerServiceException(e.getMessage(), e);
    }
  }
}
