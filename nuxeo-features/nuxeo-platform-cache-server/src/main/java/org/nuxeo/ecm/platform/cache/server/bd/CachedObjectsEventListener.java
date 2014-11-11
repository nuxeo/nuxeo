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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache.server.bd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.operation.Operation;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.platform.cache.CacheService;
import org.nuxeo.ecm.platform.cache.CacheServiceException;
import org.nuxeo.ecm.platform.cache.CacheableObjectKeys;

/**
 * A CoreEvent events listener that invalidates referenced entries in the cache
 * (DocumentModel implementations) for notifications like document deletion,
 * updating.
 *
 * @author DM
 */
public class CachedObjectsEventListener implements EventListener {

    private static final Log log = LogFactory.getLog(CachedObjectsEventListener.class);

    private final CacheService cacheService;

    /**
     * An instance of this class maintains a reference to a CacheService through
     * which objects in the real cache can be invalidated (removed).
     *
     * @param cacheService
     *            the cache service accessor to the Cache where reside objects
     *            to be invalidated
     */
    public CachedObjectsEventListener(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void handleEvent(CoreEvent coreEvent) {
        assert coreEvent != null;

        log.debug("Handle notifyEvent " + coreEvent);

        if (coreEvent.getSource() instanceof Document) {
            final Document doc = (Document) coreEvent.getSource();

            final String eventId = coreEvent.getEventId();

            try {
                handleDocumentEvent(doc, eventId);
            } catch (DocumentException e) {
                // e.printStackTrace();

                log.error("Error handling notification for Document ", e);
            } catch (CacheServiceException e) {
                // e.printStackTrace();

                log.error("Cache error while handling notification for "
                        + "Document ", e);
            }
        } else {
            log.debug("[USELESS NOTIFICATION] "
                    + "coreEvent source not interesting: "
                    + coreEvent.getSource());
        }
    }

    private void handleDocumentEvent(final Document doc, final String eventId)
            throws DocumentException, CacheServiceException {

        // we get a cacheKey only if there is a valid eventId to avoid
        // unnecessary calculation

        if (DocumentEventTypes.DOCUMENT_CREATED.equals(eventId)) {
            // we'll have to notify all lists that could potentially include
            // this document
            // TODO CacheableObjectKeys.getCacheKey(doc.getPath());
        } else if (DocumentEventTypes.DOCUMENT_UPDATED.equals(eventId)) {
            // shall invalidate the cache copy of the DocumentModel if there
            updateDocInCache(doc);
        } else if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(eventId)) {
            // shall remove the cache copy of the DocumentModel if there
            removeDocFromCache(doc);
        } else {
            log.debug("coreEvent id=" + eventId
                    + " not used for cache invalidation");
        }
    }

    private void updateDocInCache(Document doc) throws DocumentException,
            CacheServiceException {
        final String logPrefix = "UpdateDocInCache:: ";

        String cacheKey = CacheableObjectKeys.getCacheKeyForDocPath(doc
                .getPath());
        if (cacheService.exists(cacheKey)) {
            // TODO : will have to merge the doc in the cache...
            // but DO NOT remove it and add it after
            // cacheService.getObject(fqn)removeObject(cacheKey);
            final DocumentModel cacheDoc = (DocumentModel) cacheService
                    .getObject(cacheKey);
            // AbstractSession.writeModel(doc, cacheDoc);
            updateDocumentModel(cacheDoc, doc);

            log.debug(logPrefix
                    + "Updated Document in the cache with Path based key "
                    + cacheKey);
        } else {
            log.debug("Document not found in the cache with Path based key "
                    + cacheKey);
        }

        // it might exist also with ID as path
        cacheKey = CacheableObjectKeys.getCacheKeyForDocUUID(doc.getUUID());
        if (cacheService.exists(cacheKey)) {
            final DocumentModel cacheDoc = (DocumentModel) cacheService
                    .getObject(cacheKey);
            updateDocumentModel(cacheDoc, doc);
            log.debug(logPrefix + "Updated document in cache. UUID based key "
                    + cacheKey);
        } else {
            log.debug(logPrefix
                    + "Document not found in the cache with UUID based key "
                    + cacheKey);
        }
    }

    private void updateDocumentModel(DocumentModel cacheDoc, Document doc)
            throws DocumentException {
        DocumentModelImpl model = DocumentModelFactory.createDocumentModel(doc);
        if (cacheDoc instanceof DocumentModelImpl) {
            model.copyContentInto((DocumentModelImpl) cacheDoc);
        } else {
            throw new DocumentException("Unsupported document type");
        }
    }

    private void removeDocFromCache(Document doc) throws DocumentException,
            CacheServiceException {
        final String logPrefix = "RemoveDocFromCache:: ";

        log.debug(logPrefix + "Trying to remove document from cache");
        // check with doc path first
        String cacheKey = CacheableObjectKeys.getCacheKeyForDocPath(doc
                .getPath());
        if (cacheService.exists(cacheKey)) {
            cacheService.removeObject(cacheKey);
            log.debug(logPrefix
                    + "Removed document from cache. Doc path based key "
                    + cacheKey);
        } else {
            log.debug(logPrefix
                    + "Document not found in the cache with Path based key "
                    + cacheKey);
        }

        // it might exist also with ID as path
        cacheKey = CacheableObjectKeys.getCacheKeyForDocUUID(doc.getUUID());
        if (cacheService.exists(cacheKey)) {
            cacheService.removeObject(cacheKey);
            log
                    .debug(logPrefix
                            + "Removed document from cache. UUID based key "
                            + cacheKey);
        } else {
            log.debug(logPrefix
                    + "Document not found in the cache with UUID based key "
                    + cacheKey);
        }
    }

    public void setName(String name) {
        log.debug("EventListener :: setName not implemented");
    }

    public String getName() {
        log.debug("EventListener :: getName not implemented");
        return null;
    }

    public Integer getOrder() {
        log.debug("EventListener :: getOrder not implemented");
        return 0;
    }

    public void setOrder(Integer order) {
        log.debug("EventListener :: setOrder not implemented");
    }

    public void addEventId(String eventId) {
        log.debug("EventListener :: addEventId not implemented");
    }

    public void removeEventId(String eventId) {
        log.debug("EventListener :: removeEventId not implemented");
    }

    public boolean accepts(String eventId) {
        log.debug("EventListener :: accepts not implemented");
        return false;
    }

    public void operationStarted(Operation<?> cmd) throws Exception {
        // ignore this for now
    }

    public void operationTerminated(Operation<?> cmd) throws Exception {
        // ignore this for now
    }
}
