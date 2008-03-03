/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: PermissionProviderBean.java 28583 2008-01-08 20:00:27Z sfermigier $
 */

package org.nuxeo.ecm.core.security.ejb;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
@Stateless
@Local(PermissionProvider.class)
@Remote(PermissionProvider.class)
public class PermissionProviderBean implements PermissionProvider {

    private final PermissionProvider permissionProvider;

    public PermissionProviderBean() {
        // use the local runtime service as the backend
        SecurityService sservice = Framework.getLocalService(
                SecurityService.class);
        permissionProvider = sservice.getPermissionProvider();
    }

    public String[] getAliasPermissions(String perm) throws ClientException {
        return permissionProvider.getAliasPermissions(perm);
    }

    public String[] getPermissionGroups(String perm) {
        return permissionProvider.getPermissionGroups(perm);
    }

    public String[] getPermissions() {
        return permissionProvider.getPermissions();
    }

    public String[] getSubPermissions(String perm) throws ClientException {
        return permissionProvider.getSubPermissions(perm);
    }

    public String[] getUserVisiblePermissions() throws ClientException {
        return permissionProvider.getUserVisiblePermissions();
    }

    public String[] getUserVisiblePermissions(String typeName)
            throws ClientException {
        return permissionProvider.getUserVisiblePermissions(typeName);
    }
}
