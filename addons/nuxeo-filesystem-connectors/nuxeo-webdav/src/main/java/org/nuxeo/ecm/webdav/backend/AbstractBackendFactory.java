/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.backend;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;

public abstract class AbstractBackendFactory implements BackendFactory {

    @Override
    public Backend getBackend(String path, HttpServletRequest request) {
        if (request == null) {
            throw new NullPointerException("null request");
        }
        // create backend from WebEngine session
        WebContext webContext = WebEngine.getActiveContext();
        if (webContext == null) {
            throw new NullPointerException("null WebContext");
        }
        CoreSession session = webContext.getCoreSession();
        if (session == null) {
            throw new NullPointerException("null CoreSession");
        }
        Backend backend = createRootBackend(session);
        return backend.getBackend(path);
    }

    public abstract Backend createRootBackend(CoreSession session);

}
