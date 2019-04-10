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

import org.nuxeo.ecm.core.api.CoreSession;

public abstract class AbstractCoreBackend implements Backend {

    protected CoreSession session;

    public AbstractCoreBackend() {
        super();
    }

    protected AbstractCoreBackend(CoreSession session) {
        this.session = session;
    }

    @Override
    public CoreSession getSession() {
        return getSession(false);
    }

    @Override
    public CoreSession getSession(boolean synchronize) {
        if (synchronize) {
            session.save();
        }
        return session;
    }

    @Override
    public void saveChanges() {
        session.save();
    }

}
