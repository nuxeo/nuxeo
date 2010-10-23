/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webdav.provider;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Simple holder that will allow Jersey to close a core session at the
 * end of a request, if registered by Jersey's CloseableService.
 */
public class CoreSessionCloser implements Closeable {

    private final CoreSession session;

    public CoreSessionCloser(CoreSession session) {
        this.session = session;
    }

    @Override
    public void close() throws IOException {
        CoreInstance.getInstance().close(session);
        Util.endTransaction();
    }

}
