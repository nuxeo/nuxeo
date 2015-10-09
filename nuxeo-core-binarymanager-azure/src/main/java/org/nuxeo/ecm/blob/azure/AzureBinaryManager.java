/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.blob.azure;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureBinaryManager extends AbstractCloudBinaryManager {

    private static final Log log = LogFactory.getLog(AzureBinaryManager.class);

    private final static String STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=%s;" + "AccountName=%s;"
            + "AccountKey=%s";

    public static final String ENDPOINT_PROTOCOL_PROPERTY = "endpointProtocol";

    public final static String PROPERTIES_PREFIX = "nuxeo.storage.azure";

    public static final String ACCOUNT_NAME_PROPERTY = "account.name";

    public static final String ACCOUNT_KEY_PROPERTY = "account.key";

    public static final String CONTAINER_PROPERTY = "container";

    protected CloudStorageAccount storageAccount;

    protected CloudBlobClient blobClient;

    protected CloudBlobContainer container;

    @Override
    protected String getPropertyPrefix() {
        return PROPERTIES_PREFIX;
    }

    @Override
    protected void setupCloudClient() throws IOException {
        String connectionString = String.format(STORAGE_CONNECTION_STRING,
                getProperty(ENDPOINT_PROTOCOL_PROPERTY, "https"), getProperty(ACCOUNT_NAME_PROPERTY),
                getProperty(ACCOUNT_KEY_PROPERTY));
        try {
            storageAccount = CloudStorageAccount.parse(connectionString);

            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(getProperty(CONTAINER_PROPERTY));
            container.createIfNotExists();
        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            throw new IOException("Unable to initialize Azure binary manager", e);
        }
    }

    protected BinaryGarbageCollector instantiateGarbageCollector() {
        return new AzureGarbageCollector(this);
    }

    protected FileStorage getFileStorage() {
        return new AzureFileStorage(container);
    }

    protected void removeBinary(String digest) {
        try {
            container.getBlockBlobReference(digest).delete();
        } catch (StorageException | URISyntaxException e) {
            log.error("Unable to remove binary " + digest, e);
        }
    }

    @Override
    public void removeBinaries(Collection<String> digests) {
        digests.forEach(this::removeBinary);
    }
}
