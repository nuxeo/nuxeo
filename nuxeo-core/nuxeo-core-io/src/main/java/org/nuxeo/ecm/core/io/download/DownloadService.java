/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Estelle Giuy <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.download;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;

/**
 * This service allows the download of blobs to a HTTP response.
 *
 * @since 7.3
 */
public interface DownloadService {

    String EVENT_NAME = "download";

    String NXFILE = "nxfile";

    String NXDOWNLOADINFO = "nxdownloadinfo";

    /**
     * @since 9.3
     */
    String NXBLOBSTATUS = "nxblobstatus";

    String NXBIGBLOB = "nxbigblob";

    /** @deprecated since 9.1, use nxbigblob instead */
    @Deprecated
    String NXBIGZIPFILE = "nxbigzipfile";

    /** @deprecated since 7.4, use nxfile instead */
    @Deprecated
    String NXBIGFILE = "nxbigfile";

    String BLOBHOLDER_PREFIX = "blobholder:";

    String BLOBHOLDER_0 = "blobholder:0";

    /**
     * The transient store parameter name for storing an error if any. Stored entry must
     */
    String TRANSIENT_STORE_PARAM_ERROR = "error";

    String TRANSIENT_STORE_PARAM_PROGRESS = "progress";

    String TRANSIENT_STORE_STORE_NAME = "download";

    public static class ByteRange {

        private final long start;

        private final long end;

        public ByteRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public long getLength() {
            return end - start + 1;
        }
    }

    /**
     * Stores the blobs for later download.
     *
     * @param the list of blobs to store
     * @return the store key used for retrieving the blobs (@see {@link DownloadService#getDownloadUrl(String)}
     * @since 9.1
     */
    String storeBlobs(List<Blob> blobs);

    /**
     * Gets the URL to use to download the blob at the given xpath in the given document.
     * <p>
     * The URL is relative to the Nuxeo Web Application context.
     * <p>
     * Returns something like {@code nxfile/reponame/docuuid/blobholder:0/foo.jpg?changeToken=5-1}
     *
     * @param doc the document
     * @param xpath the blob's xpath or blobholder index, or {@code null} for default
     * @param filename the blob's filename, or {@code null} for default
     * @return the download URL with changeToken as query param for optimized http caching
     */
    String getDownloadUrl(DocumentModel doc, String xpath, String filename);


    /**
     * Gets the URL to use to download the blob at the given xpath in the given document.
     * <p>
     * The URL is relative to the Nuxeo Web Application context.
     * <p>
     * Returns something like {@code nxfile/reponame/docuuid/blobholder:0/foo.jpg?changeToken=5-1}
     *
     * @param doc the document
     * @param xpath the blob's xpath or blobholder index, or {@code null} for default
     * @param filename the blob's filename, or {@code null} for default
     * @param changeToken the doc changeToken which will be appended as a query parameter for optimized http caching.
     * @return the download URL
     * @since 10.3
     */
    String getDownloadUrl(String repositoryName, String docId, String xpath, String filename, String changeToken);

    /**
     * Gets the URL to use to download the blob at the given xpath in the given document.
     * <p>
     * The URL is relative to the Nuxeo Web Application context.
     * <p>
     * Returns something like {@code nxfile/reponame/docuuid/blobholder:0/foo.jpg}
     *
     * @param repositoryName the document repository
     * @param docId the document id
     * @param xpath the blob's xpath or blobholder index, or {@code null} for default
     * @param filename the blob's filename, or {@code null} for default
     * @return the download URL
     */
    String getDownloadUrl(String repositoryName, String docId, String xpath, String filename);

    /**
     * Gets the URL to use to download the blobs identified by a storage key.
     * <p>
     * The URL is relative to the Nuxeo Web Application context.
     * <p>
     * Returns something like {@code nxbigblob/key}
     *
     * @param key The key of stored blobs to download
     * @return the download URL
     * @since 9.1
     */
    String getDownloadUrl(String storeKey);

    /**
     * Finds a document's blob given the download URL returned by {@link #getDownloadUrl}.
     * <p>
     * The permissions are check whether the user can download the blob or not.
     *
     * @param downloadURL the URL to use to download the blob
     * @return the blob, or {@code null} if not found or if the user has no permission to download it
     * @since 9.1
     */
    Blob resolveBlobFromDownloadUrl(String downloadURL);

    /**
     * Handles the download of a document.
     *
     * @param req the request
     * @param resp the response
     * @param baseUrl the request baseUrl
     * @param path the request path, without the context
     * @since 9.1
     */
    void handleDownload(HttpServletRequest req, HttpServletResponse resp, String baseUrl, String path)
            throws IOException;

    /**
     * Triggers a {@link AsyncBlob} download which gives information about an asynchronous blob.
     *
     * @param storeKey the stored blobs key
     * @param reason the download reason
     * @since 9.3
     */
    void downloadBlobStatus(HttpServletRequest request, HttpServletResponse response, String storeKey, String reason)
            throws IOException;

    /**
     * Triggers a blobs download. Once the temporary blobs are transfered from the store, they are automatically
     * deleted. The returned HTTP Status Code is 200 if the blob is ready or 202 if it is still being processed.
     *
     * @param storeKey the stored blobs key
     * @param reason the download reason
     * @since 9.1
     */
    void downloadBlob(HttpServletRequest request, HttpServletResponse response, String storeKey, String reason) throws IOException;

    /**
     * Triggers a blob download.
     *
     * @param doc the document, if available
     * @param xpath the blob's xpath or blobholder index, if available
     * @param blob the blob, if already fetched
     * @param filename the filename to use
     * @param reason the download reason
     */
    void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason) throws IOException;

    /**
     * Triggers a blob download.
     *
     * @param doc the document, if available
     * @param xpath the blob's xpath or blobholder index, if available
     * @param blob the blob, if already fetched
     * @param filename the filename to use
     * @param reason the download reason
     * @param extendedInfos an optional map of extended informations to log
     */
    void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos) throws IOException;

    /**
     * Triggers a blob download.
     *
     * @param doc the document, if available
     * @param xpath the blob's xpath or blobholder index, if available
     * @param blob the blob, if already fetched
     * @param filename the filename to use
     * @param reason the download reason
     * @param extendedInfos an optional map of extended informations to log
     * @param inline if not null, force the inline flag for content-disposition
     */
    void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos, Boolean inline)
            throws IOException;

    /**
     * Triggers a blob download. The actual byte transfer is done through a {@link DownloadExecutor}.
     *
     * @param doc the document, if available
     * @param xpath the blob's xpath or blobholder index, if available
     * @param blob the blob, if already fetched
     * @param filename the filename to use
     * @param reason the download reason
     * @param extendedInfos an optional map of extended informations to log
     * @param inline if not null, force the inline flag for content-disposition
     * @param blobTransferer the transferer of the actual blob
     * @since 7.10
     */
    void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos, Boolean inline,
            Consumer<ByteRange> blobTransferer) throws IOException;

    /**
     * Copies the blob stream at the given byte range into the supplied {@link OutputStream}.
     *
     * @param blob the blob
     * @param byteRange the byte range
     * @param outputStreamSupplier the {@link OutputStream} supplier
     * @since 7.10
     */
    void transferBlobWithByteRange(Blob blob, ByteRange byteRange, Supplier<OutputStream> outputStreamSupplier);

    /**
     * Logs a download.
     *
     * @param doc the doc for which this download occurs, if available
     * @param blobXPath the blob's xpath or blobholder index, if available
     * @param filename the filename
     * @param reason the download reason
     * @param extendedInfos an optional map of extended informations to log
     */
    void logDownload(DocumentModel doc, String blobXPath, String filename, String reason,
            Map<String, Serializable> extendedInfos);

    /**
     * Finds a document's blob given an xpath or blobholder index
     *
     * @param doc the document
     * @param xpath the xpath or blobholder index
     * @return the blob, or {@code null} if not found
     */
    Blob resolveBlob(DocumentModel doc, String xpath);

    /**
     * Finds a document's blob.
     *
     * @param doc the document
     * @return the blob, or {@code null} if not available
     * @since 9.3
     */
    Blob resolveBlob(DocumentModel doc);

    /**
     * Checks whether the download of the blob is allowed.
     *
     * @param doc the doc for which this download occurs, if available
     * @param blobXPath the blob's xpath or blobholder index, if available
     * @param blob the blob
     * @param reason the download reason
     * @param extendedInfos an optional map of extended informations to log
     * @return {@code true} if download is allowed
     * @since 7.10
     */
    boolean checkPermission(DocumentModel doc, String xpath, Blob blob, String reason,
            Map<String, Serializable> extendedInfos);

}
