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

import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;

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
public class AzureGarbageCollector implements BinaryGarbageCollector {

    private static final Log log = LogFactory.getLog(AzureGarbageCollector.class);

    private static final Pattern MD5_RE = Pattern.compile("(.*/)?[0-9a-f]{32}");

    protected final AzureBinaryManager binaryManager;

    protected BinaryManagerStatus status;

    protected volatile long startTime;

    protected Set<String> marked;

    public AzureGarbageCollector(AzureBinaryManager binaryManager) {
        this.binaryManager = binaryManager;
    }

    @Override
    public String getId() {
        return "azure:" + binaryManager.container.getName();
    }

    @Override
    public void start() {
        if (startTime != 0) {
            throw new RuntimeException("Already started");
        }
        startTime = System.currentTimeMillis();
        status = new BinaryManagerStatus();
        marked = new HashSet<>();

        // XXX : we should be able to do better
        // and only remove the cache entry that will be removed from S3
        binaryManager.fileCache.clear();
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

        try {
            // list Azure objects in the container
            // record those not marked
            Set<String> unmarked = new HashSet<>();
            ResultContinuation continuationToken = null;
            ResultSegment<ListBlobItem> lbs;
            do {
                lbs = binaryManager.container.listBlobsSegmented(null, false, EnumSet.noneOf(BlobListingDetails.class),
                        null, continuationToken, null, null);

                for (ListBlobItem item : lbs.getResults()) {

                    if (!(item instanceof CloudBlockBlob)) {
                        // ignore wrong blob type
                        continue;
                    }

                    CloudBlockBlob blob = (CloudBlockBlob) item;

                    String digest;
                    try {
                        digest = blob.getName();
                    } catch (URISyntaxException e) {
                        // Should never happends
                        // @see com.microsoft.azure.storage.blob.CloudBlob.getName()
                        continue;
                    }

                    if (!isMD5(digest)) {
                        // ignore files that cannot be MD5 digests for
                        // safety
                        continue;
                    }

                    long length = blob.getProperties().getLength();
                    if (marked.contains(digest)) {
                        status.numBinaries++;
                        status.sizeBinaries += length;
                    } else {
                        status.numBinariesGC++;
                        status.sizeBinariesGC += length;
                        // record file to delete
                        unmarked.add(digest);
                        marked.remove(digest); // optimize memory
                    }

                }

                continuationToken = lbs.getContinuationToken();
            } while (lbs.getHasMoreResults());
            marked = null; // help GC

            if (delete) {
                unmarked.forEach(binaryManager::removeBinary);
            }

        } catch (StorageException e) {
            throw new RuntimeException(e);
        } finally {
            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
        }

    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
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
}
