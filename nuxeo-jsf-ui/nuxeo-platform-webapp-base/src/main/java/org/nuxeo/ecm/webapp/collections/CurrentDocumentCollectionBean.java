/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.webapp.collections;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
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

    public List<DocumentModel> getCurrentDocumentCollections() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        if (!collectionManager.isCollectable(currentDocument)) {
            return null;
        }
        final CoreSession session = (CoreSession) Component.getInstance("documentManager", true);
        List<DocumentModel> result = collectionManager.getVisibleCollection(currentDocument,
                isDisplayAll ? CollectionConstants.MAX_COLLECTION_RETURNED
                        : CollectionConstants.DEFAULT_COLLECTION_RETURNED, session);
        if (!isDisplayAll && result.size() == CollectionConstants.DEFAULT_COLLECTION_RETURNED) {
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
