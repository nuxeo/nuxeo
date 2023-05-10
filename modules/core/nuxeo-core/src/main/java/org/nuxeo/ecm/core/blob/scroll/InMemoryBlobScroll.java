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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.blob.InMemoryBlobProvider;
import org.nuxeo.ecm.core.blob.InMemoryBlobStore;

/**
 * Scroll blobs of the In-Memory blob store of a #{@link InMemoryBlobProvider}, the scroll query is the provider id.
 *
 * @since 2023
 */
public class InMemoryBlobScroll extends AbstractBlobScroll<InMemoryBlobProvider> {

    protected Iterator<String> it;

    protected InMemoryBlobStore store;

    @Override
    public void init(InMemoryBlobProvider inMemoryBlobProvider) {
        this.store = (InMemoryBlobStore) inMemoryBlobProvider.store.unwrap();
        this.it = store.getKeyIterator();
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public List<String> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<String> result = new ArrayList<>();
        for (int i = size; i > 0 && it.hasNext(); i--) {
            var next = it.next();
            addTo(result, next, () -> store.getLength(next));
        }
        return result;
    }

}
