/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.collections.jsf.actions;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.webapp.action.ImportActions.DOCUMENTS_IMPORTED;
import static org.nuxeo.ecm.webapp.bulkedit.BulkEditActions.SELECTION_EDITED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.6
 */
@Name("collectionBulkEditActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class CollectionBulkEditActions implements Serializable {

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @SuppressWarnings("unchecked")
    @Observer({ SELECTION_EDITED, DOCUMENTS_IMPORTED })
    public void addCollectionsOnEvent(List<DocumentModel> documents,
            DocumentModel doc) throws ClientException {
        List<String> collectionIds = (List<String>) doc.getContextData(
                org.nuxeo.common.collections.ScopeType.REQUEST,
                "bulk_collections");
        if (collectionIds != null && !collectionIds.isEmpty()) {
            List<DocumentModel> collections = new ArrayList<>();
            for (String collectionId : collectionIds) {
                IdRef idRef = new IdRef(collectionId);
                if (documentManager.exists(idRef)) {
                    collections.add(documentManager.getDocument(idRef));
                }
            }

            CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            for (DocumentModel collection : collections) {
                if (collectionManager.canAddToCollection(collection,
                        documentManager)) {
                    collectionManager.addToCollection(collection, documents,
                            documentManager);
                }
            }
        }
    }

}
