/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
    @Override
    @Factory("availableCoreRepositories")
    public List<Repository> getAvailableRepositories() {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        return new ArrayList<>(repositoryManager.getRepositories());
    }

    @Override
    public String selectRepository(String repositoryName) {
        boolean found = false;
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String name : repositoryManager.getRepositoryNames()) {
            if (name.equals(repositoryName)) {
                found = true;
                break;
            }
        }
        if (found) {
            log.debug("Selected core name: " + repositoryName);
            RepositoryLocation selectedLocation = new RepositoryLocation(repositoryName);
            navigationContext.setCurrentServerLocation(selectedLocation);
            return DEFAULT_VIEW;
        } else {
            return null;
        }
    }

}
