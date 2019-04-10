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
 *     Nuxeo
 */

package org.nuxeo.ecm.blob;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.CachingBinaryManager;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public abstract class AbstractCloudBinaryManager extends CachingBinaryManager implements BlobProvider {

    private static final Log log = LogFactory.getLog(AbstractCloudBinaryManager.class);

    /**
     * Gets the prefix used for configuration using system properties.
     */
    protected abstract String getSystemPropertyPrefix();

    protected abstract FileStorage getFileStorage();

    protected abstract BinaryGarbageCollector instantiateGarbageCollector();

    @Override
    public abstract void removeBinaries(Collection<String> digests);

    /**
     * Configure Cloud client using properties
     */
    protected abstract void setupCloudClient() throws IOException;

    protected Map<String, String> properties;

    protected boolean directDownload = false;

    protected int directDownloadExpire;

    public static final String CACHE_SIZE_PROPERTY = "cachesize";

    public static final String CACHE_COUNT_PROPERTY = "cachecount";

    public static final String CACHE_MIN_AGE_PROPERTY = "cacheminage";

    public static final String DEFAULT_CACHE_SIZE = "100 mb";

    public static final String DEFAULT_CACHE_COUNT = "10000";

    public static final String DEFAULT_CACHE_MIN_AGE = "3600"; // 1h

    public static final String DIRECTDOWNLOAD_PROPERTY = "directdownload";

    public static final String DEFAULT_DIRECTDOWNLOAD = "false";

    public static final String DIRECTDOWNLOAD_EXPIRE_PROPERTY = "directdownload.expire";

    public static final int DEFAULT_DIRECTDOWNLOAD_EXPIRE = 60 * 60; // 1h

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        this.properties = properties;

        // Enable direct download from the remote binary store
        directDownload = Boolean.parseBoolean(getProperty(DIRECTDOWNLOAD_PROPERTY, DEFAULT_DIRECTDOWNLOAD));
        directDownloadExpire = getIntProperty(DIRECTDOWNLOAD_EXPIRE_PROPERTY);
        if (directDownloadExpire < 0) {
            directDownloadExpire = DEFAULT_DIRECTDOWNLOAD_EXPIRE;
        }

        // Setup remote client
        setupCloudClient();

        // Set cache size
        String cacheSizeStr = getProperty(CACHE_SIZE_PROPERTY, DEFAULT_CACHE_SIZE);
        String cacheCountStr = getProperty(CACHE_COUNT_PROPERTY, DEFAULT_CACHE_COUNT);
        String minAgeStr = getProperty(CACHE_MIN_AGE_PROPERTY, DEFAULT_CACHE_MIN_AGE);
        initializeCache(cacheSizeStr, cacheCountStr, minAgeStr, getFileStorage());

        garbageCollector = instantiateGarbageCollector();
    }

    @Override
    public BinaryManager getBinaryManager() {
        return this;
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).readBlob(blobInfo);
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).writeBlob(blob);
    }

    @Override
    public boolean performsExternalAccessControl(BlobInfo blobInfo) {
        return new BinaryBlobProvider(this).performsExternalAccessControl(blobInfo);
    }

    @Override
    public boolean supportsUserUpdate() {
        return supportsUserUpdateDefaultTrue();
    }

    protected boolean supportsUserUpdateDefaultTrue() {
        return !Boolean.parseBoolean(properties.get(PREVENT_USER_UPDATE));
    }

    @Override
    public URI getURI(ManagedBlob blob, BlobManager.UsageHint hint, HttpServletRequest servletRequest)
            throws IOException {
        if (hint != BlobManager.UsageHint.DOWNLOAD || !isDirectDownload()) {
            return null;
        }
        String digest = blob.getKey();
        // strip prefix
        int colon = digest.indexOf(':');
        if (colon >= 0) {
            digest = digest.substring(colon + 1);
        }

        return getRemoteUri(digest, blob, servletRequest);
    }

    protected boolean isDirectDownload() {
        return directDownload;
    }

    protected URI getRemoteUri(String digest, ManagedBlob blob, HttpServletRequest servletRequest) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    protected String getProperty(String propertyName, String defaultValue) {
        String propValue = properties.get(propertyName);
        if (isNotBlank(propValue)) {
            return propValue;
        }
        propValue = Framework.getProperty(getSystemPropertyName(propertyName));
        if (isNotBlank(propValue)) {
            return propValue;
        }
        return defaultValue;
    }

    /**
     * Gets an integer property, or -1 if undefined.
     */
    protected int getIntProperty(String key) {
        String s = getProperty(key);
        int value = -1;
        if (!isBlank(s)) {
            try {
                value = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                log.error("Cannot parse " + key + ": " + s);
            }
        }
        return value;
    }

    /**
     * Gets a boolean property.
     *
     * @since 10.3
     */
    protected boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

    public String getSystemPropertyName(String propertyName) {
        return getSystemPropertyPrefix() + "." + propertyName;
    }

    protected String getContentTypeHeader(Blob blob) {
        String contentType = blob.getMimeType();
        String encoding = blob.getEncoding();
        if (contentType != null && !StringUtils.isBlank(encoding)) {
            int i = contentType.indexOf(';');
            if (i >= 0) {
                contentType = contentType.substring(0, i);
            }
            contentType += "; charset=" + encoding;
        }
        return contentType;
    }

    protected String getContentDispositionHeader(Blob blob, HttpServletRequest servletRequest) {
        if (servletRequest == null) {
            return RFC2231.encodeContentDisposition(blob.getFilename(), false, null);
        } else {
            return DownloadHelper.getRFC2231ContentDisposition(servletRequest, blob.getFilename());
        }
    }
}
