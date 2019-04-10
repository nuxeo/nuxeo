/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.backend;

import org.nuxeo.ecm.core.api.ClientException;
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
    public CoreSession getSession() throws ClientException {
        return getSession(false);
    }

    @Override
    public CoreSession getSession(boolean synchronize) throws ClientException {
        if (synchronize) {
            session.save();
        }
        return session;
    }

    @Override
    public void saveChanges() throws ClientException {
        session.save();
    }

}
