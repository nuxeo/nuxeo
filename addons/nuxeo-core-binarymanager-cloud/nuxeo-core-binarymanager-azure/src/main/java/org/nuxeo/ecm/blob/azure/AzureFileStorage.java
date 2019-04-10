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

package org.nuxeo.ecm.blob.azure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.binary.FileStorage;

import com.microsoft.azure.storage.StorageErrorCode;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.Base64;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureFileStorage implements FileStorage {

    private static final Log log = LogFactory.getLog(AzureFileStorage.class);

    protected CloudBlobContainer container;

    protected String prefix;

    public AzureFileStorage(CloudBlobContainer container, String prefix) {
        this.container = container;
        this.prefix = prefix;
    }

    @Override
    public void storeFile(String digest, File file) throws IOException {
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("storing blob " + digest + " to Azure");
        }
        CloudBlockBlob blob;
        try {
            blob = container.getBlockBlobReference(prefix + digest);
            if (blob.exists()) {
                if (isBlobDigestCorrect(digest, blob)) {
                    if (log.isDebugEnabled()) {
                        log.debug("blob " + digest + " is already in Azure");
                    }
                    return;
                }
            }

            try (InputStream is = new FileInputStream(file)) {
                blob.upload(is, file.length());
            }
        } catch (StorageException | URISyntaxException e) {
            throw new IOException(e);
        } finally {
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("stored blob " + digest + " to Azure in " + dtms + "ms");
            }
        }
    }

    @Override
    public boolean fetchFile(String digest, File file) throws IOException {
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("fetching blob " + digest + " from Azure");
        }
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(prefix + digest);
            if (!(blob.exists() && isBlobDigestCorrect(digest, blob))) {
                log.error("Invalid ETag in Azure, AzDigest=" + blob.getProperties().getContentMD5() + " digest="
                        + digest);
                return false;
            }
            try (OutputStream os = new FileOutputStream(file)) {
                blob.download(os);
            }
            return true;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } catch (StorageException e) {
            return false;
        } finally {
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("fetched blob " + digest + " from Azure in " + dtms + "ms");
            }
        }
    }

    protected static boolean isMissingKey(StorageException e) {
        return e.getErrorCode().equals(StorageErrorCode.RESOURCE_NOT_FOUND.toString());
    }

    protected static boolean isBlobDigestCorrect(String digest, CloudBlockBlob blob) {
        return isBlobDigestCorrect(digest, blob.getProperties().getContentMD5());
    }

    protected static boolean isBlobDigestCorrect(String digest, String contentMD5) {
        return digest.equals(decodeContentMD5(contentMD5));
    }

    protected static String decodeContentMD5(String contentMD5) {
        try {
            byte[] bytes = Base64.decode(contentMD5);
            return Hex.encodeHexString(bytes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
