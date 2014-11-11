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

package org.nuxeo.ecm.webapp.search;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated use {@link DocumentSearchActions} and content views instead
 */
@Deprecated
public interface SearchResults extends SelectDataModelListener {

    /**
     * Declaration for [Seam]Create method.
     */
    void init();

    void destroy();

    boolean isSortAscending() throws ClientException;

    String getSortColumn() throws ClientException;

    String repeatSearch() throws ClientException;

    PagedDocumentsProvider getProvider(String providerName)
            throws ClientException;

    SelectDataModel getResultsSelectModel(String providerName)
            throws ClientException;

    SelectDataModel getResultsSelectModelAdvanced() throws ClientException;

    SelectDataModel getResultsSelectModelNxql() throws ClientException;

    SelectDataModel getResultsSelectModelSimple() throws ClientException;

    /**
     * This will be called with Seam remoting...
     */
    @WebRemote
    String processSelectRow(String docRef, String providerName,
            Boolean selection) throws ClientException;

    boolean isSortable() throws ClientException;

    String downloadCSV() throws ClientException;

}
