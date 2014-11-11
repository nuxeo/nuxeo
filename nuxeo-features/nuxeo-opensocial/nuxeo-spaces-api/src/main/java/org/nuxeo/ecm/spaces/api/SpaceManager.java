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
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;

/**
 * Framework service for CRUD operations concerning Univers, Space in a specific
 * univers, and Gadget in a specific space
 *
 * @author 10044893
 *
 */
public interface SpaceManager {

    /**
     * List of all accesible universes
     *
     * @param sessionId sesssion id
     * @return the list of all accessible universes
     * @throws SpaceException a bug has happened
     */
    List<Univers> getUniversList(CoreSession session) throws SpaceException;

    /**
     * Retrieve a specific universe from its name
     *
     * @param name identifier of a univers
     * @param sessionId sesssion id
     * @return a specific universe
     * @throws UniversNotFoundException when no universe with such a name can be
     *             found
     * @throws SpaceException when a bug has happened
     */
    Univers getUnivers(String name, CoreSession session)
            throws UniversNotFoundException, SpaceException;

    /**
     *
     * @param universId
     * @return
     * @throws UniversNotFoundException
     * @throws SpaceException
     */
    Univers getUniversFromId(String universId, CoreSession session)
            throws SpaceException;

    List<SpaceProvider> getSpacesProvider(Univers univers);

    /**
     * List of all accesible spaces for a given univers
     *
     * @param univers the univers in which you are looking for spaces
     * @return all accessible univers
     * @throws UniversNotFoundException when no univers was found with the given
     *             universe id
     * @throws SpaceException when a bug has happened
     */
    List<Space> getSpacesForUnivers(Univers universe, CoreSession session)
            throws UniversNotFoundException, SpaceException;

    /**
     * Retrieve a specific space from its name and its parent universe
     *
     * @param name name of the searched space
     * @param univers parent container
     * @return the space if found , else a spaceexception is thrown
     * @throws SpaceNotFoundException when the space was not found
     */
    Space getSpace(String name, Univers univers, CoreSession session)
            throws SpaceException;

    Space getSpace(String name, CoreSession session) throws SpaceException;

    @Deprecated
    Space getSpace(String name, SpaceProvider provider, CoreSession session) throws SpaceException;

    Space getSpaceFromId(String spaceId, CoreSession session)
            throws SpaceException;

    List<SpaceProvider> getSpacesProviders();

    String getProviderName(SpaceProvider provider);

}
