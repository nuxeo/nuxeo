/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;

@XObject(value = "sessionManager")
public class SessionManagerDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("@class")
    Class<NuxeoAuthenticationSessionManager> className;

    public String getName() {
        return name;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public Class<NuxeoAuthenticationSessionManager> getClassName() {
        return className;
    }

}
