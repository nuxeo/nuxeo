/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Michal Obrebski - Nuxeo
 */

package org.nuxeo.easyshare;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.web.common.RequestCleanupHandler;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

public abstract class EasyShareUnrestrictedRunner {

    protected CoreSession session;

    public Object runUnrestricted(String docId) {
        NuxeoLoginContext loginContext = Framework.loginSystem();
        CloseableCoreSession coreSession = null;
        try {
            coreSession = CoreInstance.openCoreSession(null);

            // Run unrestricted operation
            IdRef docRef = new IdRef(docId);
            return run(coreSession, docRef);

        } finally {
            final CloseableCoreSession session2close = coreSession;
            RequestContext.getActiveContext().addRequestCleanupHandler(new RequestCleanupHandler() {

                @Override
                public void cleanup(HttpServletRequest req) {
                    session2close.close();
                    loginContext.close();
                }
            });

        }

    }

    public abstract Object run(CoreSession coreSession, IdRef docId);
}
