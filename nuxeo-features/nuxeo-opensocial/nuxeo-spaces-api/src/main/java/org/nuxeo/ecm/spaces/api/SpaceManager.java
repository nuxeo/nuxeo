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

package org.nuxeo.ecm.spaces.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.exceptions.GadgetNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;

/**
 * Framework service for CRUD operations concerning Univers, Space in a
 * specific univers, and Gadget in a specific space
 *
 * @author 10044893
 *
 */
public interface SpaceManager {

  /**
   * List of all accesible universes
   * @param sessionId  sesssion id
   * @return the list of all accessible universes
   * @throws SpaceException a bug has happened
   */
   List<Univers> getUniversList(CoreSession coreSession) throws SpaceException;

  /**
   * Retrieve a specific universe  from its name
   * @param name identifier of a univers
   * @param sessionId sesssion id
   * @return a specific universe
   * @throws UniversNotFoundException when no universe with such a name can be found
   * @throws SpaceException when a bug has happened
   */
   Univers getUnivers(String name, CoreSession coreSession)
    throws UniversNotFoundException,SpaceException;

  /**
   *
   * @param universId
   * @param sessionId
   * @return
   * @throws UniversNotFoundException
   * @throws SpaceException
   */
   Univers getUniversFromId(String universId, CoreSession coreSession)
    throws SpaceException;

  /**
   * Create a new univers
   * @param universe universe data object
   * @param sessionId session id
   * @return true if update has been successfull , else a SpaceException should have happened
   * @throws SpaceException
   */
   Univers createUnivers(Univers univers, CoreSession coreSession)
      throws SpaceException;

  /**
   * Update of an universe
   * @param newUnivers new universe data object
   * @param sessionId session id
   * @return true if update operation has been successfull , else a SpaceException should have happened
   * @throws SpaceException
   */
   Univers updateUnivers(Univers newUnivers,
      CoreSession coreSession) throws SpaceException;

  /**
   * Update of a space
   * @param newSpace new space data object
   * @param sessionId session id
   * @return true if update operation has been successfull , else a SpaceException should have happened
   * @throws SpaceNotFoundException when no space was found with the given space id
   */
   Space updateSpace( Space newSpace, CoreSession coreSession)
      throws SpaceException;

  /**
   * Update of a gadget
   * @param oldGadget old gadget data object
   * @param sessionId session id
   * @return true if update operation has been successfull , else a SpaceException should have happened
   * @throws SpaceException when a bug has happened
   * @throws GadgetNotFoundException when no gadget was found with the given gadget id
  */
   Gadget updateGadget(Gadget newGadget,
      CoreSession coreSession) throws SpaceException;

  /**
   * Delete operation on a univers
   * @param univers the univers to be deleted
   * @param sessionId session id
   * @return true if update operation has been successfull , else a SpaceException should have happened
   * @throws SpaceException true if update operation has been successfull , else a SpaceException should have happened
   */
   void deleteUnivers(Univers univers, CoreSession coreSession)
      throws SpaceException;

  /**
   * List of all accesible spaces for a given univers
   * @param univers the univers in which you are looking for spaces
   * @param sessionId the session id
   * @return all accessible univers
   * @throws UniversNotFoundException when no univers was found with the given universe id
   * @throws SpaceException when a bug has happened
   */
   List<Space> getSpacesForUnivers(Univers universe, CoreSession coreSession)
      throws UniversNotFoundException,SpaceException;

  /**
   * Retrieve a specific space  from its name and its parent universe
   * @param name name of the searched space
   * @param univers parent container
   * @param sessionId the session id
   * @return the space if found , else a spaceexception is thrown
   * @throws SpaceNotFoundException when the space was not found
   */
   Space getSpace(String name, Univers univers, CoreSession coreSession)
      throws SpaceException;

  /**
   *
   * @param spaceId
   * @param sessionId
   * @return
   * @throws SpaceNotFoundException
   * @throws SpaceException
   */
   Space getSpaceFromId(String spaceId, CoreSession coreSession)	throws SpaceNotFoundException,SpaceException;



  /**
   * Retrieve a specific gadget from its name and its parent space
   * @param name name of the searched gadget
   * @param space parent container
   * @param sessionId the session id
   * @return the gadget if found , else a spaceexception is thrown
   * @throws GadgetNotFoundException when the gadget was not found
   */
   Gadget getGadget(String name, Space space, CoreSession coreSession)throws SpaceException;

  /**
   *
   * @param gadgetId
   * @param sessionId
   * @return
   * @throws GadgetNotFoundException
   * @throws SpaceException
   */
   Gadget getGadgetFromId(String gadgetId, CoreSession coreSession)throws SpaceException;

  /**
   * Create a new space
   * @param universe the univers in which you want to create the new space
   * @param sessionId session id
   * @return true if creation has been successfull , else a SpaceException should have happened
   * @throws SpaceException a bug while creating the new space has happened
   */
   Space createSpace(Space space, Univers univers, CoreSession coreSession)
      throws SpaceException;

  /**
   * Delete operation on a space
   * @param space the univers to be deleted
   * @param univers the parent univers
   * @param sessionId session id
   * @return true if delete operation has been successfull , else a SpaceException should have happened
   * @throws SpaceException true if delete operation has been successfull , else a SpaceException should have happened
   */
   void deleteSpace(Space space,CoreSession coreSession)
      throws SpaceException;

  /**
   * List of all accessible gadgets
   * @return the list of all accessible universes
   * @param space space parent
   * @param sessionId the sesssion id
   * @return the list of all accessible gadgets
   * @throws SpaceException a bug has happened
   */
   List<Gadget> getGadgetsForSpace(Space space, CoreSession coreSession)
      throws SpaceException;

  /**
   * Creation of a new gadget
   * @param gadget the gadget to be created
   * @param space the parent space
   * @param sessionId the session id
   * @return Gadget instance if creation has been successfull , else a SpaceException should have happened
   * @throws SpaceException a bug has happened
   */
   Gadget createGadget(Gadget gadget, Space space, CoreSession coreSession)
      throws SpaceException;

  /**
   * Delete operation on a gadget
   * @param gadget the gadget to be deleted
   * @param space the parent space
   * @param sessionId the session id
   * @return true if the gadget has been successfully deleted , else a SpaceException  should have happened
   * @throws SpaceException a bug has happened while deleting the gadget
   */
   void deleteGadget(Gadget gadget, CoreSession coreSession)
      throws SpaceException;

}
