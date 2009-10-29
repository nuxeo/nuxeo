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

package org.nuxeo.opensocial.container.factory;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.opensocial.container.factory.mapping.GadgetMapper;
import org.nuxeo.opensocial.container.factory.utils.CoreSessionHelper;
import org.nuxeo.runtime.api.Framework;

public class GadgetManagerImpl implements GadgetManager {

  private static final Log log = LogFactory.getLog(GadgetManagerImpl.class);
  public static final String TITLE_KEY_PREF = "title";

  /**
   * Remove gadget to container
   *
   * @param gadget
   *          : Gadget to delete
   * @param gwtParams
   *          : container paramters
   */
  public void removeGadget(GadgetBean gadget, Map<String, String> gwtParams)
      throws ClientException {
    try {
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);
      CoreSession coreSession = getCoreSession(gwtParams);
      spaceManager.deleteGadget(new GadgetMapper(gadget), coreSession);
    } catch (Exception e) {
      log.error(e);
      throw new ClientException(e);
    }

  }

  protected CoreSession getCoreSession(Map<String, String> gwtParams)
      throws Exception {
    return CoreSessionHelper.getCoreSession(gwtParams.get(ContainerManagerImpl.REPO_NAME));
  }

  /**
   * Save Collapse
   *
   * @param gadget
   *          : Gadget to save
   * @param gwtParams
   *          : container paramters
   */
  public void saveCollapsed(GadgetBean gadget, Map<String, String> gwtParams) {
    updateFullGadget(gadget, gwtParams);
  }

  private void updateFullGadget(GadgetBean gadget, Map<String, String> gwtParams) {
    try {
      CoreSession coreSession = getCoreSession(gwtParams);
      GadgetMapper gadgetMapper = new GadgetMapper(gadget);
      Framework.getService(SpaceManager.class)
          .updateGadget(gadgetMapper, coreSession);
    } catch (Exception e) {
      log.error(e);
    }
  }

  public void savePosition(GadgetBean gadget, Map<String, String> gwtParams)
      throws ClientException {
    updateFullGadget(gadget, gwtParams);
  }

  /**
   * Save gadget preferences and update render url of gadget
   *
   */
  public void savePreferences(GadgetBean gadget,
      Map<String, String> updatePrefs, Map<String, String> gwtParams)
      throws Exception {
    try {
      GadgetMapper gadgetMapper = new GadgetMapper(gadget);
      if (updatePrefs != null) {
        gadgetMapper.setPreferences(updatePrefs);
        if (updatePrefs.containsKey(TITLE_KEY_PREF)) {
          //TODO: encode title
          gadgetMapper.setTitle(updatePrefs.get(TITLE_KEY_PREF));
        }
      }
      gadgetMapper.setName(gadget.getSpaceName());
      Framework.getService(SpaceManager.class)
          .updateGadget(gadgetMapper, getCoreSession(gwtParams));

    } catch (Exception e) {
      log.error("GadgetManagerUImlp - savePreferences : "
          + e.fillInStackTrace());
    }

  }
}
