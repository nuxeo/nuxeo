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
 * $Id$
 */

package org.nuxeo.ecm.webapp.delegate;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import javax.annotation.security.PermitAll;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("userManager")
@Scope(CONVERSATION)
public class UserManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 8469446313008631864L;

    protected UserManager userManager;

    /**
     * Acquires a new {@link UserManager} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     */
    @Unwrap
    public UserManager getUserManager() throws ClientException {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);

            } catch (Exception e) {
                final String errMsg = "Error connecting to UserManager. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }

            if (null == userManager) {
                throw new ClientException("UserManager service not bound");
            }
        }
        return userManager;
    }

    @Destroy
    @PermitAll
    public void destroy() {
        if (userManager != null) {
            userManager = null;
        }
    }
}
