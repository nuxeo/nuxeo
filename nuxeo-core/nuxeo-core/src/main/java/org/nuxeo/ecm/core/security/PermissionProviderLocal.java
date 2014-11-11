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
 * $Id: PermissionProviderLocal.java 28325 2007-12-24 08:29:26Z sfermigier $
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.api.security.PermissionProvider;

public interface PermissionProviderLocal extends PermissionProvider {

    void registerDescriptor(PermissionDescriptor descriptor)
            throws Exception;

    void unregisterDescriptor(PermissionDescriptor descriptor);

    void registerDescriptor(PermissionVisibilityDescriptor descriptor);

    void unregisterDescriptor(PermissionVisibilityDescriptor descriptor);

}
