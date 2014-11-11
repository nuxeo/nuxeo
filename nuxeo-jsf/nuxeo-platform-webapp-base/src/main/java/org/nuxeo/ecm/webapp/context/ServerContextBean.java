/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Externalize serverLocation Factory to avoid NavigationContext reentrant calls
 *
 * @author Thierry Delprat
 */
@Name("serverLocator")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class ServerContextBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private RepositoryLocation currentServerLocation;

    @Factory(value = "currentServerLocation", scope = EVENT)
    public RepositoryLocation getCurrentServerLocation() {
        return currentServerLocation;
    }

    public void setRepositoryLocation(RepositoryLocation serverLocation) {
        this.currentServerLocation = serverLocation;
    }
}
