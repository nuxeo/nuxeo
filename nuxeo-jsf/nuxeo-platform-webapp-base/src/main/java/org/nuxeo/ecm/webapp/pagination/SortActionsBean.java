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

package org.nuxeo.ecm.webapp.pagination;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@Name("sortActions")
@Scope(ScopeType.CONVERSATION)
public class SortActionsBean implements SortActions, Serializable {

    private static final long serialVersionUID = 6824092797019313562L;
    private static final Log log = LogFactory.getLog(SortActionsBean.class);

    @RequestParameter("defaultSortAscending")
    protected Boolean defaultSortAscending = true;

    @RequestParameter("sortColumn")
    protected String newSortColumn = "dc:title";

    @In(required = false, create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @RequestParameter("invalidateSeamVariables")
    protected String invalidateSeamVariables;

    @RequestParameter("providerName")
    protected String providerName;

    @Deprecated
    public void init() {
        log.debug("Initializing...");
    }

    @Deprecated
    public void destroy() {
        log.debug("Destroy...");
    }

    protected boolean getDefaultSortOrder(String column) {
        if (defaultSortAscending != null) {
            return defaultSortAscending;
        }
        return true;
    }

    public String repeatSearch() throws ClientException {
        if (providerName == null) {
            throw new IllegalArgumentException("providerName is not set");
        }

        if (newSortColumn == null) {
            throw new IllegalArgumentException("newSortColumn is not set");
        }

        if (invalidateSeamVariables != null) {
            String[] variables = invalidateSeamVariables.split(",");
            for (String variable : variables) {
                Contexts.removeFromAllContexts(variable);
            }
        }

        SortInfo sortInfo = resultsProvidersCache.get(providerName).getSortInfo();

        if (sortInfo == null) {
            sortInfo = new SortInfo(newSortColumn, getDefaultSortOrder(newSortColumn));
        } else {
            // toggle newOrderDirection
            String sortColumn = sortInfo.getSortColumn();
            boolean sortAscending = sortInfo.getSortAscending();
            if (newSortColumn.equals(sortColumn)) {
                sortAscending = !sortAscending;
            } else {
                sortColumn = newSortColumn;
                sortAscending = getDefaultSortOrder(newSortColumn);
            }
            sortInfo = new SortInfo(sortColumn, sortAscending);
        }
        resultsProvidersCache.invalidate(providerName);
        resultsProvidersCache.get(providerName, sortInfo);

        return null;
    }

}
