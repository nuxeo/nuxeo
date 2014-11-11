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
 * $Id$
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Defines actions that can be invoked from pages or other actions - relative to
 * server.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface ServerActions extends StatefulBaseLifeCycle {

    /**
     * Returns the list of available {@link RepositoryLocation}s the user can
     * connect to.
     */
    List<Repository> getAvailableRepositories() throws ClientException;

    /**
     * Stores the selected location.
     *
     * @return the page that displays the domains ( found at the selected
     *         {@link RepositoryLocation}.
     */
    String selectRepository(String repositoryName) throws ClientException;

    /**
     * Select and Stores the first location.
     *
     * @return the RepositoryLocation.
     * @throws ClientException
     */
    //RepositoryLocation selectFirstLocation() throws ClientException;

    /**
     * Removes the reference.
     */
    @Destroy
    @Remove
    @PermitAll
    void destroy();

}
