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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected static Log log = LogFactory.getLog(AbstractLazyCachableRenditionProvider.class);

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Asking \"%s\" rendition lazy rendering for document %s (id=%s).",
                    definition.getName(), doc.getPathAsString(), doc.getId()));
        }

        // Build the rendition key and get the current source document modification date
        String key = buildRenditionKey(doc, definition);
        String sourceDocumentModificationDate = getSourceDocumentModificationDate(doc, definition);

        // If rendition is not already in progress schedule it
        List<Blob> blobs = null;
        TransientStore ts = getTransientStore();
        if (!ts.exists(key)) {
            blobs = handleNewRendition(key, doc, definition, sourceDocumentModificationDate);
        } else {
            String storedSourceDocumentModificationDate = (String) ts.getParameter(key,
                    SOURCE_DOCUMENT_MODIFICATION_DATE_KEY);
            blobs = ts.getBlobs(key);
            if (ts.isCompleted(key)) {
                handleCompletedRendition(key, doc, definition, sourceDocumentModificationDate,
                        storedSourceDocumentModificationDate, blobs);
            } else {
                handleIncompleteRendition(key, doc, definition, sourceDocumentModificationDate,
                        storedSourceDocumentModificationDate);
            }
        }

        if (log.isDebugEnabled()) {
            String blobInfo = null;
            if (blobs != null) {
                blobInfo = blobs.stream()
                                .map(blob -> String.format("{filename=%s, MIME type=%s}", blob.getFilename(),
                                        blob.getMimeType()))
                                .collect(Collectors.joining(",", "[", "]"));
            }
            log.debug(String.format("Returning blobs: %s.", blobInfo));
        }
        return blobs;
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
        if (log.isDebugEnabled()) {
            log.debug(String.format("Built rendition key for document %s (id=%s): %s.", doc.getPathAsString(),
                    doc.getId(), key));
        }
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
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
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
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "No entry found for key %s in the %s transient store, scheduling rendition work with id %s and storing an empty blob for now.",
                    key, CACHE_NAME, work.getId()));
        }
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
            String sourceDocumentModificationDate, String storedSourceDocumentModificationDate, List<Blob> blobs) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Completed entry found for key %s in the %s transient store.", key, CACHE_NAME));
        }

        // No or more than one blob
        if (blobs == null || blobs.size() != 1) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "No (or more than one) rendition blob for key %s, releasing entry from the transient store.",
                        key));
            }
            getTransientStore().release(key);
            return;
        }

        // Blob in error
        Blob blob = blobs.get(0);
        String mimeType = blob.getMimeType();
        if (mimeType != null && mimeType.contains(LazyRendition.ERROR_MARKER)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Rendition blob is in error for key %s.", key));
            }
            // Check if rendition is up-to-date
            if (Objects.equals(storedSourceDocumentModificationDate, sourceDocumentModificationDate)) {
                log.debug("Removing entry from the transient store.");
                getTransientStore().remove(key);
                return;
            }
            Work work = getRenditionWork(key, doc, definition);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Source document modification date %s is different from the stored one %s, scheduling rendition work with id %s and returning an error/stale rendition.",
                        sourceDocumentModificationDate, storedSourceDocumentModificationDate, work.getId()));
            }
            if (sourceDocumentModificationDate != null) {
                getTransientStore().putParameter(key, SOURCE_DOCUMENT_MODIFICATION_DATE_KEY,
                        sourceDocumentModificationDate);
            }
            Framework.getService(WorkManager.class).schedule(work);
            blob.setMimeType(blob.getMimeType() + ";" + LazyRendition.STALE_MARKER);
            return;
        }

        // Check if rendition is up-to-date
        if (Objects.equals(storedSourceDocumentModificationDate, sourceDocumentModificationDate)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Rendition blob is up-to-date for key %s, returning it and releasing entry from the transient store.",
                        key));
            }
            getTransientStore().release(key);
            return;
        }

        // Stale rendition
        Work work = getRenditionWork(key, doc, definition);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Source document modification date %s is different from the stored one %s, scheduling rendition work with id %s and returning a stale rendition.",
                    sourceDocumentModificationDate, storedSourceDocumentModificationDate, work.getId()));
        }
        if (sourceDocumentModificationDate != null) {
            getTransientStore().putParameter(key, SOURCE_DOCUMENT_MODIFICATION_DATE_KEY,
                    sourceDocumentModificationDate);
        }
        Framework.getService(WorkManager.class).schedule(work);
        blob.setMimeType(blob.getMimeType() + ";" + LazyRendition.STALE_MARKER);
    }

    protected void handleIncompleteRendition(String key, DocumentModel doc, RenditionDefinition definition,
            String sourceDocumentModificationDate, String storedSourceDocumentModificationDate) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Incomplete entry found for key %s in the %s transient store.", key, CACHE_NAME));
        }
        WorkManager workManager = Framework.getService(WorkManager.class);
        Work work = getRenditionWork(key, doc, definition);
        String workId = work.getId();
        boolean scheduleWork = false;
        if (Objects.equals(storedSourceDocumentModificationDate, sourceDocumentModificationDate)) {
            Work.State workState = workManager.getWorkState(workId);
            if (workState == null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Found no existing work with id %s.", workId));
                }
                scheduleWork = true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Found an existing work with id %s in sate %s.", workId, workState));
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Source document modification date %s is different from the stored one %s.",
                        sourceDocumentModificationDate, storedSourceDocumentModificationDate));
            }
            if (sourceDocumentModificationDate != null) {
                getTransientStore().putParameter(key, SOURCE_DOCUMENT_MODIFICATION_DATE_KEY,
                        sourceDocumentModificationDate);
            }
            scheduleWork = true;
        }
        if (scheduleWork) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Scheduling rendition work with id %s.", workId));
            }
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
     * @return
     */
    protected abstract Work getRenditionWork(final String key, final DocumentModel doc, final RenditionDefinition def);

}
