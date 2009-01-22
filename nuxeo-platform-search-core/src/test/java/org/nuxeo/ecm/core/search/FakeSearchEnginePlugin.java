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
 * $Id:FakeSearchEnginePlugin.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.core.search;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.backend.impl.AbstractSearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class FakeSearchEnginePlugin extends AbstractSearchEngineBackend {

    private static final long serialVersionUID = -9160814888437944361L;

    public void index(ResolvedResources resources) throws IndexingException {
    }

    public void deleteAggregatedResources(String key) throws IndexingException {
    }

    public ResultSet searchQuery(ComposedNXQuery nxqlQuery, int offset, int range)
            throws SearchException {
        return null;
    }

    public ResultSet searchQuery(SQLQuery nxqlQuery, int offset, int range) {
        return null;
    }

    public ResultSet searchQuery(NativeQueryString queryString, int offset, int range) {
        return null;
    }

    public Object computeSecurityIndex(ACP acp) {
        return null;
    }

    public NativeQuery convertToNativeQuery(ComposedNXQuery query) {
        return null;
    }

    public ResultSet searchQuery(NativeQuery nativeQuery, int offset,
            int range) {
        return null;
    }

    public void deleteAtomicResource(String key) {
    }

    public void clear() throws IndexingException {
    }

    public void closeSession(String sid) {
    }

    public SearchServiceSession createSession() {
        return null;
    }

    public void saveAllSessions() throws IndexingException {
    }

}
