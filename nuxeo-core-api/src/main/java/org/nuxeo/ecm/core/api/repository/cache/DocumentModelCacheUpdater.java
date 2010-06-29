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

package org.nuxeo.ecm.core.api.repository.cache;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.operation.Modification;
import org.nuxeo.ecm.core.api.operation.OperationEvent;
import org.nuxeo.ecm.core.api.operation.OperationEventListener;

/**
 * @author Max Stepanov
 */
public class DocumentModelCacheUpdater implements OperationEventListener {
    private static final Log log = LogFactory.getLog(DocumentModelCacheUpdater.class);

    private final DocumentModelCache cache;;

    public DocumentModelCacheUpdater(DocumentModelCache cache) {
        this.cache = cache;
    }

    protected List<DocumentModelCacheListener> listeners;

    public void handleEvents(OperationEvent[] events, boolean urgent) {
        HashSet<DocumentModel> updatedDocs = new HashSet<DocumentModel>(
                events.length);
        HashSet<DocumentModel> updatedTrees = new HashSet<DocumentModel>(
                events.length);
        for (int i = 0; i < events.length; ++i) {
            try {
                handleEvent(cache, updatedDocs, updatedTrees, events[i]);
            } catch (Exception e) {
                log.error("Exception handling event", e);
            }
        }
        if (!listeners.isEmpty()) {
            if (!updatedDocs.isEmpty()) {
                DocumentModel[] docs = updatedDocs.toArray(new DocumentModel[updatedDocs.size()]);
                for (DocumentModelCacheListener listener:listeners) {
                    try {
                    listener.documentsChanged(docs, urgent);
                    } catch (Throwable error) {
                        log.error("An error while trying fire listener for document modifications", error);
                    }
                }
            }
            if (!updatedTrees.isEmpty()) {
                DocumentModel[] docs = updatedTrees.toArray(new DocumentModel[updatedTrees.size()]);
                for (DocumentModelCacheListener listener : listeners) {
                    try {
                        listener.subreeChanged(docs, urgent);
                    } catch (Throwable error) {
                        log.error("An error while trying fire listener for document modifications", error);
                    }
                }
            }
        }

    }

    public void handleEvent(DocumentModelCache cache,
            HashSet<DocumentModel> updatedDocs,
            HashSet<DocumentModel> updatedTrees, OperationEvent event)
            throws ClientException {
        DocumentRef childRef = null;
        for (Modification modif : event.getModifications()) {
            DocumentModel doc = null;
            try {
                doc = cache.getCachedDocument(modif.ref);
                if (doc == null) {
                    if (modif.isCreate()) {
                        childRef = modif.ref;
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace("Modif " + modif.ref + " [" + modif.type
                                    + "] - not in cache!");
                        }
                    }
                    continue;
                }
                if (modif.isContainerModification()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Modif " + modif.ref + " [" + modif.type
                                + "] - update children");
                    }
                    if (modif.isOrderChild()) {
                        childRef = null;
                    }
                    handleContainerModification(cache, updatedTrees, modif, doc,
                            childRef);
                } else {
                    childRef = null;
                }
                if (modif.isExistenceModification()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Modif " + modif.ref + " [" + modif.type
                                + "] - existence check");
                    }
                    childRef = modif.ref;
                }
                if (modif.isUpdateModification()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Modif " + modif.ref + " [" + modif.type
                                + "] - update content");
                    }
                    handleUpdateModification(cache, updatedDocs, doc);
                }
            }
            catch (Exception e) {
                StringBuilder sb = new StringBuilder("In event " + event.getId() + ", error handling ");
                if (modif.isCreate())
                    sb.append("create ");
                if (modif.isAddChild())
                    sb.append("add-child ");
                if (modif.isOrderChild())
                    sb.append("order-child ");
                if (modif.isRemove())
                    sb.append("remove ");
                if (modif.isRemoveChild())
                    sb.append("remove-child ");
                if (modif.isUpdateModification())
                    sb.append("update ");
                sb.append("modification " + modif);
                if (doc != null)
                    sb.append("\nCached DocumentModel '" + doc.getTitle()
                            + "' [type=" + doc.getType()
                            + " id=" + doc.getId()
                            + " path=" + doc.getPathAsString() + "\n");
                else
                    sb.append("\nCached DocumentModel is null\n");
                sb.append("childref=" + childRef + "\n");
                sb.append("\nWhilst processing the following Modification set:\n");
                for (Modification m : event.getModifications()) {
                    sb.append(m + (m == modif ? " (the cause)" : "") + "\n");
                }
                sb.append("\nThis was caused by " + e.getMessage());
                throw new ClientException(sb.toString(), e);
            }
        }
    }

    private void handleUpdateModification(DocumentModelCache cache,
            HashSet<DocumentModel> updatedDocs, DocumentModel doc)
            throws ClientException {
        if (updatedDocs.add(doc)) {
            doc.refresh();
        }
    }

    private void handleContainerModification(DocumentModelCache cache,
            HashSet<DocumentModel> updatedTrees, Modification modif,
            DocumentModel doc, DocumentRef childRef) throws ClientException {
        if (childRef != null && modif.isRemoveChild()) {
            cache.uncacheDocument(childRef);
            cache.uncacheChild(doc.getRef(), childRef);
            updatedTrees.add(doc);
            return;
        } else if (childRef != null && modif.isAddChild()) {
            if (cache.getCachedChildren(doc.getRef()) != null) {
                DocumentModel model = null;
                if ((model = cache.fetchDocument(childRef)) != null) {
                    if (model.getParentRef().equals(doc.getRef())) {
                        cache.cacheChild(doc.getRef(), childRef);
                        updatedTrees.add(doc);
                    }
                }
            }
            return;
        } else if (childRef != null) {
            cache.uncacheChildren(doc.getRef());
            updatedTrees.add(doc);
            return;
        }
        if (cache.getCachedChildren(doc.getRef()) != null) {
            cache.fetchAndCacheChildren(doc.getRef());
            updatedTrees.add(doc);
        }
    }

}
