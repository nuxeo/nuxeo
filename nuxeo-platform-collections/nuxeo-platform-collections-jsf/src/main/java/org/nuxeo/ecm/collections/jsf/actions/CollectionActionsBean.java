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

import static org.jboss.seam.international.StatusMessage.Severity.INFO;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @since 5.9.3
 */
@Name("collectionActions")
@Scope(ScopeType.PAGE)
public class CollectionActionsBean implements Serializable {

    private static final long serialVersionUID = 6091077088147407371L;

    private static final Log log = LogFactory.getLog(CollectionActionsBean.class);

    @In
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    private transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    private String selectedCollectionUid;

    private DocumentModel selectedCollection;

    private String newDescription;

    private String newTitle;

    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }

    public String getSelectedCollectionUid() {
        return selectedCollectionUid;
    }

    public void setSelectedCollectionUid(final String selectedCollectionUid) {
        this.selectedCollectionUid = selectedCollectionUid;
        if (isCreateNewCollection()) {
            setNewTitle(selectedCollectionUid.substring(CollectionConstants.MAGIC_PREFIX_ID.length()));
        }
    }

    public void addDocumentToCollection() {
        facesMessages.add(INFO, "Hello World");
    }

    public void addToCollection() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            if (isCreateNewCollection()) {
                collectionManager.addToNewCollection(getNewTitle(), getNewDescription(),
                        currentDocument, documentManager);
            } else {
                collectionManager.addToCollection(getSelectedCollection(),
                        currentDocument, documentManager);
            }
            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("collection.addedToCollection"),
                    messages.get(isCreateNewCollection() ? getNewTitle() : getSelectedCollection().getTitle()));
        }
    }

    protected DocumentModel getSelectedCollection() {
        if (selectedCollection == null
                && StringUtils.isNotBlank(selectedCollectionUid)
                && !isCreateNewCollection()) {
            try {
                selectedCollection = documentManager.getDocument(new IdRef(
                        selectedCollectionUid));
            } catch (ClientException e) {
                log.error("Cannot fetch collection");
            }
        }
        return selectedCollection;
    }

    public boolean canCurrentDocumentBeCollected() {
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        return collectionManager.isCollectable(currentDocument);
    }

    public boolean canAddToCollection() {
        CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        boolean result = (getSelectedCollection() != null && collectionManager.isCollection(getSelectedCollection()))
                || isCreateNewCollection();
        return result;
    }

    public void cancel() {
        selectedCollectionUid = null;
        newDescription = null;
        newTitle = null;
    }

    public boolean isCreateNewCollection() {
        return selectedCollectionUid != null
                && selectedCollectionUid.startsWith(CollectionConstants.MAGIC_PREFIX_ID);
    }

}
