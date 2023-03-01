/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mathieu Guillaume
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.jclouds;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.blob.binary.CachingBinaryManager;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.runtime.api.Framework;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

/**
 * A Binary Manager that stores binaries in cloud blob stores using jclouds.
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream, it is also cached in a simple text file
 * if accessed before the stream.
 */
public class JCloudsBinaryManager extends CachingBinaryManager {

    private static final Log log = LogFactory.getLog(JCloudsBinaryManager.class);

    public static final String BLOBSTORE_PROVIDER_KEY = "jclouds.blobstore.provider";

    public static final String BLOBSTORE_MAP_NAME_KEY = "jclouds.blobstore.name";

    public static final String BLOBSTORE_LOCATION_KEY = "jclouds.blobstore.location";

    public static final String BLOBSTORE_ENDPOINT_KEY = "jclouds.blobstore.endpoint";

    public static final String DEFAULT_LOCATION = null;

    public static final String BLOBSTORE_IDENTITY_KEY = "jclouds.blobstore.identity";

    public static final String BLOBSTORE_SECRET_KEY = "jclouds.blobstore.secret";

    public static final String CACHE_SIZE_KEY = "jclouds.blobstore.cachesize";

    public static final String DEFAULT_CACHE_SIZE = "100 MB";

    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    protected String container;

    protected String endpoint;

    protected String storeProvider;

    protected BlobStore blobStore;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);

        // Get settings from the configuration
        storeProvider = getConfigurationProperty(BLOBSTORE_PROVIDER_KEY, properties);
        if (isBlank(storeProvider)) {
            throw new RuntimeException("Missing conf: " + BLOBSTORE_PROVIDER_KEY);
        }

        container = getConfigurationProperty(BLOBSTORE_MAP_NAME_KEY, properties);
        if (isBlank(container)) {
            throw new RuntimeException("Missing conf: " + BLOBSTORE_MAP_NAME_KEY);
        }

        endpoint = getConfigurationProperty(BLOBSTORE_ENDPOINT_KEY, properties);

        String storeLocation = getConfigurationProperty(BLOBSTORE_LOCATION_KEY, properties);
        if (isBlank(storeLocation)) {
            storeLocation = null;
        }

        String storeIdentity = getConfigurationProperty(BLOBSTORE_IDENTITY_KEY, properties);
        if (isBlank(storeIdentity)) {
            throw new RuntimeException("Missing conf: " + BLOBSTORE_IDENTITY_KEY);
        }

        String storeSecret = getConfigurationProperty(BLOBSTORE_SECRET_KEY, properties);
        if (isBlank(storeSecret)) {
            throw new RuntimeException("Missing conf: " + BLOBSTORE_SECRET_KEY);
        }

        String cacheSizeStr = getConfigurationProperty(CACHE_SIZE_KEY, properties);
        if (isBlank(cacheSizeStr)) {
            cacheSizeStr = DEFAULT_CACHE_SIZE;
        }

        String proxyHost = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST);
        String proxyPort = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT);
        final String proxyLogin = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
        final String proxyPassword = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);

        // Set up proxy
        if (isNotBlank(proxyHost)) {
            System.setProperty("https.proxyHost", proxyHost);
        }
        if (isNotBlank(proxyPort)) {
            System.setProperty("https.proxyPort", proxyPort);
        }
        if (isNotBlank(proxyLogin)) {
            System.setProperty("https.proxyUser", proxyLogin);
            System.setProperty("https.proxyPassword", proxyPassword);
            Authenticator.setDefault(new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyLogin, proxyPassword.toCharArray());
                }
            });
        }

        ContextBuilder builder = ContextBuilder.newBuilder(storeProvider).credentials(storeIdentity, storeSecret);

        if (isNotBlank(endpoint)) {
            builder.endpoint(endpoint);
        }

        BlobStoreContext context = builder.buildView(BlobStoreContext.class);

        // Try to create container if it doesn't exist
        blobStore = context.getBlobStore();
        boolean created;
        if (storeLocation == null) {
            created = blobStore.createContainerInLocation(null, container);
        } else {
            Location location = new LocationBuilder().scope(LocationScope.REGION)
                                                     .id(storeLocation)
                                                     .description(storeLocation)
                                                     .build();
            created = blobStore.createContainerInLocation(location, container);
        }
        if (created) {
            log.debug("Created container " + container);
        }

        // Create file cache
        initializeCache(cacheSizeStr, new JCloudsFileStorage());
        createGarbageCollector();
    }

    // Get a property based first on the value in the properties map, then
    // from the system properties.
    private String getConfigurationProperty(String key, Map<String, String> properties) {
        String value = properties.get(key);
        if (isBlank(value)) {
            value = Framework.getProperty(key);
        }
        return value;
    }

    protected void createGarbageCollector() {
        garbageCollector = new JCloudsBinaryGarbageCollector(this);
    }

    protected void removeBinary(String digest) {
        blobStore.removeBlob(container, digest);
    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }

    public class JCloudsFileStorage implements FileStorage {

        @Override
        public void storeFile(String digest, File file) throws IOException {
            Blob currentObject;
            try {
                currentObject = blobStore.getBlob(container, digest);
            } catch (Exception e) {
                throw new IOException("Unable to check existence of binary", e);
            }
            if (currentObject == null) {
                // no data, store the blob
                ByteSource byteSource = Files.asByteSource(file);
                Blob remoteBlob = blobStore.blobBuilder(digest)
                                           .payload(byteSource)
                                           .contentLength(byteSource.size())
                                           .contentMD5(byteSource.hash(Hashing.md5()))
                                           .build();
                try {
                    blobStore.putBlob(container, remoteBlob);
                } catch (Exception e) {
                    throw new IOException("Unable to store binary", e);
                }
                // validate storage
                // TODO only check presence and size/md5
                Blob checkBlob;
                try {
                    checkBlob = blobStore.getBlob(container, digest);
                } catch (Exception e) {
                    try {
                        // Remote blob can't be validated - remove it
                        blobStore.removeBlob(container, digest);
                    } catch (Exception e2) {
                        log.error("Possible data corruption : binary " + digest
                                + " validation failed but it could not be removed.");
                    }
                    throw new IOException("Unable to validate stored binary", e);
                }
                if (checkBlob == null || !remoteBlob.getMetadata().getContentMetadata().getContentLength().equals(
                        checkBlob.getMetadata().getContentMetadata().getContentLength())) {
                    if (checkBlob != null) {
                        // Remote blob is incomplete - remove it
                        try {
                            blobStore.removeBlob(container, digest);
                        } catch (Exception e2) {
                            log.error("Possible data corruption : binary " + digest
                                    + " validation failed but it could not be removed.");
                        }
                    }
                    throw new IOException("Upload to blob store failed");
                }
            }
        }

        @Override
        public boolean fetchFile(String digest, File tmp) {
            Blob remoteBlob;
            try {
                remoteBlob = blobStore.getBlob(container, digest);
            } catch (Exception e) {
                log.error("Could not cache binary from remote storage: " + digest, e);
                return false;
            }
            if (remoteBlob == null) {
                log.error("Unknown binary: " + digest);
                return false;
            } else {
                InputStream remoteStream = remoteBlob.getPayload().getInput();
                OutputStream localStream = null;
                try {
                    localStream = new FileOutputStream(tmp);
                    IOUtils.copy(remoteStream, localStream);
                } catch (IOException e) {
                    log.error("Unable to cache binary from remote storage: " + digest, e);
                    return false;
                } finally {
                    IOUtils.closeQuietly(remoteStream);
                    IOUtils.closeQuietly(localStream);
                }
            }
            return true;
        }
    }

    /**
     * Garbage collector for the blobstore binaries that stores the marked (in use) binaries in memory.
     */
    public static class JCloudsBinaryGarbageCollector implements BinaryGarbageCollector {

        protected final JCloudsBinaryManager binaryManager;

        protected volatile long startTime;

        protected BinaryManagerStatus status;

        protected Set<String> marked;

        public JCloudsBinaryGarbageCollector(JCloudsBinaryManager binaryManager) {
            this.binaryManager = binaryManager;
        }

        @Override
        public String getId() {
            return "jclouds/" + binaryManager.storeProvider + ":" + binaryManager.container;
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return status;
        }

        @Override
        public boolean isInProgress() {
            // volatile as this is designed to be called from another thread
            return startTime != 0;
        }

        @Override
        public void start() {
            if (startTime != 0) {
                throw new RuntimeException("Alread started");
            }
            startTime = System.currentTimeMillis();
            status = new BinaryManagerStatus();
            marked = new HashSet<>();
        }

        @Override
        public void mark(String digest) {
            marked.add(digest);
        }

        @Override
        public void stop(boolean delete) {
            if (startTime == 0) {
                throw new RuntimeException("Not started");
            }

            Set<String> unmarked = new HashSet<>();
            ListContainerOptions options = ListContainerOptions.NONE;
            for (;;) {
                PageSet<? extends StorageMetadata> metadatas = binaryManager.blobStore.list(binaryManager.container,
                        options);
                for (StorageMetadata metadata : metadatas) {
                    String digest = metadata.getName();
                    if (!isMD5(digest)) {
                        // ignore files that cannot be MD5 digests for safety
                        continue;
                    }
                    // TODO size in metadata available only in upcoming JClouds 1.9.0 (JCLOUDS-654)
                    if (marked.contains(digest)) {
                        status.numBinaries++;
                        // status.sizeBinaries += size;
                    } else {
                        status.numBinariesGC++;
                        // status.sizeBinariesGC += size;
                        // record file to delete
                        unmarked.add(digest);
                        marked.remove(digest); // optimize memory
                    }
                }
                String marker = metadatas.getNextMarker();
                if (marker == null) {
                    break;
                }
                options = ListContainerOptions.Builder.afterMarker(marker);
            }
            marked = null; // help GC

            // delete unmarked objects
            if (delete) {
                for (String digest : unmarked) {
                    binaryManager.removeBinary(digest);
                }
            }

            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
        }

        @Override
        public void reset() {
            startTime = 0;
        }
    }

}
