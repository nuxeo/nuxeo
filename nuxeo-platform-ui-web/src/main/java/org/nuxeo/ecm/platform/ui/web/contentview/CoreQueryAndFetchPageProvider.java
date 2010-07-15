/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.contentview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.AbstractPageProvider;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PageProvider;
import org.nuxeo.ecm.core.api.PageSelections;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * @author Anahide Tchertchian
 */
public class CoreQueryAndFetchPageProvider extends
        AbstractPageProvider<Map<String, Serializable>> implements
        PageProvider<Map<String, Serializable>> {

    private static final long serialVersionUID = 1L;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String QUERY_PROPERTY = "query";

    protected String query;

    protected List<Map<String, Serializable>> currentItems;

    @Override
    public List<Map<String, Serializable>> getCurrentPage() {
        try {
            buildQuery();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        if (currentItems == null) {

            currentItems = new ArrayList<Map<String, Serializable>>();

            Map<String, Serializable> props = getProperties();
            CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
            if (coreSession == null) {
                throw new ClientRuntimeException("cannot find core session");
            }

            IterableQueryResult result = null;
            try {
                result = coreSession.queryAndFetch(query, "NXQL");
                resultsCount = result.size();
                if (offset < resultsCount) {
                    result.skipTo(offset);
                }

                Iterator<Map<String, Serializable>> it = result.iterator();
                int pos = 0;
                while (it.hasNext() && pos < pageSize) {
                    pos += 1;
                    Map<String, Serializable> item = it.next();
                    currentItems.add(item);
                }

            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            } finally {
                if (result != null) {
                    result.close();
                }
            }
        }

        return currentItems;
    }

    protected void buildQuery() throws ClientException {
        Map<String, Serializable> props = getProperties();

        String originalQuery = (String) props.get(QUERY_PROPERTY);
        if (originalQuery != null) {
            // remove new lines and following spaces
            originalQuery = originalQuery.replaceAll("\r?\n\\s*", " ");
        }

        SortInfo[] sortArray = null;
        if (sortInfos != null) {
            sortArray = sortInfos.toArray(new SortInfo[] {});
        }
        String newQuery = NXQLQueryBuilder.getQuery(originalQuery,
                getParameters(), sortArray);

        if (!newQuery.equals(query)) {
            // query has changed => refresh
            refresh();
        }
    }

    @Override
    public PageSelections<Map<String, Serializable>> getCurrentSelectPage() {
        // handle refresh of select page according to query
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new ClientRuntimeException("cannot find core session");
        }

        try {
            buildQuery();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        return super.getCurrentSelectPage();
    }

    @Override
    public void refresh() {
        super.refresh();
        currentItems = null;
    }

}
