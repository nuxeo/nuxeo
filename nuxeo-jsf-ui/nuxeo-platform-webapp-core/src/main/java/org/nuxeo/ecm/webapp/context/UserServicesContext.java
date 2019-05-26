/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

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
 * This should not refer other beans that may require heavy initialization. This component is initialized and used
 * earlier than some resources become available.
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
    private transient NuxeoPrincipal currentUser;

    private transient RepositoryLocation repoLocation;

    private transient Boolean serverLocationRetrieved = false;

    private boolean isServerLocationSelected() {
        if (!serverLocationRetrieved) {
            repoLocation = navigationContext.getCurrentServerLocation();
            serverLocationRetrieved = true;
        }

        return repoLocation != null;
    }

    @Observer(value = { EventNames.LOCATION_SELECTION_CHANGED }, create = false)
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
     * Checks if an user has the right to see the vocabularies management link.
     *
     * @return - true if the user has this right<br>
     *         - false otherwise
     */
    public boolean getVocabulariesEnabled() {
        if (currentUser == null) {
            return false;
        } else {
            return currentUser.isAdministrator();
        }
    }

}
