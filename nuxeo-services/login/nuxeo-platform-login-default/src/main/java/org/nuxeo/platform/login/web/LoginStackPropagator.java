/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.platform.login.web;

import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPropagator;

/**
 * Propagate the login information from the web authentication filter to the client login module stack.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LoginStackPropagator implements NuxeoAuthenticationPropagator {

    public CleanupCallback propagateUserIdentificationInformation(CachableUserIdentificationInfo cachableUserIdent) {
        ClientLoginModule.getThreadLocalLogin().push(cachableUserIdent.getPrincipal(),
                cachableUserIdent.getUserInfo().getPassword().toCharArray(),
                cachableUserIdent.getLoginContext().getSubject());
        return new CleanupCallback() {

            @Override
            public void cleanup() {
                ClientLoginModule.getThreadLocalLogin().pop();
            }

        };
    }

}
