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
package org.nuxeo.ecm.core.storage.mongodb.blob;

import static com.mongodb.client.model.Projections.include;
import static org.nuxeo.ecm.core.storage.mongodb.blob.GridFSBlobStore.METADATA_PROPERTY_FILENAME;
import static org.nuxeo.ecm.core.storage.mongodb.blob.GridFSBlobStore.METADATA_PROPERTY_LENGTH;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.bson.Document;
import org.nuxeo.ecm.core.blob.scroll.AbstractBlobScroll;

import com.mongodb.client.MongoCursor;

/**
 * Scroll files of the GridFS blob store of a {@link GridFSBlobProvider}, the scroll query is the provider id.
 *
 * @since 2023.5
 */
public class GridFSBlobScroll extends AbstractBlobScroll<GridFSBlobProvider> {

    protected MongoCursor<Document> it;

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public List<String> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size && hasNext(); i++) {
            Document next = this.it.next();
            addTo(result, next.getString(METADATA_PROPERTY_FILENAME), () -> next.getLong(METADATA_PROPERTY_LENGTH));
        }
        return result;
    }

    @Override
    protected void init(GridFSBlobProvider provider) {
        GridFSBlobStore store = (GridFSBlobStore) provider.store.unwrap();
        it = store.getFilesColl()
                  .find()
                  .batchSize(size)
                  .projection(include(METADATA_PROPERTY_FILENAME, METADATA_PROPERTY_LENGTH))
                  .cursor();
    }

    @Override
    public void close() {
        it.close();
        super.close();
    }
}
