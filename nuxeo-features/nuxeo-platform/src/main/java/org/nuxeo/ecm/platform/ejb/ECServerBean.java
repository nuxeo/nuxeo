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

package org.nuxeo.ecm.platform.ejb;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.interfaces.ejb.ECServer;
import org.nuxeo.ecm.platform.interfaces.local.ECServerLocal;
import org.nuxeo.ecm.platform.util.LocationManagerService;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;

/**
 * implementation class.
 *
 * @author Razvan Caraghin
 *
 */
@Deprecated
@Stateless
public class ECServerBean implements ECServer, ECServerLocal {

    public static final String DEFAULT_REPOSITORY_LOCATION_KEY = "default";

    protected LocationManagerService getLocationManagerService() {
        return (LocationManagerService) Framework.getRuntime().getComponent(
                LocationManagerService.NAME);
    }

    public List<RepositoryLocation> getAvailableRepositoryLocations() {
        List<RepositoryLocation> returningLocations = new ArrayList<RepositoryLocation>();
        try {
            LocationManagerService lms = getLocationManagerService();
            returningLocations.addAll(lms.getAvailableLocations().values());
        } catch (Throwable t) {
            EJBExceptionHandler.wrapException(t);
        }
        return returningLocations;
    }

    public List<Principal> getAuthorizedPrincipals() {
        List<Principal> principals = new ArrayList<Principal>();
        try {
            principals.add(new UserPrincipal("q"));
            principals.add(new UserPrincipal("useradmin"));
        } catch (Throwable t) {
            EJBExceptionHandler.wrapException(t);
        }
        return principals;
    }

    public RepositoryLocation getDefaultRepositoryLocation() {
        RepositoryLocation repositoryLocation = null;
        for (RepositoryLocation each : getAvailableRepositoryLocations()) {
            if (each.getName().equals(DEFAULT_REPOSITORY_LOCATION_KEY)) {
                repositoryLocation = each;
                break;
            }
        }
        return repositoryLocation;
    }

    public RepositoryLocation getRepositoryLocationForName(@NotNull String repName) {
        final List<RepositoryLocation> repLocations = getAvailableRepositoryLocations();
        for (RepositoryLocation location : repLocations) {
            if (repName.equals(location.getName())) {
                return location;
            }
        }
        return null;
    }

    public void remove() throws ClientException {
    }

}
