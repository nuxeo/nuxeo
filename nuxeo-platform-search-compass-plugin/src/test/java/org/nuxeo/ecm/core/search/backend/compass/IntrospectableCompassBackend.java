/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.backend.compass;

import java.util.List;

import org.compass.core.CompassSession;
import org.compass.core.Resource;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.client.IndexingException;

/**
 * A class to give access to some internals to JUnit tests.
 *
 * @author <a href="mailto:gr@nuxeo.com>Georges Racinet</a>
 */
class IntrospectableCompassBackend extends CompassBackend {

    private static final long serialVersionUID = -5725481961363579633L;

    IntrospectableCompassBackend(String configurationFileName) {
        super("compass", configurationFileName);
    }

    public FakeSearchService getSearchService() {
        return (FakeSearchService) searchService;
    }

    public CompassSession openSession() {
        return getCompass().openSession();
    }

    public static Resource buildResource(CompassSession session,
            ResolvedResource resource) throws IndexingException {
        return buildResource(session, resource, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    public static Resource buildResource(CompassSession session,
            ResolvedResource resource,
            List<ResolvedData> commonData,
            String joinIdName, String joinIdValue, ACP acp)
            throws IndexingException {
        return CompassBackend.buildResource(session, null, resource, commonData,
                joinIdName, joinIdValue, acp);
    }

}
