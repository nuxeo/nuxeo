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

package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.security.Principal;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * This should not refer other beans that may require heavy initialization. This
 * component is initialized and used earlier than some resources become
 * available.
 *
 * @author DM
 */
@Name("userServicesContext")
@Scope(SESSION)
@Startup
public class UserServicesContext implements Serializable {

    private static final long serialVersionUID = -4938620211123775744L;

    @In(create = true)
    private transient NavigationContext navigationContext;

    @In(required = false, create = true)
    private transient Principal currentUser;

    private transient RepositoryLocation repoLocation;

    private transient Boolean serverLocationRetrieved = false;

    private boolean isServerLocationSelected() {
        if (!serverLocationRetrieved) {
            repoLocation = navigationContext.getCurrentServerLocation();
            serverLocationRetrieved = true;
        }

        return repoLocation != null;
    }

    @Observer(value={EventNames.LOCATION_SELECTION_CHANGED}, create=false)
    @BypassInterceptors
    public void invalidate() {
        repoLocation = null;
        serverLocationRetrieved = false;
    }

    public Boolean getSearchEnabled() {
        return isServerLocationSelected();
    }

    public Boolean getDashboardEnabled() {
        return isServerLocationSelected();
    }

    public Boolean getUserManagerEnabled() {
        return isServerLocationSelected();
    }

    /**
     * Checks if an user has the right to see the vocabularies
     * management link.
     *
     * @return - true if the user has this right<br>
     *         - false otherwise
     */
    public boolean getVocabulariesEnabled() {
        if (currentUser == null) {
            return false;
        } else {
            return ((NuxeoPrincipal) currentUser).isAdministrator();
        }
    }

}
