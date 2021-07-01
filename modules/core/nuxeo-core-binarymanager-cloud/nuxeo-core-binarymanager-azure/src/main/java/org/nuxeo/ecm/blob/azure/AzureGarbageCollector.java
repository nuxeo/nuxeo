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

import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.nuxeo.ecm.blob.AbstractBinaryGarbageCollector;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.microsoft.azure.storage.ResultContinuation;
import com.microsoft.azure.storage.ResultSegment;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobListingDetails;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureGarbageCollector extends AbstractBinaryGarbageCollector<AzureBinaryManager> {

    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    public AzureGarbageCollector(AzureBinaryManager binaryManager) {
        super(binaryManager);
    }

    @Override
    public String getId() {
        return "azure:" + binaryManager.container.getName();
    }

    /**
     * @since 11.5
     */
    @Override
    public void computeToDelete() {
        toDelete = new HashSet<>();
        ResultContinuation continuationToken = null;
        ResultSegment<ListBlobItem> lbs;
        do {
            try {
                lbs = binaryManager.container.listBlobsSegmented(binaryManager.prefix, false,
                        EnumSet.noneOf(BlobListingDetails.class), null, continuationToken, null, null);
            } catch (StorageException e) {
                throw new NuxeoException(e);
            }

            // ignore subdirectories by considering only instances of CloudBlockBlob
            lbs.getResults().stream().filter(CloudBlockBlob.class::isInstance).forEach(item -> {
                CloudBlockBlob blob = (CloudBlockBlob) item;

                String name = blob.getName();
                String digest = name.substring(binaryManager.prefix.length());

                if (!isMD5(digest)) {
                    // ignore files that cannot be MD5 digests for
                    // safety
                    return;
                }

                long length = blob.getProperties().getLength();
                status.sizeBinaries += length;
                status.numBinaries++;
                toDelete.add(digest);
            });

            continuationToken = lbs.getContinuationToken();
        } while (lbs.getHasMoreResults());
    }

    /**
     * @since 11.5
     */
    @Override
    protected void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
        for (String digest : toDelete) {
            try {
                long length = binaryManager.lengthOfBlob(digest);
                if (length < 0) {
                    // shouldn't happen except if blob concurrently removed
                    continue;
                }
                status.sizeBinariesGC += length;
                status.numBinariesGC++;
                status.sizeBinaries -= length;
                status.numBinaries--;
                if (delete) {
                    binaryManager.removeBinary(digest);
                }
            } catch (URISyntaxException | StorageException e) {
                throw new NuxeoException(e);
            }
        }
    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }
}
