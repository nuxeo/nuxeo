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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.LocalBlobProvider;
import org.nuxeo.ecm.core.blob.LocalBlobStore;
import org.nuxeo.ecm.core.blob.PathStrategy;

/**
 * Scroll blobs of the Local blob store of a #{@link LocalBlobProvider}, the scroll query is the provider id.
 *
 * @since 2023
 */
public class LocalBlobScroll extends AbstractBlobScroll<LocalBlobProvider> {

    private static final Logger log = LogManager.getLogger(LocalBlobScroll.class);

    protected Iterator<Path> it;

    protected Stream<Path> stream;

    protected PathStrategy pathStrategy;

    @Override
    public void init(LocalBlobProvider localBlobProvider) {
        LocalBlobStore store = (LocalBlobStore) localBlobProvider.store.unwrap();
        try {
            this.pathStrategy = store.getPathStrategy();
            this.stream = Files.walk(store.getDirectory()).filter(path -> !Files.isDirectory(path));
            this.it = this.stream.iterator();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void close() {
        stream.close();
        super.close();
    }

    protected long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            log.error("Cannot get file size for path: {}", path, e);
            return 0;
        }
    }

    @Override
    public boolean hasNext() {
        return this.it.hasNext();
    }

    @Override
    public List<String> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<String> result = new ArrayList<>();
        for (int i = size; i > 0 && hasNext();) {
            Path next = this.it.next();
            addTo(result, pathStrategy.getKeyForPath(next.toString()), () -> getSize(next));
            i--;
        }
        return result;
    }

}
