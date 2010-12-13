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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.platform.login.web;

import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPropagator;

/**
 * Propagate the login information from the web authentication filter to the
 * client login module stack.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginStackPropagator implements NuxeoAuthenticationPropagator {

    public void propagateUserIdentificationInformation(
            CachableUserIdentificationInfo cachableUserIdent) {
        ClientLoginModule.getThreadLocalLogin().push(
                cachableUserIdent.getPrincipal(),
                cachableUserIdent.getUserInfo().getPassword().toCharArray(),
                cachableUserIdent.getLoginContext().getSubject());
    }

}
