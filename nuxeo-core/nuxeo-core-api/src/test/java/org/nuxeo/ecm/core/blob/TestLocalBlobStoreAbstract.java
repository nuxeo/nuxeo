/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.blob.LocalBlobStore.LocalBlobGarbageCollector;

public abstract class TestLocalBlobStoreAbstract extends TestAbstractBlobStore {

    @Override
    public boolean hasGCTimeThreshold() {
        return true;
    }

    @Override
    public void waitForGCTimeThreshold() {
        try {
            Thread.sleep(LocalBlobGarbageCollector.TIME_RESOLUTION + 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException();
        }
    }

}
