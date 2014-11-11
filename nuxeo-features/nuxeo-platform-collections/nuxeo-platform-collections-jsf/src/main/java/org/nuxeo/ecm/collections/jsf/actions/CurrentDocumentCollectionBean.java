/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.jsf.actions;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
@Name("currentDocumentCollection")
@Scope(ScopeType.PAGE)
@BypassInterceptors
public class CurrentDocumentCollectionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected boolean hasCurrentDocumentMoreCollectionToDisplay = false;

    private boolean isDisplayAll;

    public List<DocumentModel> getCurrentDocumentCollections()
            throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        if (!collectionManager.isCollectable(currentDocument)) {
            return null;
        }
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);
        List<DocumentModel> result = collectionManager.getVisibleCollection(
                currentDocument,
                isDisplayAll ? CollectionConstants.MAX_COLLECTION_RETURNED
                        : CollectionConstants.DEFAULT_COLLECTION_RETURNED,
                session);
        if (!isDisplayAll
                && result.size() == CollectionConstants.DEFAULT_COLLECTION_RETURNED) {
            hasCurrentDocumentMoreCollectionToDisplay = true;
        } else {
            isDisplayAll = true;
            hasCurrentDocumentMoreCollectionToDisplay = false;
        }
        return result;
    }

    public Boolean getHasCurrentDocumentMoreCollectionToDisplay() {
        return hasCurrentDocumentMoreCollectionToDisplay;
    }

    public boolean isDisplayAll() {
        return isDisplayAll;
    }

    public void setDisplayAll(final boolean isDisplayAll) {
        this.isDisplayAll = isDisplayAll;
    }

}
