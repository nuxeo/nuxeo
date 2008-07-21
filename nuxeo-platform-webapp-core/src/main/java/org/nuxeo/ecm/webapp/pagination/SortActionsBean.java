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
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Name("sortActions")
@Scope(ScopeType.CONVERSATION)
public class SortActionsBean extends InputController implements SortActions, Serializable {

    private static final long serialVersionUID = 6824092797019313562L;
    private static final Log log = LogFactory.getLog(SortActionsBean.class);

    @RequestParameter("sortColumn")
    private String newSortColumn;

    @In(required = false, create = true)
    private transient ResultsProvidersCache resultsProvidersCache;

    @RequestParameter("providerName")
    private String providerName;

    public void init() throws ClientException {
        log.debug("Initializing...");
    }

    public void destroy() {
        log.debug("Destroy...");
    }

    public String repeatSearch() throws ClientException {
        if (providerName == null) {
            throw new IllegalArgumentException("providerName is not set");
        }

        if (newSortColumn == null) {
            throw new IllegalArgumentException("newSortColumn is not set");
        }

        SortInfo sortInfo = resultsProvidersCache.get(providerName).getSortInfo();

        if (sortInfo == null) {
            sortInfo = new SortInfo("dc:title", true);
        }

        // toggle newOrderDirection
        String sortColumn = sortInfo.getSortColumn();
        boolean sortAscending = sortInfo.getSortAscending();
        if (newSortColumn.equals(sortColumn)) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = newSortColumn;
            sortAscending = true;
        }
        sortInfo = new SortInfo(sortColumn, sortAscending);
        resultsProvidersCache.invalidate(providerName);
        resultsProvidersCache.get(providerName, sortInfo);

        return null;
    }

}
