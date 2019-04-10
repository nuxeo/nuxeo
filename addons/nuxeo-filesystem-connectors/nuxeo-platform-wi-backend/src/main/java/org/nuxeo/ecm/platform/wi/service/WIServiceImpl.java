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
package org.nuxeo.ecm.platform.wi.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.ecm.platform.wi.filter.SessionCacheHolder;
import org.nuxeo.ecm.platform.wi.filter.WISession;

import java.security.Principal;

public class WIServiceImpl implements WIService {

    @Override
    public String getPathById(String uuid, CoreSession session)
            throws ClientException {
        DocumentModel model = session.getDocument(new IdRef(uuid));
        if (model == null) {
            return null;
        }

        String path = model.getPathAsString();
        Backend backend = getBackend(session);
        return backend.getVirtualPath(path);
    }

    public void invalidateCache() {
        SessionCacheHolder.getInstance().getCache().invalidateCache();
    }

    private Backend getBackend(CoreSession session) {
        if (session == null) {
            return null;
        }
        Principal principal = session.getPrincipal();
        if (principal == null || StringUtils.isEmpty(principal.getName())) {
            return null;
        }
        String principalName = principal.getName();
        WISession wiSession = SessionCacheHolder.getInstance().getCache().get(
                principalName);
        wiSession.setAttribute(WISession.CORESESSION_KEY, session);
        return new PluggableBackendFactory().getBackend(wiSession);
    }

}
