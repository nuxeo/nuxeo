/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core.blob;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Manages the cold storage of the main content of a {@link DocumentModel}.
 *
 * @since 11.1
 */
public class ColdStorageHelper {

    private static final Logger log = LogManager.getLogger(ColdStorageHelper.class);

    public static final String FILE_CONTENT_PROPERTY = "file:content";

    public static final String COLD_STORAGE_CONTENT_PROPERTY = "coldstorage:coldContent";

    public static final String COLD_STORAGE_BEING_RETRIEVED_PROPERTY = "coldstorage:beingRetrieved";

    public static final String GET_DOCUMENTS_TO_CHECK_QUERY = "SELECT * FROM Document WHERE coldstorage:beingRetrieved = 1";

    public static final String COLD_STORAGE_CONTENT_AVAILABLE_EVENT_NAME = "ColdStorageContentBecomeAvailable";

    /**
     * Moves the main content associated with the document of the given {@link DocumentRef} to a cold storage.
     * 
     * @return the updated document model if the move succeeds
     * @throws NuxeoException if there is no main content associated with the given document, or if the main content is
     *             already in the cold storage
     */
    public static DocumentModel moveContentToColdStorage(CoreSession session, DocumentRef documentRef) {
        DocumentModel documentModel = session.getDocument(documentRef);
        log.debug("Move to cold storage the main content of document: {}", documentModel);

        Serializable mainContent = documentModel.getPropertyValue(FILE_CONTENT_PROPERTY);
        if (mainContent == null) {
            throw new NuxeoException(String.format("There is no main content for document: %s.", documentModel),
                    SC_NOT_FOUND);
        }

        if (documentModel.hasFacet(COLD_STORAGE)
                && documentModel.getPropertyValue(COLD_STORAGE_CONTENT_PROPERTY) != null) {
            throw new NuxeoException(
                    String.format("The main content for document: %s is already in cold storage.", documentModel),
                    SC_CONFLICT);
        }

        documentModel.addFacet(COLD_STORAGE);
        documentModel.setPropertyValue(COLD_STORAGE_CONTENT_PROPERTY, mainContent);
        Blob thumbnail = Framework.getService(ThumbnailService.class).getThumbnail(documentModel, session);
        documentModel.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) thumbnail);
        return session.saveDocument(documentModel);
    }

    /**
     * Retrieves the cold storage content associated with the document of the given {@link DocumentRef}.
     * 
     * @param session the core session
     * @param documentRef the document reference
     * @param numberOfDaysOfAvailability number of days that you want your cold storage content to be accessible after
     *            restoring
     * @apiNote This method will initiate a request restore, calling the {@link Blob#getStream()} during this process
     *          doesn't means you will get the blob content immediately.
     * @return the updated document model if the retrieve succeeds
     * @throws NuxeoException if there is no cold storage content associated with the given document, or if it is being
     *             retrieved
     */
    public static DocumentModel retrieveContentFromColdStorage(CoreSession session, DocumentRef documentRef,
            int numberOfDaysOfAvailability) {
        DocumentModel documentModel = session.getDocument(documentRef);
        log.debug("Retrieve from cold storage the content of document: {} for: {} days", () -> documentModel,
                () -> numberOfDaysOfAvailability);

        if (!documentModel.hasFacet(COLD_STORAGE)
                || documentModel.getPropertyValue(COLD_STORAGE_CONTENT_PROPERTY) == null) {
            throw new NuxeoException(String.format("No cold storage content defined for document: %s.", documentModel),
                    SC_NOT_FOUND);
        }

        Serializable beingRetrieved = documentModel.getPropertyValue(COLD_STORAGE_BEING_RETRIEVED_PROPERTY);
        if (Boolean.TRUE.equals(beingRetrieved)) {
            throw new NuxeoException(
                    String.format("The cold storage content associated with the document: %s is being retrieved.",
                            documentModel),
                    SC_CONFLICT);
        }

        documentModel.setPropertyValue(COLD_STORAGE_BEING_RETRIEVED_PROPERTY, true);
        return session.saveDocument(documentModel);
    }

    /**
     * Checks if the retrieved cold storage content are available for downloading.
     * 
     * @implSpec: This method will query all documents with a cold storage content which are being retrieved
     *            {@value COLD_STORAGE_BEING_RETRIEVED_PROPERTY},
     *            {@link #retrieveContentFromColdStorage(CoreSession, DocumentRef, int)} and it checks if it is
     *            available for downloading. In which case then it will fire an event with name
     *            {@value COLD_STORAGE_CONTENT_AVAILABLE_EVENT_NAME}
     */
    // FIXME NXP-28429 will be responsible of setting "coldstorage:beingRetrieved" -> false once the email
    // is sent to avoid inconsistency
    public static ColdStorageContentStatus checkAvailabilityOfColdStorageContent(CoreSession coreSession) {
        log.debug("Start checking the available cold storage content which are being retrieved on the repository: {}",
                coreSession::getRepositoryName);

        DocumentModelList documents = coreSession.query(GET_DOCUMENTS_TO_CHECK_QUERY);
        int total = documents.size();
        EventService eventService = Framework.getService(EventService.class);
        List<Event> events = documents.stream() //
                                      .filter(ColdStorageHelper::isColdStorageContentAvailable)
                                      .map(doc -> {
                                          DocumentEventContext ctx = new DocumentEventContext(coreSession,
                                                  coreSession.getPrincipal(), doc);
                                          Event event = ctx.newEvent(COLD_STORAGE_CONTENT_AVAILABLE_EVENT_NAME);
                                          eventService.fireEvent(event);
                                          return event;
                                      })
                                      .collect(Collectors.toList());
        int available = events.size();
        log.debug("End checking the available cold storage content on the repository: {}, found: {}/{}",
                coreSession.getRepositoryName(), available, total);

        return new ColdStorageContentStatus(coreSession.getRepositoryName(), total, available);
    }

    /**
     * Checks if the retrieved cold storage content associated with the given document is available for downloading.
     * {@link #retrieveContentFromColdStorage(CoreSession, DocumentRef, int)}
     * {@link #checkAvailabilityOfColdStorageContent(CoreSession)}
     */
    public static boolean isColdStorageContentAvailable(DocumentModel documentModel) {
        // FIXME to be reworked depending on how we will check the availability
        return documentModel.getPropertyValue(COLD_STORAGE_CONTENT_PROPERTY) != null
                && documentModel.hasFacet(COLD_STORAGE);
    }

    /**
     * Gives status about the cold storage content being retrieved or are available for a given repository.
     */
    public static class ColdStorageContentStatus {

        protected final String repositoryName;

        protected final int totalBeingRetrieved;

        protected final int totalContentAvailable;

        public ColdStorageContentStatus(final String repositoryName, final int totalBeingRetrieved,
                final int totalContentAvailable) {
            this.repositoryName = repositoryName;
            this.totalBeingRetrieved = totalBeingRetrieved;
            this.totalContentAvailable = totalContentAvailable;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public int getTotalBeingRetrieved() {
            return totalBeingRetrieved;
        }

        public int getTotalContentAvailable() {
            return totalContentAvailable;
        }
    }

    private ColdStorageHelper() {
        // no instance allowed
    }
}
