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