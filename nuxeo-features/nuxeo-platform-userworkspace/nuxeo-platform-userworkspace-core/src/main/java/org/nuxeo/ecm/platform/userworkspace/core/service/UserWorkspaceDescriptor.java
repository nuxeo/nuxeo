/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

    /**
     * @since 5.9.5
     */
    @XNode("userWorkspaceType")
    protected String userWorkspaceType = "Workspace";

    /**
     * @since 5.9.5
     */
    @XNode("userWorkspaceRootType")
    protected String userWorkspaceRootType = "UserWorkspacesRoot";

    @XNode("@class")
    private Class<? extends UserWorkspaceService> userWorkspaceClass;

    public Class<? extends UserWorkspaceService> getUserWorkspaceClass() {
        return userWorkspaceClass;
    }

    public String getTargetDomainName() {
        return targetDomainName;
    }

    public String getUserWorkspaceType() {
        return userWorkspaceType;
    }

    public String getUserWorkspaceRootType() {
        return userWorkspaceRootType;
    }

}
