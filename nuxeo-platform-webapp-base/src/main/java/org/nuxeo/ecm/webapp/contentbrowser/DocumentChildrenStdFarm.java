/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentsPageProvider;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Creates ResultsProvider for the children of the current document.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Name("documentChildrenFarm")
@Scope(SESSION)
public class DocumentChildrenStdFarm extends InputController implements
        ResultsProviderFarm, Serializable {

    // Result providers
    public static final String CHILDREN_BY_COREAPI = "CURRENT_DOC_CHILDREN";

    private static final long serialVersionUID = 8609573595065569339L;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true)
    private DocumentChildrenSearchFarm documentChildrenSearchFarm;

    public PagedDocumentsProvider getResultsProvider(String name,
            SortInfo sortInfo) throws ClientException {

        final DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (CHILDREN_BY_COREAPI.equals(name)) {

            final boolean browseViaSearch = currentDoc.hasFacet(FacetNames.BROWSE_VIA_SEARCH);

            PagedDocumentsProvider provider;
            if (browseViaSearch) {
                provider = documentChildrenSearchFarm.getResultsProvider(name,
                        sortInfo);
            } else {
                provider = getResProviderForDocChildren(currentDoc.getRef());
            }
            provider.setName(name);
            return provider;
        } else {
            throw new ClientException("Unknown (or not supported) provider: "
                    + name);
        }
    }

    private PagedDocumentsProvider getResProviderForDocChildren(
            DocumentRef docRef) throws ClientException {
        FacetFilter facetFilter = new FacetFilter(
                FacetNames.HIDDEN_IN_NAVIGATION, false);
        LifeCycleFilter lifeCycleFilter = new LifeCycleFilter(
                LifeCycleConstants.DELETED_STATE, false);
        CompoundFilter filter = new CompoundFilter(facetFilter, lifeCycleFilter);
        DocumentModelIterator resultDocsIt = documentManager.getChildrenIterator(
                docRef, null, SecurityConstants.READ, filter);

        return new DocumentsPageProvider(resultDocsIt, 10);
    }

    public PagedDocumentsProvider getResultsProvider(String name)
            throws ClientException, ResultsProviderFarmUserException {
        return getResultsProvider(name, null);
    }
}
