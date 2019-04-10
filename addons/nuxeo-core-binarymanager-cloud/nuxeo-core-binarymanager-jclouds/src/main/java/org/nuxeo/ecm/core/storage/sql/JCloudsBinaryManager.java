/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mathieu Guillaume
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

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
import org.jclouds.blobstore.BlobMap;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.storage.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerDescriptor;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.storage.binary.CachingBinaryManager;
import org.nuxeo.ecm.core.storage.binary.FileStorage;
import org.nuxeo.runtime.api.Framework;

/**
 * A Binary Manager that stores binaries in cloud blob stores using jclouds.
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream,
 * it is also cached in a simple text file if accessed before the stream.
 */
public class JCloudsBinaryManager extends CachingBinaryManager {

    private static final Log log = LogFactory.getLog(JCloudsBinaryManager.class);

    public static final String BLOBSTORE_PROVIDER_KEY = "jclouds.blobstore.provider";

    public static final String BLOBSTORE_MAP_NAME_KEY = "jclouds.blobstore.name";

    public static final String BLOBSTORE_LOCATION_KEY = "jclouds.blobstore.location";

    public static final String DEFAULT_LOCATION = null;

    public static final String BLOBSTORE_IDENTITY_KEY = "jclouds.blobstore.identity";

    public static final String BLOBSTORE_SECRET_KEY = "jclouds.blobstore.secret";

    public static final String CACHE_SIZE_KEY = "jclouds.blobstore.cachesize";

    public static final String DEFAULT_CACHE_SIZE = "100 MB";

    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    protected String storeName;

    protected String storeProvider;

    protected BlobMap storeMap;

    @Override
    public void initialize(BinaryManagerDescriptor binaryManagerDescriptor)
            throws IOException {
        super.initialize(binaryManagerDescriptor);

        // Get settings from the configuration
        storeProvider = Framework.getProperty(BLOBSTORE_PROVIDER_KEY);
        if (isBlank(storeProvider)) {
            throw new RuntimeException("Missing conf: "
                    + BLOBSTORE_PROVIDER_KEY);
        }
        storeName = Framework.getProperty(BLOBSTORE_MAP_NAME_KEY);
        if (isBlank(storeName)) {
            throw new RuntimeException("Missing conf: "
                    + BLOBSTORE_MAP_NAME_KEY);
        }

        String storeLocation = Framework.getProperty(BLOBSTORE_LOCATION_KEY);
        if (isBlank(storeLocation)) {
            storeLocation = null;
        }

        String storeIdentity = Framework.getProperty(BLOBSTORE_IDENTITY_KEY);
        if (isBlank(storeIdentity)) {
            throw new RuntimeException("Missing conf: "
                    + BLOBSTORE_IDENTITY_KEY);
        }

        String storeSecret = Framework.getProperty(BLOBSTORE_SECRET_KEY);
        if (isBlank(storeSecret)) {
            throw new RuntimeException("Missing conf: " + BLOBSTORE_SECRET_KEY);
        }

        String cacheSizeStr = Framework.getProperty(CACHE_SIZE_KEY);
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
                    return new PasswordAuthentication(proxyLogin,
                            proxyPassword.toCharArray());
                }
            });
        }

        BlobStoreContext context = ContextBuilder.newBuilder(storeProvider).credentials(
                storeIdentity, storeSecret).buildView(BlobStoreContext.class);

        // Try to create container if it doesn't exist
        BlobStore store = context.getBlobStore();
        boolean created = false;
        if (storeLocation == null) {
            created = store.createContainerInLocation(null, storeName);
        } else {
            Location location = new LocationBuilder().scope(
                    LocationScope.REGION).id(storeLocation).description(
                    storeLocation).build();
            created = store.createContainerInLocation(location, storeName);
        }
        if (created) {
            log.debug("Created container " + storeName);
        }

        storeMap = context.createBlobMap(storeName);

        // Create file cache
        initializeCache(cacheSizeStr, new JCloudsFileStorage());
        createGarbageCollector();
    }

    protected void createGarbageCollector() {
        garbageCollector = new JCloudsBinaryGarbageCollector(this);
    }

    protected void removeBinary(String digest) {
        storeMap.remove(digest);
    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }

    public class JCloudsFileStorage implements FileStorage {

        @Override
        public void storeFile(String digest, File file) throws IOException {
            Blob currentObject;
            try {
                currentObject = storeMap.get(digest);
            } catch (Exception e) {
                throw new IOException("Unable to check existence of binary", e);
            }
            if (currentObject == null) {
                // no data, store the blob
                Blob remoteBlob = storeMap.blobBuilder().name(digest).payload(
                        file).calculateMD5().build();
                try {
                    storeMap.put(digest, remoteBlob);
                } catch (Exception e) {
                    throw new IOException("Unable to store binary", e);
                }
                // validate storage
                Blob checkBlob;
                try {
                    checkBlob = storeMap.get(digest);
                } catch (Exception e) {
                    try {
                        // Remote blob can't be validated - remove it
                        storeMap.remove(digest);
                    } catch (Exception e2) {
                        log.error("Possible data corruption : binary "
                                + digest
                                + " validation failed but it could not be removed.");
                    }
                    throw new IOException("Unable to validate stored binary", e);
                }
                if (checkBlob == null
                        || remoteBlob.getMetadata().getContentMetadata().getContentLength() != checkBlob.getMetadata().getContentMetadata().getContentLength()) {
                    if (checkBlob != null) {
                        // Remote blob is incomplete - remove it
                        try {
                            storeMap.remove(digest);
                        } catch (Exception e2) {
                            log.error("Possible data corruption : binary "
                                    + digest
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
                remoteBlob = storeMap.get(digest);
            } catch (Exception e) {
                log.error("Could not cache binary from remote storage: "
                        + digest, e);
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
                    log.error("Unable to cache binary from remote storage: "
                            + digest, e);
                    return false;
                } finally {
                    IOUtils.closeQuietly(remoteStream);
                    IOUtils.closeQuietly(localStream);
                }
            }
            return true;
        }

        @Override
        public Long fetchLength(String digest) {
            Blob remoteBlob;
            try {
                remoteBlob = storeMap.get(digest);
            } catch (Exception e) {
                log.error("Unable to fetch binary information from remote storage");
                return null;
            }
            if (remoteBlob == null) {
                return null;
            } else {
                return remoteBlob.getMetadata().getContentMetadata().getContentLength();
            }
        }

    }

    /**
     * Garbage collector for the blobstore binaries that stores the marked (in
     * use)
     * binaries in memory.
     */
    public static class JCloudsBinaryGarbageCollector implements
            BinaryGarbageCollector {

        protected final JCloudsBinaryManager binaryManager;

        protected volatile long startTime;

        protected BinaryManagerStatus status;

        protected Set<String> marked;

        public JCloudsBinaryGarbageCollector(JCloudsBinaryManager binaryManager) {
            this.binaryManager = binaryManager;
        }

        @Override
        public String getId() {
            return "jclouds/" + binaryManager.storeProvider + ":"
                    + binaryManager.storeName;
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

            Set<Map.Entry<String, Blob>> blobList = binaryManager.storeMap.entrySet();
            Set<String> unmarked = new HashSet<>();
            for (Map.Entry<String, Blob> blobEntry : blobList) {
                String digest = blobEntry.getKey();
                if (!isMD5(digest)) {
                    // ignore files that cannot be MD5 digests for safety
                    continue;
                }
                Blob blob = blobEntry.getValue();
                // Too costly to do for all binaries
                // long length =
                // blob.getMetadata().getContentMetadata().getContentLength();
                if (marked.contains(digest)) {
                    status.numBinaries++;
                    // status.sizeBinaries += length;
                } else {
                    long length = blob.getMetadata().getContentMetadata().getContentLength();
                    status.numBinariesGC++;
                    status.sizeBinariesGC += length;
                    // record file to delete
                    unmarked.add(digest);
                    marked.remove(digest); // optimize memory
                }
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
    }

}
