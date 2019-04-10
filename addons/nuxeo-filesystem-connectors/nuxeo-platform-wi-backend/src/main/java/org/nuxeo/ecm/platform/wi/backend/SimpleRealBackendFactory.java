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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.wi.backend;

import org.nuxeo.ecm.core.api.CoreSession;

public class SimpleRealBackendFactory implements RealBackendFactory {

    @Override
    public Backend createBackend(String backendDisplayName, String rootPath, String rootUrl) {
        return new SimpleBackend(backendDisplayName, rootPath, rootUrl);
    }

    @Override
    public Backend createBackend(String backendDisplayName, String rootPath, String rootUrl, CoreSession session) {
        return new SimpleBackend(backendDisplayName, rootPath, rootUrl, session);
    }
}
