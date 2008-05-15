/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.interfaces.ejb;

import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Server-related operations. The clients of the platform will be able to query
 * the existing repository locations {@link RepositoryLocation} and connect to
 * one. Name insight: Enterprise Component Server.
 * <p>
 * In the future, will also contain security information as to who has the
 * rights to change the locations.
 * <p>
 * The locations and the security information will be persisted/retrieved to a
 * storage. For the moment they are hardcoded.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
public interface ECServer {

    /**
     * Removes the instance from the container once the client has no more
     * business with it. It will also disconnect the session.
     *
     * @throws ClientException
     */
    void remove() throws ClientException;

    /**
     * Returns the available repository locations.
     */
    List<RepositoryLocation> getAvailableRepositoryLocations();

    /**
     * Returns the list of authorized principals that can make modifications to
     * the storage associated with the server.
     */
    List<Principal> getAuthorizedPrincipals();

    /**
     * Returns the default repository location.
     *
     * @return the default RepositoryLocation instance.
     */
    RepositoryLocation getDefaultRepositoryLocation();

    /**
     * @param repName
     * @return a repository with the given name or <code>null</code> if
     *  there's no repository available for that name
     */
    RepositoryLocation getRepositoryLocationForName(String repName);

}
