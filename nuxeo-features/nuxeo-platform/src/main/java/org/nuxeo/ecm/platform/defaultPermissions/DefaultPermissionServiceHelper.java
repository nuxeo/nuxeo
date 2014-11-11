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

package org.nuxeo.ecm.platform.defaultPermissions;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated remove in 5.2
 */
@Deprecated
public final class DefaultPermissionServiceHelper {

    // Utility class.
    private DefaultPermissionServiceHelper() {
    }

    public static DefaultPermissionService getService() {
        return (DefaultPermissionService) Framework.getRuntime().getComponent(
                DefaultPermissionService.ID);
    }

}
