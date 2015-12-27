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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * A wrapper for a {@link File}, which must be closed when done with it in order to release resources.
 *
 * @since 7.2
 */
public class CloseableFile implements Closeable {

    public final File file;

    public final boolean deleteOnClose;

    public CloseableFile(File file, boolean deleteOnClose) {
        this.file = file;
        this.deleteOnClose = deleteOnClose;
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        if (deleteOnClose) {
            file.delete();
        }
    }
}
