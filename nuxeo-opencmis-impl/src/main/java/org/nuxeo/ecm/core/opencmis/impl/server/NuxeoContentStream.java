/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterators;
import org.apache.chemistry.opencmis.commons.data.CacheHeaderContentStream;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentLengthContentStream;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.LastModifiedContentStream;
import org.apache.chemistry.opencmis.commons.data.RedirectingContentStream;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.input.ProxyInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.runtime.api.Framework;

/**
 * Nuxeo implementation of a CMIS {@link ContentStream}, backed by a {@link Blob}.
 */
public class NuxeoContentStream
        implements CacheHeaderContentStream, LastModifiedContentStream, ContentLengthContentStream {

    public static final String CONTENT_MD5_DIGEST_ALGORITHM = "contentMD5";

    public static final String CONTENT_MD5_HEADER_NAME = "Content-MD5";

    public static final String WANT_DIGEST_HEADER_NAME = "Want-Digest";

    public static final String DIGEST_HEADER_NAME = "Digest";

    // for tests
    public static long LAST_MODIFIED;

    protected final Blob blob;

    protected final GregorianCalendar lastModified;

    protected final InputStream stream;

    private NuxeoContentStream(Blob blob, GregorianCalendar lastModified, boolean isHeadRequest) {
        this.blob = blob;
        this.lastModified = lastModified;
        // The callers of getStream() often just want to know if the stream is null or not.
        // (Callers are ObjectService.GetContentStream / AbstractServiceCall.sendContentStreamHeaders)
        // Also in case we end up redirecting, we don't want to get the stream (which is possibly costly) to just have
        // it closed immediately. So we wrap in a lazy implementation
        if (isHeadRequest) {
            stream = new NullInputStream(0);
        } else {
            stream = new LazyInputStream(this::getActualStream);
        }
    }

    public static NuxeoContentStream create(DocumentModel doc, String xpath, Blob blob, String reason,
            Map<String, Serializable> extendedInfos, GregorianCalendar lastModified, HttpServletRequest request) {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        URI uri;
        try {
            uri = blobManager.getURI(blob, UsageHint.DOWNLOAD, request);
        } catch (IOException e) {
            throw new CmisRuntimeException("Failed to get download URI", e);
        }
        if (uri != null) {
            extendedInfos = new HashMap<>(extendedInfos == null ? Collections.emptyMap() : extendedInfos);
            extendedInfos.put("redirect", uri.toString());
        }
        boolean isHeadRequest = isHeadRequest(request);
        if (!isHeadRequest) {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.logDownload(doc, xpath, blob.getFilename(), reason, extendedInfos);
        }
        if (uri == null) {
            return new NuxeoContentStream(blob, lastModified, isHeadRequest);
        } else {
            return new NuxeoRedirectingContentStream(blob, lastModified, isHeadRequest, uri.toString());
        }
    }

    public static boolean isHeadRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        if (request instanceof HttpServletRequestWrapper) {
            request = (HttpServletRequest) ((HttpServletRequestWrapper) request).getRequest();
        }
        return request.getMethod().equals("HEAD");
    }

    public static boolean hasWantDigestRequestHeader(HttpServletRequest request, String digestAlgorithm) {
        if (request == null || digestAlgorithm == null) {
            return false;
        }
        Enumeration<String> values = request.getHeaders(WANT_DIGEST_HEADER_NAME);
        if (values == null) {
            return false;
        }
        Iterator<String> it = Iterators.forEnumeration(values);
        while (it.hasNext()) {
            String value = it.next();
            int semicolon = value.indexOf(';');
            if (semicolon >= 0) {
                value = value.substring(0, semicolon);
            }
            if (value.equalsIgnoreCase(digestAlgorithm)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getLength() {
        return blob.getLength();
    }

    @Override
    public BigInteger getBigLength() {
        return BigInteger.valueOf(blob.getLength());
    }

    @Override
    public String getMimeType() {
        return blob.getMimeType();
    }

    @Override
    public String getFileName() {
        return blob.getFilename();
    }

    @Override
    public InputStream getStream() {
        return stream;
    }

    protected InputStream getActualStream() {
        try {
            return blob.getStream();
        } catch (IOException e) {
            throw new CmisRuntimeException("Failed to get stream", e);
        }
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        return null;
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> extensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCacheControl() {
        return null;
    }

    @Override
    public String getETag() {
        return blob.getDigest();
    }

    @Override
    public GregorianCalendar getExpires() {
        return null;
    }

    @Override
    public GregorianCalendar getLastModified() {
        LAST_MODIFIED = lastModified == null ? 0 : lastModified.getTimeInMillis();
        return lastModified;
    }

    /**
     * An {@link InputStream} that fetches the actual stream from a {@link Supplier} on first use.
     *
     * @since 7.10
     */
    public static class LazyInputStream extends ProxyInputStream {

        protected Supplier<InputStream> supplier;

        public LazyInputStream(Supplier<InputStream> supplier) {
            super(null);
            this.supplier = supplier;
        }

        @Override
        protected void beforeRead(int n) {
            if (in == null) {
                in = supplier.get();
                supplier = null;
            }
        }

        @Override
        public void close() throws IOException {
            if (in == null) {
                in = new ClosedInputStream();
                supplier = null;
                return;
            }
            super.close();
        }

        @Override
        public long skip(long ln) throws IOException {
            beforeRead(0);
            return super.skip(ln);
        }

        @Override
        public int available() throws IOException {
            beforeRead(0);
            return super.available();
        }

        @Override
        public void mark(int readlimit) {
            beforeRead(0);
            super.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            beforeRead(0);
            super.reset();
        }

        @Override
        public boolean markSupported() {
            beforeRead(0);
            return super.markSupported();
        }
    }

    /**
     * A {@link NuxeoContentStream} that will generate a redirect.
     *
     * @since 7.10
     */
    public static class NuxeoRedirectingContentStream extends NuxeoContentStream implements RedirectingContentStream {

        protected final String location;

        private NuxeoRedirectingContentStream(Blob blob, GregorianCalendar lastModified, boolean isHeadRequest,
                String location) {
            super(blob, lastModified, isHeadRequest);
            this.location = location;
        }

        @Override
        public int getStatus() {
            // use same redirect code as HttpServletResponse.sendRedirect
            return HttpServletResponse.SC_FOUND;
        }

        @Override
        public String getLocation() {
            return location;
        }
    }

}
