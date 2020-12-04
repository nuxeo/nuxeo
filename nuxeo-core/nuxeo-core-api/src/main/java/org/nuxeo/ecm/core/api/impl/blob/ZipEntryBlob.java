/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl.blob;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.input.ProxyInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * A {@link Blob} backed by an entry in a ZIP file.
 *
 * @since 7.2
 */
public class ZipEntryBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final ZipFile zipFile;

    protected final ZipEntry zipEntry;

    protected final Blob zipBlob;

    protected final String entryName;

    /**
     * Creates a {@link Blob} from an entry in a zip file. The {@link ZipFile} must not be closed until the stream has
     * been read.
     *
     * @param zipFile the zip file
     * @param zipEntry the zip entry
     */
    public ZipEntryBlob(ZipFile zipFile, ZipEntry zipEntry) {
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
        zipBlob = null;
        entryName = null;
    }

    /**
     * Creates a {@link Blob} from an entry in a ZIP file blob.
     *
     * @param zipBlob the ZIP file blob
     * @param entryName the ZIP entry name
     * @since 11.5
     */
    public ZipEntryBlob(Blob zipBlob, String entryName) {
        zipFile = null;
        zipEntry = null;
        this.zipBlob = zipBlob;
        this.entryName = entryName;
    }

    @Override
    public InputStream getStream() throws IOException {
        if (zipBlob == null) {
            return zipFile.getInputStream(zipEntry);
        } else {
            return ZipEntryInputStream.of(zipBlob, entryName);
        }
    }

    @Override
    public long getLength() {
        if (zipBlob == null) {
            return zipEntry.getSize();
        } else {
            File file = zipBlob.getFile();
            if (file != null) {
                // if there's a file then we can be fast
                try (ZipFile zf = new ZipFile(file)) {
                    ZipEntry entry = zf.getEntry(entryName);
                    return entry == null ? 0 : entry.getSize();
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            } else {
                // else use the stream
                try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(zipBlob.getStream()))) {
                    ZipEntry entry;
                    while ((entry = zin.getNextEntry()) != null) {
                        if (entry.getName().equals(entryName)) {
                            return entry.getSize();
                        }
                    }
                    return 0;
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
        }
    }

    // @since 11.5
    @Override
    public String getFilename() {
        return entryName != null ? entryName : super.getFilename();
    }

    /**
     * {@link InputStream} for a ZIP entry, that closes all necessary resources when {@linkplain #close closed}.
     *
     * @since 11.5
     */
    public static class ZipEntryInputStream extends ProxyInputStream {

        // what we'll need to close in addition to the InputStream itself
        protected final List<Closeable> closeables;

        /**
         * Factory for a {@link ZipEntryInputStream}.
         */
        public static ZipEntryInputStream of(Blob zipBlob, String entryName) throws IOException {
            List<Closeable> closeables = new ArrayList<>(2);
            InputStream in;
            try {
                CloseableFile closeableFile = zipBlob.getCloseableFile();
                closeables.add(closeableFile);
                ZipFile zipFile = new ZipFile(closeableFile.getFile());
                closeables.add(zipFile);
                in = zipFile.getInputStream(zipFile.getEntry(entryName));
            } catch (IOException ioe) {
                try {
                    close(closeables);
                } catch (IOException e) {
                    ioe.addSuppressed(e);
                }
                throw ioe;
            }
            return new ZipEntryInputStream(in, closeables);
        }

        protected ZipEntryInputStream(InputStream in, List<Closeable> closeables) {
            super(in);
            this.closeables = closeables;
        }

        @Override
        public void close() throws IOException {
            List<Closeable> closing = new ArrayList<>(3);
            closing.add(super::close);
            closing.addAll(closeables);
            closeables.clear();
            close(closing);
        }

        protected static void close(List<Closeable> closeables) throws IOException {
            IOException ioe = null;
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    if (ioe == null) {
                        ioe = e;
                    } else {
                        ioe.addSuppressed(e);
                    }
                }
            }
            if (ioe != null) {
                throw ioe;
            }
        }
    }

}
