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

package org.nuxeo.opensocial.spaces.webobject;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Gadget ( Nuxeo-spaces-api concept ) web engine object
 **/
@WebObject(type = "Gadget")
@Produces("text/html; charset=UTF-8")
public class GadgetWebObject extends DocumentObject {

  /**
   * Logger log4j
   */
  private static final Log LOGGER = LogFactory.getLog(GadgetWebObject.class);

  /**
   * Parent space of the gadget
   */
  private Space space = null;

  /**
   * Gadget data object
   */
  private Gadget gadget = null;

  /**
   * Grand-parent of the gadget or parent of space
   */
  private Univers univers = null;

  public Univers getUnivers() {
    return univers;
  }

  public Space getSpace() {
    return space;
  }

  public Gadget getGadget() {
    return gadget;
  }

  @Override
  public void initialize(Object... args) {
    assert args != null && args.length == 3;
    LOGGER.info("Gadget has been set");
    this.gadget = (Gadget) args[0];
    try {
      super.initialize(getSession()
          .getDocument(new IdRef(this.gadget.getId())));
    } catch (ClientException e) {
      throw new RuntimeException(e);
    }
    space = (Space) args[1];
    univers = (Univers) args[2];
  }


  /**
   * Delete a space
   */
  @Override
  public Response doDelete() {
    try {

      CoreSession coreSession = getSession();
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      spaceManager.deleteGadget(gadget, coreSession);

      LOGGER.info("Gadget has been successfully deleted.");
      return redirect(getPrevious().getPath());

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }

  }

  private CoreSession getSession() {
    return WebEngine.getActiveContext()
        .getCoreSession();
  }

  @Override
  public Response doPut() {
    try {

      Gadget newGadget = Mapper.createGadget(ctx.getForm(), gadget.getId());
      CoreSession session = getSession();
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      spaceManager.updateGadget(newGadget, session);

      LOGGER.info("Space has been successfully updated");
      return redirect(ctx.getModulePath());

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

}
