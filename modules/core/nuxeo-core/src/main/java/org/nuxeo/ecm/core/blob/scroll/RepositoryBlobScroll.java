/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.blob.scroll;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.runtime.api.Framework;

/**
 * Meta Scroll to scroll the blobs of the blob provider(s) of a repository, the scroll query is the repository name.
 *
 * @since 2023
 */
public class RepositoryBlobScroll implements Scroll {

    private static final Logger log = LogManager.getLogger(RepositoryBlobScroll.class);

    public static final String SCROLL_NAME = "repositoryBlobScroll";

    protected int size;

    protected Stack<Scroll> scrolls;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        RepositoryService rs = Framework.getService(RepositoryService.class);
        DocumentBlobManager documentBlobManager = Framework.getService(DocumentBlobManager.class);
        if (rs.getRepositoryNames().size() > 1 && !documentBlobManager.isUseRepositoryName()) {
            // Case where we have many repos with a custom Blob Dispatcher configuration
            // We cannot ascertain that each repository has its own different binary store path
            throw new UnsupportedOperationException(
                    "Cannot scroll blobs with a multi-repositories and blob dispatcher config.");
        }
        if (!(request instanceof GenericScrollRequest scrollRequest)) {
            throw new IllegalArgumentException(
                    "Requires a GenericScrollRequest got a " + request.getClass().getCanonicalName());
        }
        BlobManager blobManager = Framework.getService(BlobManager.class);
        String repositoryName = scrollRequest.getQuery();
        scrolls = new Stack<>();
        ScrollService scrollService = Framework.getService(ScrollService.class);
        this.size = scrollRequest.getSize();
        for (String providerId : documentBlobManager.getProviderIds(repositoryName)) {
            BlobProvider provider = blobManager.getBlobProvider(providerId);
            if (!(provider instanceof BlobStoreBlobProvider bsbp)) {
                log.warn("Provider: {} must extend BlobStoreBlobProvider but was {}", () -> providerId,
                        () -> provider.getClass().getCanonicalName());
                continue;
            }
            try {
                String scrollName = bsbp.getStoreScrollName();
                ScrollRequest subRequest = GenericScrollRequest.builder(scrollName, providerId).size(size).build();
                if (scrollService.exists(request)) {
                    scrolls.push(scrollService.scroll(subRequest));
                } else {
                    log.error("Scroll: {} does not exist", scrollName);
                }
            } catch (UnsupportedOperationException e) {
                log.warn("Cannot scroll blobs of repository: {} for provider: {} because class {} is not supported",
                        () -> repositoryName, () -> providerId, () -> bsbp.getClass().getCanonicalName());
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (scrolls == null || scrolls.isEmpty()) {
            return false;
        }
        if (!scrolls.peek().hasNext()) {
            scrolls.pop().close();
            return hasNext();
        }
        return true;
    }

    @Override
    public List<String> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<String> result = scrolls.peek().next();
        if (result.size() < this.size && !scrolls.peek().hasNext() && scrolls.size() > 1) {
            log.debug("There may are more results in next scroll");
        }
        return result;
    }

    @Override
    public void close() {
        scrolls.forEach(Scroll::close);
    }

}
