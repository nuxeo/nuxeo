/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.CachingBinaryManager;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureBinaryManager extends CachingBinaryManager implements BlobProvider {

    private static final String DEFAULT_CACHE_SIZE = "100 mb";

    private final static String STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=%s;" + "AccountName=%s;"
            + "AccountKey=%s";

    public final static String PROPERTIES_PREFIX = "nuxeo.storage.azure";

    public static final String ENDPOINT_PROTOCOL_PROPERTY = "endpointProtocol";

    public static final String ACCOUNT_NAME_PROPERTY = "account.name";

    public static final String ACCOUNT_KEY_PROPERTY = "account.key";

    public static final String CONTAINER_PROPERTY = "container";

    public static final String CACHE_PROPERTY = "cache";

    protected Map<String, String> properties;

    protected CloudStorageAccount storageAccount;

    protected CloudBlobClient blobClient;

    protected CloudBlobContainer container;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        this.properties = properties;

        String connectionString = String.format(STORAGE_CONNECTION_STRING,
                getProperty(ENDPOINT_PROTOCOL_PROPERTY, "https"), getProperty(ACCOUNT_NAME_PROPERTY),
                getProperty(ACCOUNT_KEY_PROPERTY));
        try {
            storageAccount = CloudStorageAccount.parse(connectionString);

            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(getProperty(CONTAINER_PROPERTY));
            container.createIfNotExists();

            String cacheSizeStr = getProperty(CACHE_PROPERTY, DEFAULT_CACHE_SIZE);
            initializeCache(cacheSizeStr, getFileStorage());

            // TODO
            // Create dedicated GarbageCollector
        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            throw new IOException("Unable to initialize Azure binary manager", e);
        }
    }

    protected FileStorage getFileStorage() {
        return new AzureFileStorage(container);
    }

    protected void removeBinary(String digest) throws URISyntaxException, StorageException {
        container.getBlockBlobReference(digest).delete();
    }

    @Override
    public Blob readBlob(BlobManager.BlobInfo blobInfo) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).readBlob(blobInfo);
    }

    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).writeBlob(blob, doc);
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    protected String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    protected String getProperty(String propertyName, String defaultValue) {
        if (properties.containsKey(propertyName)) {
            return properties.get(propertyName);
        }

        return Framework.getProperty(getPropertyKey(propertyName), defaultValue);
    }

    public static String getPropertyKey(String propertyName) {
        return String.format("%s.%s", PROPERTIES_PREFIX, propertyName);
    }
}
