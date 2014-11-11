/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;

/**
 * Action listener that knows how to retrieve a list of core servers.
 *
 * @author Olivier Grisel
 * @author Florent Guillaume
 */
@Name("serverActions")
@Scope(CONVERSATION)
public class ServerActionsBean implements ServerActions, Serializable {

    private static final long serialVersionUID = 1L;

    // XXX AT: hardcoded right now
    protected static final String DEFAULT_VIEW = "view_domains";

    private static final Log log = LogFactory.getLog(ServerActionsBean.class);

    @In(required = true, create = true)
    protected transient NavigationContext navigationContext;

    /**
     * Retrieves the available locations.
     */
    @Factory("availableCoreRepositories")
    public List<Repository> getAvailableRepositories() throws ClientException {
        try {
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            return new ArrayList<Repository>(repositoryManager.getRepositories());
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String selectRepository(String repositoryName) throws ClientException {
        try {
            boolean found = false;
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            for (String name : repositoryManager.getRepositoryNames()) {
                if (name.equals(repositoryName)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                log.debug("Selected core name: " + repositoryName);
                RepositoryLocation selectedLocation = new RepositoryLocation(
                        repositoryName);
                navigationContext.setCurrentServerLocation(selectedLocation);
                return DEFAULT_VIEW;
            } else {
                return null;
            }
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

}
