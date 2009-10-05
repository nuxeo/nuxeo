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

import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Univers ( Nuxeo-spaces-api concept ) web engine object
 **/
@WebObject(type = "Univers")
@Produces("text/html; charset=UTF-8")
public class UniversWebObject extends DocumentObject {

  /**
   * Logger log4j
   */
  private static final Log LOGGER = LogFactory.getLog(UniversWebObject.class);

  /**
   * Current universe
   */
  private Univers univers = null;

  /**
   * all spaces in the current universe
   */
  private List<Space> spaces;

  public List<Space> getSpaces() {
    return spaces;
  }

  public Univers getUnivers() {
    return univers;
  }

  @Override
  public void initialize(Object... args) {
    assert args != null && args.length == 1;
    this.univers = (Univers) args[0];
    try {
      DocumentModel universDocumentModel = getSession().getDocument(
          new IdRef(univers.getId()));
      super.initialize(universDocumentModel);
    } catch (ClientException e) {
      throw new RuntimeException(e);
    }
    buildSpaces();
    LOGGER.info("initialize OK");

  }

  private void buildSpaces() {
    try {
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      this.spaces = spaceManager.getSpacesForUnivers(univers, getSession());

    } catch (Exception e) {
      LOGGER.error("Unable to retrieve spaces for this univers :"+e.getMessage(),e);

      throw ExceptionManager.wrap(e);
    }
    LOGGER.info("buildSpaces OK");
  }

  /**
   * Read a space with spaces API
   *
   * @param spacename
   * @return
   */
  @Path("{spacename}")
  public Object doGetSpace(@PathParam("spacename") String spacename) {
    try {

      CoreSession coreSession = getSession();
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      Space mySpace = spaceManager.getSpace(spacename, univers, coreSession);

      return newObject("Space", mySpace, univers);

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

  /**
   * Remove univers via spaces API
   */
  @Override
  public Response doDelete() {
    try {
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      spaceManager.deleteUnivers(univers, getSession());

      LOGGER.info("Univers has been successfully deleted");
      return redirect(ctx.getModulePath());

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

  private CoreSession getSession() {
    return WebEngine.getActiveContext()
        .getCoreSession();
  }

  /**
   * Update the univers with spaces API
   */
  @Override
  public Response doPut() {
    try {

      Univers newUnivers = Mapper.createUnivers(ctx.getForm(), univers.getId());
      CoreSession session = getSession();
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      spaceManager.updateUnivers(newUnivers, session);

      LOGGER.info("Univers has been successfully updated");
      return redirect(ctx.getModulePath());
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

  /**
   * Create a new space in this univers with spaces API
   *
   * @return
   */
  @POST
  @Path("@createSpace")
  public Response createSpace() {
    try {
      Space space = Mapper.createSpace(ctx.getForm(), null);
      CoreSession coreSession = getSession();
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      space = spaceManager.createSpace(space, univers, coreSession);

      return redirect(getPath() + "/" + space.getName());
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }
}