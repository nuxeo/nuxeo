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

import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.blob.AbstractBinaryGarbageCollector;

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

    private static final Log log = LogFactory.getLog(AzureGarbageCollector.class);

    private static final Pattern MD5_RE = Pattern.compile("(.*/)?[0-9a-f]{32}");

    public AzureGarbageCollector(AzureBinaryManager binaryManager) {
        super(binaryManager);
    }

    @Override
    public String getId() {
        return "azure:" + binaryManager.container.getName();
    }

    @Override
    public Set<String> getUnmarkedBlobs() {
        Set<String> unmarked = new HashSet<>();
        ResultContinuation continuationToken = null;
        ResultSegment<ListBlobItem> lbs;
        do {
            try {
                lbs = binaryManager.container.listBlobsSegmented(null, false, EnumSet.noneOf(BlobListingDetails.class),
                        null, continuationToken, null, null);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }

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
                    marked.remove(digest); // optimize memory
                } else {
                    status.numBinariesGC++;
                    status.sizeBinariesGC += length;
                    // record file to delete
                    unmarked.add(digest);
                }
            }

            continuationToken = lbs.getContinuationToken();
        } while (lbs.getHasMoreResults());
        marked = null; // help GC

        return unmarked;
    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }
}
