/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.lazy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.DefaultAutomationRenditionProvider;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of an asynchronous {@link RenditionProvider}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractLazyCachableRenditionProvider extends DefaultAutomationRenditionProvider {

    public static final String SOURCE_DOCUMENT_MODIFICATION_DATE_KEY = "sourceDocumentModificationDate";

    public static final String CACHE_NAME = "LazyRenditionCache";

    protected static Logger log = LogManager.getLogger(AbstractLazyCachableRenditionProvider.class);

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        log.debug("Asking \"{}\" rendition lazy rendering for document {} (id={}).", definition::getName,
                doc::getPathAsString, doc::getId);

        // Build the rendition key and get the current source document modification date
        String key = buildRenditionKey(doc, definition);
        String sourceDocumentModificationDate = getSourceDocumentModificationDate(doc, definition);

        // If rendition is not already in progress schedule it
        List<Blob> blobs;
        TransientStore ts = getTransientStore();
        if (!ts.exists(key)) {
            blobs = handleNewRendition(key, doc, definition, sourceDocumentModificationDate);
        } else {
            String tsSourceDocumentModificationDate = (String) ts.getParameter(key,
                    SOURCE_DOCUMENT_MODIFICATION_DATE_KEY);
            blobs = ts.getBlobs(key);
            if (ts.isCompleted(key)) {
                handleCompletedRendition(key, doc, definition, sourceDocumentModificationDate,
                        tsSourceDocumentModificationDate, blobs);
            } else {
                handleIncompleteRendition(key, doc, definition, sourceDocumentModificationDate,
                        tsSourceDocumentModificationDate);
            }
        }
        final List<Blob> finalBlobs = blobs;
        log.debug("Returning blobs: {}.", () -> getBlobInfo(finalBlobs));
        return blobs;
    }

    /**
     * @since 11.1
     */
    protected String getBlobInfo(List<Blob> blobs) {
        if (blobs == null) {
            return null;
        }
        return blobs.stream()
                    .map(blob -> String.format("{filename=%s, MIME type=%s}", blob.getFilename(), blob.getMimeType()))
                    .collect(Collectors.joining(",", "[", "]"));
    }

    public String buildRenditionKey(DocumentModel doc, RenditionDefinition def) {
        StringBuilder sb = new StringBuilder(doc.getId());
        sb.append("::");
        String variant = getVariant(doc, def);
        if (variant != null) {
            sb.append(variant);
            sb.append("::");
        }
        sb.append(def.getName());

        String key = getDigest(sb.toString());
        log.debug("Built rendition key for document {} (id={}): {}.", doc::getPathAsString, doc::getId, () -> key);
        return key;
    }

    public String getSourceDocumentModificationDate(DocumentModel doc, RenditionDefinition definition) {
        String modificationDatePropertyName = definition.getSourceDocumentModificationDatePropertyName();
        Calendar modificationDate = (Calendar) doc.getPropertyValue(modificationDatePropertyName);
        if (modificationDate == null) {
            return null;
        }
        long millis = modificationDate.getTimeInMillis();
        // the date may have been rounded by the storage layer, normalize it to the second
        millis -= millis % 1000;
        return String.valueOf(millis);
    }

    protected String getDigest(String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return key;
        }
        byte[] buf = digest.digest(key.getBytes());
        return toHexString(buf);
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    protected String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(2 * data.length);
        for (byte b : data) {
            sb.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            sb.append(HEX_DIGITS[0x0F & b]);
        }
        return sb.toString();
    }

    protected TransientStore getTransientStore() {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(CACHE_NAME);
        if (ts == null) {
            throw new NuxeoException("Unable to find Transient Store  " + CACHE_NAME);
        }
        return ts;
    }

    protected List<Blob> handleNewRendition(String key, DocumentModel doc, RenditionDefinition definition,
            String sourceDocumentModificationDate) {
        Work work = getRenditionWork(key, doc, definition);
        log.debug(
                "No entry found for key {} in the {} transient store, scheduling rendition work with id {} and storing"
                        + " an empty blob for now.",
                () -> key, () -> CACHE_NAME, work::getId);
        if (sourceDocumentModificationDate != null) {
            getTransientStore().putParameter(key, SOURCE_DOCUMENT_MODIFICATION_DATE_KEY,
                    sourceDocumentModificationDate);
        }
        StringBlob emptyBlob = new StringBlob("");
        emptyBlob.setFilename(LazyRendition.IN_PROGRESS_MARKER);
        emptyBlob.setMimeType("text/plain;" + LazyRendition.EMPTY_MARKER);
        getTransientStore().putBlobs(key, Collections.singletonList(emptyBlob));
        Framework.getService(WorkManager.class).schedule(work);
        return Collections.singletonList(emptyBlob);
    }

    protected void handleCompletedRendition(String key, DocumentModel doc, RenditionDefinition definition,
            String sourceDocumentModificationDate, String tsSourceDocumentModificationDate, List<Blob> blobs) {
        log.debug("Completed entry found for key {} in the {} transient store.", key, CACHE_NAME);

        // No or more than one blob
        if (blobs == null || blobs.size() != 1) {
            log.debug("No (or more than one) rendition blob for key {}, releasing entry from the transient store.",
                    key);
            getTransientStore().release(key);
            return;
        }

        // Blob in error
        Blob blob = blobs.get(0);
        String mimeType = blob.getMimeType();
        if (mimeType != null && mimeType.contains(LazyRendition.ERROR_MARKER)) {
            log.debug("Rendition blob is in error for key {}.", key);
            // Check if rendition is up-to-date
            if (Objects.equals(tsSourceDocumentModificationDate, sourceDocumentModificationDate)) {
                log.debug("Removing entry from the transient store.");
                getTransientStore().remove(key);
                return;
            }
            Work work = getRenditionWork(key, doc, definition);
            log.debug(
                    "Source document modification date {} is different from the corresponding transient store parameter"
                            + " {}, scheduling rendition work with id {} and returning an error/stale rendition.",
                    () -> sourceDocumentModificationDate, () -> tsSourceDocumentModificationDate, work::getId);
            Framework.getService(WorkManager.class).schedule(work);
            blob.setMimeType(blob.getMimeType() + ";" + LazyRendition.STALE_MARKER);
            return;
        }

        // Check if rendition is up-to-date
        if (Objects.equals(tsSourceDocumentModificationDate, sourceDocumentModificationDate)) {
            log.debug("Rendition blob is up-to-date for key {}, returning it and releasing entry from the transient"
                    + " store.", key);
            getTransientStore().release(key);
            return;
        }

        // Stale rendition
        Work work = getRenditionWork(key, doc, definition);
        log.debug(
                "Source document modification date {} is different from the corresponding transient store parameter {},"
                        + " scheduling rendition work with id {} and returning a stale rendition.",
                () -> sourceDocumentModificationDate, () -> tsSourceDocumentModificationDate, work::getId);
        Framework.getService(WorkManager.class).schedule(work);
        blob.setMimeType(blob.getMimeType() + ";" + LazyRendition.STALE_MARKER);
    }

    protected void handleIncompleteRendition(String key, DocumentModel doc, RenditionDefinition definition,
            String sourceDocumentModificationDate, String tsSourceDocumentModificationDate) {
        log.debug("Incomplete entry found for key {} in the {} transient store.", key, CACHE_NAME);
        WorkManager workManager = Framework.getService(WorkManager.class);
        Work work = getRenditionWork(key, doc, definition);
        String workId = work.getId();
        boolean scheduleWork = false;
        if (Objects.equals(tsSourceDocumentModificationDate, sourceDocumentModificationDate)) {
            Work.State workState = workManager.getWorkState(workId);
            if (workState == null) {
                log.debug("Found no existing work with id {}.", workId);
                scheduleWork = true;
            } else {
                log.debug("Found an existing work with id {} in sate {}.", workId, workState);
            }
        } else {
            log.debug("Source document modification date {} is different from the corresponding transient store"
                    + " parameter {}.", sourceDocumentModificationDate, tsSourceDocumentModificationDate);
            scheduleWork = true;
        }
        if (scheduleWork) {
            log.debug("Scheduling rendition work with id {}.", workId);
            workManager.schedule(work);
        }
    }

    /**
     * Return the {@link Work} that will compute the {@link Rendition}. {@link AbstractRenditionBuilderWork} can be used
     * as a base class
     *
     * @param key the key used to rendition
     * @param doc the target {@link DocumentModel}
     * @param def the {@link RenditionDefinition}
     */
    protected abstract Work getRenditionWork(final String key, final DocumentModel doc, final RenditionDefinition def);

}
