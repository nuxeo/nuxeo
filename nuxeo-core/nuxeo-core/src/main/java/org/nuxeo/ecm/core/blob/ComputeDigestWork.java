/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * Work to compute a blob digest asynchronously.
 *
 * @since 11.5
 */
public class ComputeDigestWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public final String blobProviderId;

    public final String key;

    public ComputeDigestWork(String blobProviderId, String key) {
        this.blobProviderId = blobProviderId;
        this.key = key;
    }

    @Override
    public String getTitle() {
        return "Compute digest work";
    }

    @Override
    public String getCategory() {
        return "computeDigest";
    }

    @Override
    public void work() {
        try {
            new ComputeDigestHelper(blobProviderId, key).computeAndReplaceDigest();
        } catch (NuxeoException e) {
            e.addInfo("For blob provider: " + blobProviderId + " and key: " + key);
            throw e;
        }
    }

}
