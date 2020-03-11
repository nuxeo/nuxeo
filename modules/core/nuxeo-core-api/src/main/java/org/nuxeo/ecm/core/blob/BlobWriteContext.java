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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.nuxeo.ecm.core.blob.KeyStrategy.WriteObserver;

/**
 * Context available when a blob is written.
 *
 * @since 11.1
 */
public class BlobWriteContext {

    public final BlobContext blobContext;

    public final WriteObserver writeObserver;

    public final Supplier<String> keyComputer;

    public final KeyStrategy keyStrategy;

    protected Path file;

    public BlobWriteContext(BlobContext blobContext, WriteObserver writeObserver, Supplier<String> keyComputer, KeyStrategy keyStrategy) {
        this.blobContext = blobContext;
        this.writeObserver = writeObserver;
        this.keyComputer = keyComputer;
        this.keyStrategy = keyStrategy;
    }

    public BlobWriteContext copyWithKey(String key) {
        BlobWriteContext context = new BlobWriteContext(blobContext, writeObserver, () -> key,
                KeyStrategyDocId.instance()); // KeyStrategyDocId is used to mean "no deduplication"
        context.setFile(file);
        return context;
    }

    public BlobWriteContext copyWithNoWriteObserverAndKey(String key) {
        BlobWriteContext context = new BlobWriteContext(blobContext, null, () -> key, null);
        context.setFile(file);
        return context;
    }

    public boolean useDeDuplication() {
        return keyStrategy.useDeDuplication();
    }

    public String getKey() {
        return keyComputer.get();
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    public InputStream getStream() throws IOException {
        return file == null ? blobContext.blob.getStream() : Files.newInputStream(file);
    }

}
