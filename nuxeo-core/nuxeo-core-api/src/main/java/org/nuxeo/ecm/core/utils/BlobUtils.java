/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.core.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

/**
 * Blob utility methods.
 *
 * @since 9.3
 */
public class BlobUtils {

    public enum ZIP_ENTRY_ENCODING_OPTIONS {
        ascii
    }

    public static final String ZIP_ENTRY_ENCODING_PROPERTY = "zip.entry.encoding";

    protected static String escapeEntryPath(String path) {
        String zipEntryEncoding = Framework.getProperty(ZIP_ENTRY_ENCODING_PROPERTY);
        if (zipEntryEncoding != null && zipEntryEncoding.equals(ZIP_ENTRY_ENCODING_OPTIONS.ascii.toString())) {
            return StringUtils.toAscii(path, true);
        }
        return path;
    }

    protected static String getFileName(Blob blob) {
        String entry = blob.getFilename();
        if (entry == null) {
            entry = "Unknown_" + System.identityHashCode(blob);
        }
        return escapeEntryPath(entry);
    }

    /**
     * Zip the given blob.
     *
     * @param blob the blob
     * @param filename if no filename is given, the blob's filename will be used
     * @return a zip containing the blob
     * @throws IOException
     */
    public static Blob zip(Blob blob, String filename) throws IOException {
        if (filename == null || (filename = filename.trim()).length() == 0) {
            filename = blob.getFilename();
        }
        File file = Framework.createTempFile("nxops-createzip-", ".tmp");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        Framework.trackFile(file, file);
        try {
            zip(blob, out);
        } finally {
            out.finish();
            out.close();
        }
        return Blobs.createBlob(file, "application/zip", null, filename);
    }

    protected static void zip(Blob blob, ZipOutputStream out) throws IOException {
        String entry = getFileName(blob);
        InputStream in = blob.getStream();
        try {
            ZipUtils._zip(entry, in, out);
        } finally {
            in.close();
        }
    }

    /**
     * Zip a list of blob.
     *
     * @param blobs the blob list
     * @param fileName if no filename is given, the first blob's filename will be used
     * @return a zip containing the list of blob
     * @throws IOException
     */
    public static Blob zip(List<Blob> blobs, String fileName) throws IOException {
        if (fileName == null || (fileName = fileName.trim()).length() == 0) {
            fileName = blobs.isEmpty() ? null : blobs.get(0).getFilename();
        }
        File file = Framework.createTempFile("nxops-createzip-", ".tmp");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        Framework.trackFile(file, file);
        try {
            zip(blobs, out);
        } finally {
            out.finish();
            out.close();
        }
        return Blobs.createBlob(file, "application/zip", null, fileName);
    }

    protected static void zip(List<Blob> blobs, ZipOutputStream out) throws IOException {
        // use a set to avoid zipping entries with same names
        Collection<String> names = new HashSet<>();
        int cnt = 1;
        for (Blob blob : blobs) {
            String entry = getFileName(blob);
            if (!names.add(entry)) {
                entry = "renamed_" + (cnt++) + "_" + entry;
            }
            InputStream in = blob.getStream();
            try {
                ZipUtils._zip(entry, in, out);
            } finally {
                in.close();
            }
        }
    }

}
