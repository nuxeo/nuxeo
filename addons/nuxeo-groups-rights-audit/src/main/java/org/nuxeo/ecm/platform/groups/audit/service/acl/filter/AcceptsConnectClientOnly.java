/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

public class AcceptsConnectClientOnly extends AbstractContentFilter implements
        IContentFilter {
    @Override
    public boolean acceptsUserOrGroup(String userOrGroup) {
        if (isEveryone(userOrGroup))
            return true;
        if (userOrGroup.startsWith("ConnectClient"))
            return true;
        return false;
    }
}
