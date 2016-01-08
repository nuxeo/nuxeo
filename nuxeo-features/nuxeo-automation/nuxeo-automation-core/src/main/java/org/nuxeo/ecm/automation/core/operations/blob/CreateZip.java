/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.ZipOutputStream;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

/**
 * TODO: detect mine?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateZip.ID, category = Constants.CAT_BLOB, label = "Zip", description = "Creates a zip file from the input file(s). If no file name is given, the first file name in the input will be used. Returns the zip file.")
public class CreateZip {

    public static final String ID = "Blob.CreateZip";

    public static final String ZIP_ENTRY_ENCODING_PROPERTY = "zip.entry.encoding";

    public static enum ZIP_ENTRY_ENCODING_OPTIONS {
        ascii
    }

    @Context
    protected OperationContext ctx;

    @Param(name = "filename", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(Blob blob) throws IOException {
        if (fileName == null || (fileName = fileName.trim()).length() == 0) {
            fileName = blob.getFilename();
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
        return Blobs.createBlob(file, "application/zip", null, fileName);
    }

    @OperationMethod
    public Blob run(BlobList blobs) throws IOException {
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

    protected String getFileName(Blob blob) {
        String entry = blob.getFilename();
        if (entry == null) {
            entry = "Unknown_" + System.identityHashCode(blob);
        }
        return escapeEntryPath(entry);
    }

    protected void zip(Blob blob, ZipOutputStream out) throws IOException {
        String entry = getFileName(blob);
        InputStream in = blob.getStream();
        try {
            ZipUtils._zip(entry, in, out);
        } finally {
            in.close();
        }
    }

    protected void zip(BlobList blobs, ZipOutputStream out) throws IOException {
        // use a set to avoid zipping entries with same names
        Collection<String> names = new HashSet<String>();
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

    protected String escapeEntryPath(String path) {
        String zipEntryEncoding = Framework.getProperty(ZIP_ENTRY_ENCODING_PROPERTY);
        if (zipEntryEncoding != null && zipEntryEncoding.equals(ZIP_ENTRY_ENCODING_OPTIONS.ascii.toString())) {
            return StringUtils.toAscii(path, true);
        }
        return path;
    }

}
