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
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.backend.security;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Shared info about security filtering.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public final class SecurityFiltering {

    /**
     * This is the list of all permissions that grant access to some indexed
     * document. This is used in place of groups of permissions resolution that
     * aren't accessible in async mode for now.
     *
     * @deprecated use getBrowsePermissionList() instead
     */
    @Deprecated
    public static final List<String> GRANT = Arrays.asList(
            SecurityConstants.BROWSE, SecurityConstants.EVERYTHING,
            SecurityConstants.READ, SecurityConstants.READ_WRITE);

    public static final String[] BROWSE_PERMISSION_SEEDS = { SecurityConstants.BROWSE };

    /**
     * Return the recursive closure of all permissions that comprises the
     * requested seed permissions.
     *
     * TODO: this logics should be moved upward to the PermissionProvider
     * interface.
     *
     * @param seedPermissions
     * @return the list of permissions, seeds inclusive
     * @throws Exception
     */
    public static List<String> getPermissionList(String[] seedPermissions)
            throws Exception {
        PermissionProvider pprovider = Framework.getService(PermissionProvider.class);
        List<String> aggregatedPerms = new LinkedList<String>();
        for (String seedPerm : seedPermissions) {
            aggregatedPerms.add(seedPerm);
            String[] compoundPerms = pprovider.getPermissionGroups(seedPerm);
            if (compoundPerms != null) {
                aggregatedPerms.addAll(Arrays.asList(compoundPerms));
            }
        }
        // EVERYTHING is special and may not be explicitly registered as a
        // compound
        if (!aggregatedPerms.contains(SecurityConstants.EVERYTHING)) {
            aggregatedPerms.add(SecurityConstants.EVERYTHING);
        }
        return aggregatedPerms;
    }

    /**
     * This is the list of all permissions that grant access to some indexed
     * document.
     *
     * @return the list of all permissions that include Browse directly or
     *         un-directly
     * @throws Exception
     */
    public static List<String> getBrowsePermissionList() throws Exception {
        return getPermissionList(BROWSE_PERMISSION_SEEDS);
    }

    public static final String SEPARATOR = "#";

    public static final String ESCAPE = "[#]";

    // Constant utility class.
    private SecurityFiltering() {
    }

}
