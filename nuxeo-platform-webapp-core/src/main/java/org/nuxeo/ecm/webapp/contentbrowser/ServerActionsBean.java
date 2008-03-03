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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;

/**
 * Action listener that knows how to retrieve a list of core servers.
 *
 * @author <a href="mailto:ogriseln@nuxeo.com">Olivier Grisel</a>
 */
@Name("serverActions")
@Scope(CONVERSATION)
public class ServerActionsBean extends InputController implements ServerActions {

    // XXX AT: hardcoded right now
    protected static final String DEFAULT_VIEW = "view_domains";

    private static final Log log = LogFactory.getLog(ServerActionsBean.class);

    private RepositoryManager repositoryManager;

    private Collection<Repository> availableRepositories;

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Seam component destroyed...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    private RepositoryManager getRepositoryManager() throws Exception {
        if (repositoryManager == null) {
            repositoryManager = Framework.getService(RepositoryManager.class);
        }
        return repositoryManager;
    }

    /**
     * Retrieves the available locations.
     */
    @Factory("availableCoreRepositories")
    public List<Repository> getAvailableRepositories() throws ClientException {
        try {
            if (availableRepositories == null) {
                availableRepositories = getRepositoryManager().getRepositories();
            }
            List<Repository> result = new ArrayList<Repository>();
            result.addAll(availableRepositories);
            return result;

        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String selectRepository(String repositoryName) throws ClientException {
        try {
            Repository selectedRepository = null;

            for (Repository repo : getRepositoryManager().getRepositories()) {
                if (repo.getName().equals(repositoryName)) {
                    selectedRepository = repo;
                    break;
                }
            }
            log.debug("Selected core name: " + repositoryName);
            if (selectedRepository != null) {
                RepositoryLocation selectedLocation = new RepositoryLocation(
                        selectedRepository.getName());
                navigationContext.setCurrentServerLocation(selectedLocation);
                return DEFAULT_VIEW;
            } else {
                return null;
            }
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

}
