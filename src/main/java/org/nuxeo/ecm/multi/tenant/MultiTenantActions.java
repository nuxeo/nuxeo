/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.jboss.seam.ScopeType.STATELESS;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webapp.directory.DirectoryUIActionsBean;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@Name("multiTenantActions")
@Scope(STATELESS)
@Install(precedence = FRAMEWORK)
public class MultiTenantActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected DirectoryUIActionsBean directoryUIActions;

    public List<DocumentModel> getTenants() throws ClientException {
        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        return multiTenantService.getTenants();
    }

    public boolean isTenantIsolationEnabled() throws ClientException {
        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        return multiTenantService.isTenantIsolationEnabled(documentManager);
    }

    public void enableTenantIsolation() throws ClientException {
        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        multiTenantService.enableTenantIsolation(documentManager);
    }

    public void disableTenantIsolation() throws ClientException {
        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        multiTenantService.disableTenantIsolation(documentManager);
    }

    public boolean isReadOnlyDirectory(String directoryName)
            throws ClientException {
        MultiTenantService multiTenantService = Framework.getLocalService(MultiTenantService.class);
        if (multiTenantService.isTenantIsolationEnabled(documentManager)) {
            if (multiTenantService.isTenantAdministrator(documentManager.getPrincipal())) {
                DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
                return !directoryService.getDirectory(directoryName).isMultiTenant();
            }
        }
        return directoryUIActions.isReadOnly(directoryName);
    }

}
