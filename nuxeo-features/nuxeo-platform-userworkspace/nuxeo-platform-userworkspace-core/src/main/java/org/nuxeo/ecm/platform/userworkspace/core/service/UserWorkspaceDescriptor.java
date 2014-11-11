/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.userworkspace.core.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;

@XObject("userWorkspace")
public class UserWorkspaceDescriptor {


    @XNode("@targetDomainName")
    private String targetDomainName = "default-domain";

    @XNode("@class")
    private Class<? extends UserWorkspaceService> userWorkspaceClass;

    public Class<? extends UserWorkspaceService> getUserWorkspaceClass() {
        return userWorkspaceClass;
    }


    public String getTargetDomainName() {
        return targetDomainName;
    }
}
