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

import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Space ( Nuxeo-spaces-api concept ) web engine object
 **/
@WebObject(type = "Space")
@Produces("text/html; charset=UTF-8")
public class SpaceWebObject extends DocumentObject {

  /**
   * Logger log4j
   */
  private static final Log LOGGER = LogFactory.getLog(SpaceWebObject.class);

  /**
   * Current space
   */
  private Space space = null;

  /**
   * Parent univers
   */
  private Univers univers = null;

  /**
   * all spaces in the parent univers , including current space itself.
   */
  private List<Space> spaces;

  /**
   * All gadgets of the current space
   */
  private List<Gadget> gadgets;

  public List<Space> getSpaces() {
    return spaces;
  }

  public List<Gadget> getGadgets() {
    return gadgets;
  }

  public Space getSpace() {
    return space;
  }

  public Univers getUnivers() {
    return univers;
  }

  /**
   * Calculates space data objects
   */
  @Override
  public void initialize(Object... args) {
    assert args != null && args.length == 2;
    try {
      this.space = (Space) args[0];

      try {
        DocumentModel spaceDocumentModel = getSession().getDocument(
            new IdRef(this.space.getId()));
        super.initialize(spaceDocumentModel);
      } catch (ClientException e) {
        throw new RuntimeException(e);
      }

      if (this.space == null)
        throw new Exception("Space argument can't be null");

      // JIRA WEB-279 => now use RequestAttribute
      if (space.getTheme() != null) {
        getContext().getRequest()
            .setAttribute("org.nuxeo.theme.theme",
                space.getTheme() + "/default");
        LOGGER.debug("setting theme from space in context request wall again "
            + space.getTheme());
      } else {
        LOGGER.debug("no theme found from space ");
      }

      this.univers = (Univers) args[1];
      buildSpacesAndGadgets();
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
    LOGGER.debug("Space has been set");
  }

  private CoreSession getSession() {
    return WebEngine.getActiveContext()
        .getCoreSession();
  }

  private void buildSpacesAndGadgets() throws Exception

  {
    CoreSession coreSession = getSession();
    LOGGER.debug("session id =" + coreSession.getSessionId());
    SpaceManager spaceManager = Framework.getService(SpaceManager.class);

    this.spaces = spaceManager.getSpacesForUnivers(univers, coreSession);
    this.gadgets = spaceManager.getGadgetsForSpace(space, coreSession);
  }

  @Path("{name}")
  public Object doGetGadget(@PathParam("name") String name) {
    try {
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      Gadget gadget = spaceManager.getGadget(name, space, getSession());

      return newObject("Gadget", gadget, space, univers);
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

  /**
   * Delete a space via spaces API
   */
  @Override
  public Response doDelete() {
    try {

      CoreSession session = getSession();
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);

      spaceManager.deleteSpace(space, session);

      LOGGER.info("Space" + space.getName() + " has been successfully deleted");
      return redirect(ctx.getModulePath() + "/" + univers.getName());

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

  /**
   * Pour simplifier le process de mise a jour on utilise le doc directement ici
   * 
   * @return
   */
  @POST
  @Path("@updateDoc")
  public Response updateDoc() {
    return super.doPut();
  }

  @POST
  @Path("@updateSpace")
  public Response updateTheme() {
    super.doPut();
    return redirect(getPath() + "#openManager");
  }

  @Override
  public Response doPut() {
    try {

      Space newSpace = Mapper.createSpace(ctx.getForm(), space.getId());

      this.space = Framework.getService(SpaceManager.class)
          .updateSpace(newSpace, getSession());

      return redirect(getPath());

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }
  }

  /**
   * Space creation in the current univers
   * 
   * @return
   */
  @POST
  @Path("@createSpace")
  public Response createSpace() {
    try {
      Space newSpace = Mapper.createSpace(ctx.getForm(), null);
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);
      Space createSpace = spaceManager.createSpace(newSpace, univers,
          getSession());
      return redirect(ctx.getModulePath() + "/" + univers.getName() + "/"
          + createSpace.getName());

    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }

  }

  /**
   * Space creation in the current univers
   * 
   * @return
   */
  @POST
  @Path("@createVersion")
  public Response createVersion() {
    try {
      Calendar d = getDatePublication(ctx.getForm());
      if (d.compareTo(Calendar.getInstance()) == 1) {

        CoreSession session = this.ctx.getCoreSession();
        DocumentModel createDoc = session.copy(this.doc.getRef(),
            this.doc.getParentRef(), this.doc.getName());
        createDoc.setPropertyValue(Constants.Document.PUBLICATION_DATE, d);
        session.saveDocument(createDoc);
        session.save();
        return Response.ok()
            .entity(
                ctx.getModulePath() + "/" + univers.getName() + "/"
                    + createDoc.getName())
            .build();
      } else {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
            .build();
      }
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }

  }

  /**
   * update space
   * 
   * @return
   */
  @POST
  @Path("@save")
  public Response save() {
    try {
      Calendar d = getDatePublication(ctx.getForm());
      if (d.compareTo(Calendar.getInstance()) == 1) {
        updatePubicationDate(getDatePublication(ctx.getForm()));
        return Response.ok()
            .entity("OK")
            .build();
      } else {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
            .build();
      }
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }

  }

  public Calendar getDatePublication(FormData formData) {

    StringTokenizer st = new StringTokenizer(formData.getString("dc:valid"),
        "/");
    Calendar date = Calendar.getInstance();
    date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(st.nextToken()));
    date.set(Calendar.MONTH, Integer.parseInt(st.nextToken()) - 1);
    date.set(Calendar.YEAR, Integer.parseInt(st.nextToken()));
    date.set(Calendar.HOUR_OF_DAY,
        Integer.parseInt(formData.getString("hours")));
    date.set(Calendar.MINUTE, Integer.parseInt(formData.getString("minutes")));
    return date;
  }

  /**
   * update space
   * 
   * @return
   */
  @POST
  @Path("@publish")
  public Response publishNow() {
    try {
      updatePubicationDate(Calendar.getInstance());
      return redirect(ctx.getModulePath() + "/" + univers.getName() + "/"
          + ctx.getForm()
              .getString("actualVersionName"));
    } catch (ClientException e) {
      throw ExceptionManager.wrap(e);
    }
  }

  public void updatePubicationDate(Calendar cal) throws ClientException {
    try {
      DocumentModel doc = getDocument();
      doc.setPropertyValue("dc:valid", cal);
      CoreSession session = getSession();
      session.saveDocument(doc);
      session.save();
    } catch (PropertyException e) {
      throw ExceptionManager.wrap(e);
    }
  }

  /**
   * Space creation in the current univers
   * 
   * @return
   */
  @POST
  @Path("@removeSpace")
  public Response removeSpace() {
    super.doDelete();
    return redirect(ctx.getModulePath() + "/" + univers.getName());
  }

  /**
   * Gadget creation in the current space
   * 
   * @return
   */
  @POST
  @Path("@createGadget")
  public Response createGadget() {
    try {
      Gadget gadget = Mapper.createGadget(ctx.getForm(), null);
      SpaceManager spaceManager = Framework.getService(SpaceManager.class);
      Gadget createdGadget = spaceManager.createGadget(gadget, space,
          getSession());
      if (createdGadget != null) {
        return redirect(getPath() + "/" + gadget.getName());
      } else {
        throw new Exception("Problem while creating Gadget");
      }
    } catch (Exception e) {
      throw ExceptionManager.wrap(e);
    }

  }

}
