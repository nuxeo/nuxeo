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

package org.nuxeo.ecm.platform.usermanager;

import org.nuxeo.runtime.api.Framework;

/**
 * @deprecated You should always fetch the {@link UserManager} directly using
 *   <code>UserManager userManager = Framework.getService(UserManager.class);<code>
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Deprecated
public final class UserServiceHelper {

    // Utility class
    private UserServiceHelper() {
    }

    public static UserService getUserService() {
        return (UserService) Framework.getRuntime().getComponent(UserService.NAME);
    }

}
